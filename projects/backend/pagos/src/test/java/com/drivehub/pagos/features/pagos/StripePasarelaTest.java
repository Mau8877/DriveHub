package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.features.pagos.pasarela.SesionCobro;
import com.drivehub.pagos.features.pagos.pasarela.SolicitudCobro;
import com.drivehub.pagos.features.pagos.pasarela.StripePasarela;
import com.sun.net.httpserver.HttpServer;

/**
 * La pasarela de Stripe (CU26) contra un servidor del JDK que hace de API de Stripe: nunca se llama a Stripe de
 * verdad.
 */
class StripePasarelaTest {

    private static final String CLAVE = "sk_test_clave_de_prueba";

    private HttpServer servidor;
    private final Map<String, String> cabeceras = new HashMap<>();
    private final Map<String, String> formulario = new HashMap<>();
    private final AtomicInteger llamadas = new AtomicInteger();
    private volatile int estado = 200;
    private volatile String respuesta;

    @BeforeEach
    void levantarServidor() throws IOException {
        respuesta = """
            {"id":"cs_test_123","object":"checkout.session",
             "url":"https://checkout.stripe.com/c/pay/cs_test_123","expires_at":1893456000}
            """;
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            llamadas.incrementAndGet();
            intercambio.getRequestHeaders().forEach((k, v) -> cabeceras.put(k.toLowerCase(), v.get(0)));
            cabeceras.put("metodo", intercambio.getRequestMethod());
            cabeceras.put("ruta", intercambio.getRequestURI().getPath());
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            for (String par : cuerpo.split("&")) {
                String[] kv = par.split("=", 2);
                formulario.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
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

    private StripePasarela pasarela(String clave, String baseUrl) {
        return new StripePasarela(PagosPropertiesPrueba.conPasarela(PagosPropertiesPrueba.pasarela(clave, baseUrl)),
            java.time.Clock.systemUTC());
    }

    private String urlServidor() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private SolicitudCobro solicitud(UUID pagoCodigo) {
        return new SolicitudCobro(pagoCodigo, "clave-del-pago", "Plan 10 horas", 3823L,
            Instant.ofEpochSecond(1893456000L), "https://retorno/ok", "https://retorno/cancelado");
    }

    @Test
    void crearSesion_mandaMontoEnCentavosUsdReferenciaVigenciaYClaveDeIdempotencia() {
        UUID pago = UUID.randomUUID();

        SesionCobro sesion = pasarela(CLAVE, urlServidor()).crearSesionCobro(solicitud(pago));

        assertThat(cabeceras.get("metodo")).isEqualTo("POST");
        assertThat(cabeceras.get("ruta")).isEqualTo("/v1/checkout/sessions");
        assertThat(cabeceras.get("authorization")).isEqualTo("Bearer " + CLAVE);
        assertThat(cabeceras.get("idempotency-key")).isEqualTo("clave-del-pago");
        assertThat(formulario.get("mode")).isEqualTo("payment");
        assertThat(formulario.get("client_reference_id")).isEqualTo(pago.toString());
        assertThat(formulario.get("metadata[pagoCodigo]")).isEqualTo(pago.toString());
        assertThat(formulario.get("expires_at")).isEqualTo("1893456000");
        assertThat(formulario.get("success_url")).isEqualTo("https://retorno/ok");
        assertThat(formulario.get("cancel_url")).isEqualTo("https://retorno/cancelado");
        assertThat(formulario.get("line_items[0][quantity]")).isEqualTo("1");
        assertThat(formulario.get("line_items[0][price_data][currency]")).isEqualTo("usd");
        assertThat(formulario.get("line_items[0][price_data][unit_amount]")).isEqualTo("3823");
        assertThat(formulario.get("line_items[0][price_data][product_data][name]")).isEqualTo("Plan 10 horas");
        assertThat(sesion.referenciaExterna()).isEqualTo("cs_test_123");
        assertThat(sesion.urlPago()).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_123");
        assertThat(sesion.expiraEn()).isEqualTo(Instant.ofEpochSecond(1893456000L));
    }

    @Test
    void crearSesion_conErrorDeLaPasarela_esServicioNoDisponible() {
        estado = 500;
        respuesta = "{\"error\":{\"type\":\"api_error\",\"message\":\"algo fallo\"}}";

        assertThatThrownBy(() -> pasarela(CLAVE, urlServidor()).crearSesionCobro(solicitud(UUID.randomUUID())))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.PASARELA_NO_DISPONIBLE);
                assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            });
    }

    @Test
    void crearSesion_conClaveRechazada_esServicioNoDisponible() {
        estado = 401;
        respuesta = "{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Invalid API Key\"}}";

        assertThatThrownBy(() -> pasarela(CLAVE, urlServidor()).crearSesionCobro(solicitud(UUID.randomUUID())))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void crearSesion_conLaPasarelaCaida_esServicioNoDisponible() {
        String url = urlServidor();
        servidor.stop(0);

        assertThatThrownBy(() -> pasarela(CLAVE, url).crearSesionCobro(solicitud(UUID.randomUUID())))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void crearSesion_conRespuestaIncompleta_esServicioNoDisponible() {
        respuesta = "{\"id\":\"cs_test_123\",\"object\":\"checkout.session\"}";

        assertThatThrownBy(() -> pasarela(CLAVE, urlServidor()).crearSesionCobro(solicitud(UUID.randomUUID())))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void crearSesion_sinClaveConfigurada_noLlamaYEsServicioNoDisponible() {
        assertThatThrownBy(() -> pasarela("", urlServidor()).crearSesionCobro(solicitud(UUID.randomUUID())))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.PASARELA_NO_DISPONIBLE));
        assertThat(llamadas.get()).isZero();
    }
}
