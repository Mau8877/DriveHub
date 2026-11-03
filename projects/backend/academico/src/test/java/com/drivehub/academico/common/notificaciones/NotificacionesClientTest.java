package com.drivehub.academico.common.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import com.sun.net.httpserver.HttpServer;

/**
 * El cliente de Notificaciones (CU36) contra un servidor del JDK que hace de Notificaciones.
 */
class NotificacionesClientTest {

    private static final UUID EVENTO = UUID.fromString("0198f0a0-0000-7000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("0198f0a0-0000-7000-8000-000000000002");

    private HttpServer servidor;
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> credencial = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicInteger llamadas = new AtomicInteger();
    private volatile int estado = 200;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            llamadas.incrementAndGet();
            ruta.set(intercambio.getRequestURI().getPath());
            credencial.set(intercambio.getRequestHeaders().getFirst("X-Service-Key"));
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = "{\"status\":\"success\"}".getBytes(StandardCharsets.UTF_8);
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

    private NotificacionesClient cliente(String clave) {
        return new NotificacionesClient("http://127.0.0.1:" + servidor.getAddress().getPort(), 2, clave);
    }

    @Test
    void avisar_mandaLaCredencialYElCuerpoDelPedido() {
        cliente("clave-secreta").avisar(EVENTO, USUARIO, "PAGO", "Compra confirmada", "Acreditamos tus horas",
            Map.of("pagoCodigo", "abc"));

        assertThat(ruta.get()).isEqualTo("/internal/v1/notificaciones");
        assertThat(credencial.get()).isEqualTo("clave-secreta");
        assertThat(cuerpo.get()).contains(EVENTO.toString(), USUARIO.toString(), "\"tipo\":\"PAGO\"",
            "\"titulo\":\"Compra confirmada\"", "\"pagoCodigo\":\"abc\"");
    }

    @Test
    void avisar_sinCredencial_noHaceNingunaPeticion() {
        cliente("").avisar(EVENTO, USUARIO, "PAGO", "t", "m", null);

        assertThat(llamadas.get()).isZero();
    }

    @Test
    void avisar_siNotificacionesRechazaOFalla_noLanzaNada() {
        estado = 401;
        assertThatCode(() -> cliente("mala").avisar(EVENTO, USUARIO, "PAGO", "t", "m", null)).doesNotThrowAnyException();

        estado = 503;
        assertThatCode(() -> cliente("clave").avisar(EVENTO, USUARIO, "PAGO", "t", "m", null)).doesNotThrowAnyException();
    }

    @Test
    void avisar_conNotificacionesCaido_noLanzaNada() {
        servidor.stop(0);

        assertThatCode(() -> cliente("clave").avisar(EVENTO, USUARIO, "PAGO", "t", "m", null))
            .doesNotThrowAnyException();
    }

    @Test
    void eventoCodigo_esDeterministaPorEventoYRecurso() {
        UUID pago = UUID.randomUUID();

        assertThat(NotificacionesClient.eventoCodigo("pago.confirmado", pago))
            .isEqualTo(NotificacionesClient.eventoCodigo("pago.confirmado", pago))
            .isNotEqualTo(NotificacionesClient.eventoCodigo("pago.vencido", pago))
            .isNotEqualTo(NotificacionesClient.eventoCodigo("pago.confirmado", UUID.randomUUID()));
    }
}
