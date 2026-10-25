package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.features.pagos.client.EscuelaClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * El cliente de Escuela (CU29) contra un servidor del JDK que hace de Escuela, con `total` alumnos asignados.
 */
class EscuelaClientAlumnosTest {

    private HttpServer servidor;
    private final List<String> peticiones = new CopyOnWriteArrayList<>();
    private final List<String> autorizaciones = new CopyOnWriteArrayList<>();
    private volatile int total = 0;
    private volatile int estadoFijo = 0;
    private volatile long demoraMs = 0;
    private volatile List<UUID> alumnos = List.of();

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
        peticiones.add(intercambio.getRequestURI().toString());
        autorizaciones.add(intercambio.getRequestHeaders().getFirst("Authorization"));
        try {
            Thread.sleep(demoraMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String ruta = intercambio.getRequestURI().getPath();
        int estado = estadoFijo != 0 ? estadoFijo : 200;
        String cuerpo;
        if (estadoFijo != 0) {
            cuerpo = "{\"status\":\"failed\",\"error\":\"ERR_X\",\"message\":\"x\"}";
        } else if (ruta.endsWith("/mis-alumnos")) {
            cuerpo = pagina(intercambio.getRequestURI().getQuery());
        } else {
            String codigo = ruta.substring(ruta.lastIndexOf('/') + 1);
            boolean esMio = alumnos.stream().anyMatch(a -> a.toString().equals(codigo));
            estado = esMio ? 200 : 404;
            cuerpo = esMio ? "{\"status\":\"success\",\"data\":{\"estudianteCodigo\":\"" + codigo + "\"}}"
                : "{\"status\":\"failed\",\"error\":\"ERR_ESC_15\",\"message\":\"x\"}";
        }
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(estado, bytes.length);
        intercambio.getResponseBody().write(bytes);
        intercambio.close();
    }

    private String pagina(String query) {
        int page = Integer.parseInt(query.replaceAll(".*page=(\\d+).*", "$1"));
        int tamano = Integer.parseInt(query.replaceAll(".*pageSize=(\\d+).*", "$1"));
        int desde = (page - 1) * tamano;
        List<String> codigos = new ArrayList<>();
        for (int i = desde; i < Math.min(desde + tamano, total); i++) {
            codigos.add("{\"estudianteCodigo\":\"" + new UUID(0, i + 1) + "\"}");
        }
        int paginas = (int) Math.ceil(total / (double) tamano);
        return "{\"status\":\"success\",\"data\":[" + String.join(",", codigos) + "],\"meta\":{\"page\":" + page
            + ",\"pageSize\":" + tamano + ",\"totalItems\":" + total + ",\"totalPages\":" + paginas + "}}";
    }

    private EscuelaClient cliente(long timeoutSegundos) {
        return new EscuelaClient("http://127.0.0.1:" + servidor.getAddress().getPort(), timeoutSegundos);
    }

    @Test
    void alumnosDelInstructor_recorreTodasLasPaginasDeCien_yReenviaElBearer() {
        total = 250;

        List<UUID> resultado = cliente(5).alumnosDelInstructor("Bearer token-del-instructor");

        assertThat(resultado).hasSize(250).doesNotHaveDuplicates();
        assertThat(peticiones).hasSize(3);
        assertThat(peticiones.get(0)).startsWith("/api/v1/asignaciones-alumno/mis-alumnos")
            .contains("page=1").contains("pageSize=100");
        assertThat(autorizaciones).containsOnly("Bearer token-del-instructor");
    }

    @Test
    void alumnosDelInstructor_sinAlumnos_devuelveUnaListaVaciaConUnaSolaPeticion() {
        total = 0;

        assertThat(cliente(5).alumnosDelInstructor("Bearer t")).isEmpty();
        assertThat(peticiones).hasSize(1);
    }

    @Test
    void alumnosDelInstructor_cortaEnElTopeDeQuinientos() {
        total = 730;

        List<UUID> resultado = cliente(5).alumnosDelInstructor("Bearer t");

        assertThat(resultado).hasSize(500);
        assertThat(peticiones).hasSize(5);
    }

    @Test
    void alumnosDelInstructor_conExactamenteQuinientos_losTrae() {
        total = 500;

        assertThat(cliente(5).alumnosDelInstructor("Bearer t")).hasSize(500);
    }

    @Test
    void esMiAlumno_con200esVerdadero_yCon404esFalso() {
        UUID mio = UUID.randomUUID();
        alumnos = List.of(mio);

        assertThat(cliente(5).esMiAlumno(mio, "Bearer t")).isTrue();
        assertThat(cliente(5).esMiAlumno(UUID.randomUUID(), "Bearer t")).isFalse();
        assertThat(autorizaciones).containsOnly("Bearer t");
        assertThat(peticiones.get(0)).isEqualTo("/api/v1/asignaciones-alumno/mis-alumnos/" + mio);
    }

    @Test
    void cualquierErrorDeEscuela_esServicioNoDisponible() {
        for (int codigo : new int[] {400, 401, 403, 429, 500, 503}) {
            estadoFijo = codigo;
            assertThatThrownBy(() -> cliente(5).alumnosDelInstructor("Bearer t"))
                .as("listado con %s", codigo)
                .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE));
            assertThatThrownBy(() -> cliente(5).esMiAlumno(UUID.randomUUID(), "Bearer t"))
                .as("confirmación con %s", codigo)
                .isInstanceOf(ServicioNoDisponibleException.class);
        }
    }

    @Test
    void unaRespuestaSinLosDatosEsperados_esServicioNoDisponible() throws IOException {
        servidor.stop(0);
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            byte[] bytes = "{\"status\":\"success\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();

        assertThatThrownBy(() -> cliente(5).alumnosDelInstructor("Bearer t"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void unaRespuestaLenta_vencePorTimeout_yEsServicioNoDisponible() {
        total = 1;
        demoraMs = 2500;

        assertThatThrownBy(() -> cliente(1).alumnosDelInstructor("Bearer t"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void sinConexion_esServicioNoDisponible() {
        servidor.stop(0);

        assertThatThrownBy(() -> cliente(1).alumnosDelInstructor("Bearer t"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
