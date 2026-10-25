package com.drivehub.pagos.features.comprobantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.AlmacenProperties;
import com.drivehub.pagos.features.comprobantes.almacen.MinioAlmacenComprobantes;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * El almacén de comprobantes (CU30) contra un servidor del JDK que habla lo mínimo de S3 (PUT y GET de un objeto,
 * en el estilo de ruta `/bucket/clave` que usa MinIO). La imagen oficial de MinIO ya no está en Docker Hub, así
 * que no se usa Testcontainers: lo que se prueba es lo nuestro (claves, errores, privacidad), no MinIO.
 */
class MinioAlmacenComprobantesTest {

    private HttpServer servidor;
    private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();
    private final List<String> peticiones = new CopyOnWriteArrayList<>();
    private final List<String> tiposDeContenido = new CopyOnWriteArrayList<>();
    private volatile int estadoFijo = 0;
    private volatile String bucketExistente = "drivehub-pagos";
    private volatile long demoraMs = 0;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    private void atender(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().getPath();
        peticiones.add(intercambio.getRequestMethod() + " " + ruta);
        byte[] cuerpo = intercambio.getRequestBody().readAllBytes();
        try {
            Thread.sleep(demoraMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int estado = 200;
        byte[] respuesta = new byte[0];
        String[] partes = ruta.substring(1).split("/", 2);
        if (estadoFijo != 0) {
            estado = estadoFijo;
            respuesta = error("InternalError");
        } else if (!partes[0].equals(bucketExistente)) {
            estado = 404;
            respuesta = error("NoSuchBucket");
        } else if ("PUT".equals(intercambio.getRequestMethod())) {
            tiposDeContenido.add(String.valueOf(intercambio.getRequestHeaders().getFirst("Content-Type")));
            objetos.put(partes[1], desempaquetar(intercambio, cuerpo));
            intercambio.getResponseHeaders().add("ETag", "\"abc\"");
        } else if (objetos.containsKey(partes[1])) {
            respuesta = objetos.get(partes[1]);
            intercambio.getResponseHeaders().add("Content-Type", "application/pdf");
        } else {
            estado = 404;
            respuesta = error("NoSuchKey");
        }
        if (estado >= 400) {
            intercambio.getResponseHeaders().add("Content-Type", "application/xml");
        }
        boolean sinCuerpo = respuesta.length == 0;
        intercambio.sendResponseHeaders(estado, sinCuerpo ? -1 : respuesta.length);
        if (!sinCuerpo) {
            intercambio.getResponseBody().write(respuesta);
        }
        intercambio.close();
    }

    // * Por HTTP (no HTTPS) el SDK firma el cuerpo por trozos (`aws-chunked`): `tamaño-hex;chunk-signature=...`,
    // * un salto de línea, los datos y otro salto de línea; el último trozo mide cero.
    private static byte[] desempaquetar(HttpExchange intercambio, byte[] cuerpo) {
        String contenido = intercambio.getRequestHeaders().getFirst("x-amz-content-sha256");
        if (contenido == null || !contenido.startsWith("STREAMING")) {
            return cuerpo;
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int i = 0;
        while (i < cuerpo.length) {
            int fin = i;
            while (cuerpo[fin] != (byte) 0x0D) {
                fin++;
            }
            String cabecera = new String(cuerpo, i, fin - i, StandardCharsets.US_ASCII);
            int tamano = Integer.parseInt(cabecera.substring(0, cabecera.indexOf(';')), 16);
            i = fin + 2;
            if (tamano == 0) {
                break;
            }
            salida.write(cuerpo, i, tamano);
            i += tamano + 2;
        }
        return salida.toByteArray();
    }

    private static byte[] error(String codigo) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><Code>" + codigo + "</Code><Message>" + codigo
            + "</Message></Error>").getBytes(StandardCharsets.UTF_8);
    }

    private MinioAlmacenComprobantes almacen(String bucket, int timeoutSegundos) {
        return new MinioAlmacenComprobantes(new AlmacenProperties("http://127.0.0.1:" + servidor.getAddress().getPort(),
            "acceso", "secreto", bucket, timeoutSegundos));
    }

    @Test
    void guardaYLeeElMismoContenido_byteAByte_enLaRutaDelBucket() {
        byte[] pdf = new byte[] {'%', 'P', 'D', 'F', 0, 1, 2, (byte) 255, (byte) 128, 10, 13, 0};
        MinioAlmacenComprobantes almacen = almacen("drivehub-pagos", 5);

        almacen.guardar("comprobantes/escuela/2026/CMP-2026-000184.pdf", pdf);

        assertThat(almacen.leer("comprobantes/escuela/2026/CMP-2026-000184.pdf")).isEqualTo(pdf);
        assertThat(peticiones).containsExactly("PUT /drivehub-pagos/comprobantes/escuela/2026/CMP-2026-000184.pdf",
            "GET /drivehub-pagos/comprobantes/escuela/2026/CMP-2026-000184.pdf");
        assertThat(tiposDeContenido).containsExactly("application/pdf");
    }

    @Test
    void guardarOtraVezLaMismaClave_losReemplaza() {
        MinioAlmacenComprobantes almacen = almacen("drivehub-pagos", 5);

        almacen.guardar("k.pdf", new byte[] {1});
        almacen.guardar("k.pdf", new byte[] {2, 3});

        assertThat(almacen.leer("k.pdf")).containsExactly(2, 3);
    }

    @Test
    void unaClaveInexistente_fallaConNoSuchElement() {
        assertThatThrownBy(() -> almacen("drivehub-pagos", 5).leer("no-existe.pdf"))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void unBucketInexistente_seReportaComoServicioNoDisponible_yNoSeCrea() {
        MinioAlmacenComprobantes almacen = almacen("otro-bucket", 5);

        assertThatThrownBy(() -> almacen.guardar("k.pdf", new byte[] {1}))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE));
        assertThatThrownBy(() -> almacen.leer("k.pdf")).isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(peticiones).noneMatch(p -> p.startsWith("PUT /otro-bucket") && p.equals("PUT /otro-bucket"));
    }

    @Test
    void unErrorDelAlmacen_esServicioNoDisponible() {
        estadoFijo = 500;

        assertThatThrownBy(() -> almacen("drivehub-pagos", 5).guardar("k.pdf", new byte[] {1}))
            .isInstanceOf(ServicioNoDisponibleException.class);
        assertThatThrownBy(() -> almacen("drivehub-pagos", 5).leer("k.pdf"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void sinAlmacen_esServicioNoDisponible() {
        MinioAlmacenComprobantes almacen = almacen("drivehub-pagos", 1);
        servidor.stop(0);

        assertThatThrownBy(() -> almacen.guardar("k.pdf", new byte[] {1}))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void sinCredencialesConfiguradas_crearElAlmacenNoFalla_yUsarloEsServicioNoDisponible() {
        MinioAlmacenComprobantes almacen = new MinioAlmacenComprobantes(
            new AlmacenProperties("http://127.0.0.1:9", "", "", "drivehub-pagos", 1));

        assertThatThrownBy(() -> almacen.guardar("k.pdf", new byte[] {1}))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_15"));
        assertThatThrownBy(() -> almacen.leer("k.pdf")).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void unaRespuestaLenta_vencePorTimeout_yEsServicioNoDisponible() {
        demoraMs = 3000;

        assertThatThrownBy(() -> almacen("drivehub-pagos", 1).leer("k.pdf"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
