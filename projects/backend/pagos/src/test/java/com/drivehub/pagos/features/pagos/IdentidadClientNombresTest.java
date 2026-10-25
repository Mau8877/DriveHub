package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.sun.net.httpserver.HttpServer;

/**
 * La consulta de nombres de estudiantes del cliente de Identidad (CU29) contra un servidor del JDK que hace de
 * Identidad: responde un nombre por cada código pedido, salvo los que se marquen como desconocidos.
 */
class IdentidadClientNombresTest {

    private static final Pattern CODIGO = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private HttpServer servidor;
    private final List<String> cuerpos = new CopyOnWriteArrayList<>();
    private final List<String> autorizaciones = new CopyOnWriteArrayList<>();
    private final List<String> rutas = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile long demoraMs = 0;
    private volatile List<UUID> desconocidos = List.of();

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            cuerpos.add(cuerpo);
            rutas.add(intercambio.getRequestURI().getPath() + " " + intercambio.getRequestMethod());
            autorizaciones.add(intercambio.getRequestHeaders().getFirst("Authorization"));
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            List<String> personas = new CopyOnWriteArrayList<>();
            Matcher m = CODIGO.matcher(cuerpo);
            while (m.find()) {
                UUID codigo = UUID.fromString(m.group());
                if (!desconocidos.contains(codigo)) {
                    personas.add("{\"codigo\":\"" + codigo + "\",\"nombres\":\"Ana\",\"apellidos\":\"Pérez\"}");
                }
            }
            String respuesta = estado == 200
                ? "{\"status\":\"success\",\"data\":[" + String.join(",", personas) + "]}"
                : "{\"status\":\"failed\",\"error\":\"ERR_IDE_XX\",\"message\":\"x\"}";
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    private IdentidadClient cliente(long timeoutSegundos) {
        return new IdentidadClient("http://127.0.0.1:" + servidor.getAddress().getPort(), timeoutSegundos);
    }

    private static List<UUID> codigos(int cantidad) {
        return IntStream.range(0, cantidad).mapToObj(i -> new UUID(0, i + 1)).collect(Collectors.toList());
    }

    @Test
    void nombresDe_devuelveUnMapaCodigoANombreCompleto_yReenviaElBearer() {
        UUID a = UUID.randomUUID();

        Map<UUID, IdentidadClient.NombreEstudiante> nombres = cliente(5).nombresDe(List.of(a), "Bearer token-del-instructor");

        assertThat(nombres).containsEntry(a, new IdentidadClient.NombreEstudiante("Ana", "Pérez"));
        assertThat(nombres.get(a).completo()).isEqualTo("Ana Pérez");
        assertThat(rutas).containsExactly("/api/v1/estudiantes/consulta POST");
        assertThat(autorizaciones).containsExactly("Bearer token-del-instructor");
        assertThat(cuerpos.get(0)).contains("\"codigos\"").contains(a.toString());
    }

    @Test
    void nombresDe_conMasDeCienCodigos_losPideEnLotesDeCien() {
        Map<UUID, IdentidadClient.NombreEstudiante> nombres = cliente(5).nombresDe(codigos(250), "Bearer t");

        assertThat(nombres).hasSize(250);
        assertThat(cuerpos).hasSize(3);
        assertThat(CODIGO.matcher(cuerpos.get(0)).results().count()).isEqualTo(100);
        assertThat(CODIGO.matcher(cuerpos.get(2)).results().count()).isEqualTo(50);
    }

    @Test
    void nombresDe_losQueNoVuelvenNoEstanEnElMapa() {
        UUID conocido = UUID.randomUUID();
        UUID desconocido = UUID.randomUUID();
        desconocidos = List.of(desconocido);

        Map<UUID, IdentidadClient.NombreEstudiante> nombres =
            cliente(5).nombresDe(List.of(conocido, desconocido), "Bearer t");

        assertThat(nombres).containsOnlyKeys(conocido);
    }

    @Test
    void nombresDe_sinCodigos_noLlamaAIdentidad() {
        assertThat(cliente(5).nombresDe(List.of(), "Bearer t")).isEmpty();
        assertThat(cuerpos).isEmpty();
    }

    @Test
    void nombresDe_quitaLosCodigosRepetidosAntesDePedir() {
        UUID a = UUID.randomUUID();

        cliente(5).nombresDe(List.of(a, a, a), "Bearer t");

        assertThat(CODIGO.matcher(cuerpos.get(0)).results().count()).isEqualTo(1);
    }

    @Test
    void nombresDe_conUnErrorDeIdentidad_esServicioNoDisponible() {
        for (int codigo : new int[] {400, 401, 403, 429, 500, 503}) {
            estado = codigo;
            assertThatThrownBy(() -> cliente(5).nombresDe(List.of(UUID.randomUUID()), "Bearer t"))
                .as("estado %s", codigo)
                .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE));
        }
    }

    @Test
    void nombresDe_conUnaRespuestaLenta_vencePorTimeout() {
        demoraMs = 2500;

        assertThatThrownBy(() -> cliente(1).nombresDe(List.of(UUID.randomUUID()), "Bearer t"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
