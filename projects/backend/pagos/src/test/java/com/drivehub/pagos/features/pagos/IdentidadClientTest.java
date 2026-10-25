package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.sun.net.httpserver.HttpServer;

/**
 * El cliente de Identidad (CU26) contra un servidor del JDK que hace de Identidad.
 */
class IdentidadClientTest {

    private HttpServer servidor;
    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private volatile int estado = 200;
    private volatile String respuesta;
    private volatile long demoraMs = 0;

    @BeforeEach
    void levantarServidor() throws IOException {
        respuesta = """
            {"status":"success","message":"Perfil obtenido correctamente","timestamp":"2026-10-25T00:00:00Z",
             "data":{"codigo":"0198f0a0-0000-7000-8000-000000000003","nombres":"María José","apellidos":"Peña Núñez",
                     "email":"m@example.com","roles":["ESTUDIANTE"],"escuelaNombre":"Escuela Example",
                     "ci":"1234567","campoNuevoQueNoConocemos":true}}
            """;
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            autorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            ruta.set(intercambio.getRequestURI().getPath());
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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

    @Test
    void perfilPropio_reenviaElBearerYDevuelveElNombreCompletoYLaEscuela() {
        IdentidadClient.PerfilEstudiante perfil = cliente(5).perfilPropio("Bearer token-del-estudiante");

        assertThat(autorizacion.get()).isEqualTo("Bearer token-del-estudiante");
        assertThat(ruta.get()).isEqualTo("/api/v1/usuarios/me");
        assertThat(perfil.nombreCompleto()).isEqualTo("María José Peña Núñez");
        assertThat(perfil.escuelaNombre()).isEqualTo("Escuela Example");
    }

    @Test
    void perfilPropio_sinEscuela_devuelveLaEscuelaNula() {
        respuesta = "{\"status\":\"success\",\"data\":{\"nombres\":\"Ana\",\"apellidos\":\"Ruiz\"}}";

        assertThat(cliente(5).perfilPropio("Bearer t").escuelaNombre()).isNull();
    }

    @Test
    void perfilPropio_conUnErrorDeIdentidad_esServicioNoDisponible() {
        for (int codigo : new int[] {401, 403, 404, 429, 500, 503}) {
            estado = codigo;
            respuesta = "{\"status\":\"failed\",\"error\":\"ERR_IDE_XX\",\"message\":\"algo\"}";

            assertThatThrownBy(() -> cliente(5).perfilPropio("Bearer t"))
                .as("estado %d", codigo)
                .isInstanceOfSatisfying(ServicioNoDisponibleException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE);
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
        }
    }

    @Test
    void perfilPropio_conRespuestaIncompleta_esServicioNoDisponible() {
        respuesta = "{\"status\":\"success\",\"data\":null}";
        assertThatThrownBy(() -> cliente(5).perfilPropio("Bearer t"))
            .isInstanceOf(ServicioNoDisponibleException.class);

        respuesta = "{\"status\":\"success\",\"data\":{\"nombres\":\"Ana\"}}";
        assertThatThrownBy(() -> cliente(5).perfilPropio("Bearer t"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void perfilPropio_conTimeout_esServicioNoDisponible() {
        demoraMs = 2_500;

        assertThatThrownBy(() -> cliente(1).perfilPropio("Bearer t"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void perfilPropio_conIdentidadCaida_esServicioNoDisponible() {
        IdentidadClient cliente = cliente(5);
        servidor.stop(0);

        assertThatThrownBy(() -> cliente.perfilPropio("Bearer t"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
