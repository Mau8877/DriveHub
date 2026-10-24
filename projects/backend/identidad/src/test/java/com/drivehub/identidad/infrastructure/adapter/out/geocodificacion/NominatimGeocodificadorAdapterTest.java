package com.drivehub.identidad.infrastructure.adapter.out.geocodificacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * El adaptador contra un Nominatim falso (HttpServer del JDK).
 */
class NominatimGeocodificadorAdapterTest {

    private static final AtomicReference<String> ULTIMA_CONSULTA = new AtomicReference<>();
    private static final AtomicReference<String> ULTIMO_USER_AGENT = new AtomicReference<>();
    private static final HttpServer NOMINATIM_FALSO = levantarNominatimFalso();

    private static final BigDecimal LA_PAZ_LAT = new BigDecimal("-16.489700");
    private static final BigDecimal LA_PAZ_LON = new BigDecimal("-68.119300");

    @AfterAll
    static void apagar() {
        NOMINATIM_FALSO.stop(0);
    }

    private NominatimGeocodificadorAdapter adaptador(String ruta, long timeoutSegundos) {
        return new NominatimGeocodificadorAdapter(
            "http://localhost:" + NOMINATIM_FALSO.getAddress().getPort() + ruta, timeoutSegundos, "DriveHub/test");
    }

    @Test
    void direccionDe_conPuntoConocido_devuelveElDisplayName() {
        assertThat(adaptador("/ok", 3).direccionDe(LA_PAZ_LAT, LA_PAZ_LON))
            .hasValue("Avenida Arce 2345, Sopocachi, La Paz, Bolivia");
    }

    @Test
    void direccionDe_envia_userAgent_idioma_y_coordenadas() {
        adaptador("/ok", 3).direccionDe(LA_PAZ_LAT, LA_PAZ_LON);

        assertThat(ULTIMO_USER_AGENT.get()).isEqualTo("DriveHub/test");
        assertThat(ULTIMA_CONSULTA.get())
            .contains("lat=-16.489700")
            .contains("lon=-68.119300")
            .contains("accept-language=es")
            .contains("format=jsonv2");
    }

    @Test
    void direccionDe_conDireccionMuyLarga_laRecortaA300() {
        assertThat(adaptador("/larga", 3).direccionDe(LA_PAZ_LAT, LA_PAZ_LON))
            .hasValueSatisfying(d -> assertThat(d).hasSize(300));
    }

    @Test
    void direccionDe_conPuntoSinDireccion_devuelveVacio() {
        assertThat(adaptador("/sin-direccion", 3).direccionDe(LA_PAZ_LAT, LA_PAZ_LON)).isEmpty();
    }

    @Test
    void direccionDe_conErrorDelServidor_devuelveVacioSinLanzar() {
        assertThat(adaptador("/rota", 3).direccionDe(LA_PAZ_LAT, LA_PAZ_LON)).isEmpty();
    }

    @Test
    void direccionDe_conCuerpoQueNoEsJson_devuelveVacioSinLanzar() {
        assertThat(adaptador("/basura", 3).direccionDe(LA_PAZ_LAT, LA_PAZ_LON)).isEmpty();
    }

    @Test
    void direccionDe_cuandoElServidorTardaMasQueElTimeout_devuelveVacio() {
        assertThat(adaptador("/lenta", 1).direccionDe(LA_PAZ_LAT, LA_PAZ_LON)).isEmpty();
    }

    @Test
    void direccionDe_conServidorCaido_devuelveVacioSinLanzar() {
        NominatimGeocodificadorAdapter caido =
            new NominatimGeocodificadorAdapter("http://localhost:1", 1, "DriveHub/test");

        assertThat(caido.direccionDe(LA_PAZ_LAT, LA_PAZ_LON)).isEmpty();
    }

    @Test
    void direccionDe_dosLlamadasSeguidas_respetanElEspaciadoDeUnSegundo() {
        NominatimGeocodificadorAdapter adaptador = adaptador("/ok", 3);
        adaptador.direccionDe(LA_PAZ_LAT, LA_PAZ_LON);
        long inicio = System.nanoTime();

        adaptador.direccionDe(LA_PAZ_LAT, LA_PAZ_LON);

        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        assertThat(milisegundos).isGreaterThanOrEqualTo(900);
    }

    private static HttpServer levantarNominatimFalso() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", intercambio -> {
                String ruta = intercambio.getRequestURI().getPath();
                ULTIMA_CONSULTA.set(intercambio.getRequestURI().getRawQuery());
                ULTIMO_USER_AGENT.set(intercambio.getRequestHeaders().getFirst("User-Agent"));
                int estado = 200;
                String cuerpo;
                switch (ruta) {
                    case "/ok/reverse" -> cuerpo = "{\"display_name\":\"Avenida Arce 2345, Sopocachi, La Paz, Bolivia\"}";
                    case "/larga/reverse" -> cuerpo = "{\"display_name\":\"" + "x".repeat(500) + "\"}";
                    case "/sin-direccion/reverse" -> cuerpo = "{\"error\":\"Unable to geocode\"}";
                    case "/basura/reverse" -> cuerpo = "<html>no soy json</html>";
                    case "/lenta/reverse" -> {
                        dormir(2500);
                        cuerpo = "{\"display_name\":\"tarde\"}";
                    }
                    default -> {
                        estado = 500;
                        cuerpo = "{}";
                    }
                }
                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(estado, bytes.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
