package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.PagosProperties;
import com.drivehub.pagos.features.pagos.pasarela.AvisoPasarela;
import com.drivehub.pagos.features.pagos.pasarela.DevolucionCreada;
import com.drivehub.pagos.features.pagos.pasarela.SolicitudDevolucion;
import com.drivehub.pagos.features.pagos.pasarela.StripePasarela;
import com.drivehub.pagos.features.pagos.pasarela.TipoAviso;
import com.sun.net.httpserver.HttpServer;

/**
 * La devolución en Stripe (CU31) contra un servidor del JDK que hace de API de Stripe, y los avisos firmados de
 * reembolso: nunca se llama a Stripe de verdad.
 */
class StripePasarelaReembolsoTest {

    private static final String CLAVE = "sk_test_clave_de_prueba";
    private static final String SECRETO = "whsec_secreto_de_prueba";
    private static final Instant AHORA = Instant.parse("2030-01-01T00:00:00Z");
    private static final UUID PAGO = UUID.randomUUID();
    private static final UUID REVERSION = UUID.randomUUID();

    private HttpServer servidor;
    private final Map<String, String> cabeceras = new HashMap<>();
    private final Map<String, String> formulario = new HashMap<>();
    private final AtomicInteger llamadas = new AtomicInteger();
    private volatile int estado = 200;
    private volatile String respuesta;

    @BeforeEach
    void levantarServidor() throws IOException {
        respuesta = "{\"id\":\"re_test_1\",\"object\":\"refund\",\"status\":\"pending\",\"amount\":3632}";
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

    private StripePasarela pasarela(String clave) {
        PagosProperties.Pasarela config = new PagosProperties.Pasarela(clave, SECRETO, 30, "https://retorno",
            "https://app", "http://127.0.0.1:" + servidor.getAddress().getPort(), 300);
        return new StripePasarela(PagosPropertiesPrueba.conPasarela(config), Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static SolicitudDevolucion solicitud() {
        return new SolicitudDevolucion("pi_test_456", 3632L, REVERSION.toString(), PAGO, REVERSION);
    }

    // ------------------------------------------------------------------ crear la devolución

    @Test
    void reembolsar_mandaCobroMontoClaveDeIdempotenciaYMetadata() {
        DevolucionCreada devolucion = pasarela(CLAVE).reembolsar(solicitud());

        assertThat(cabeceras.get("metodo")).isEqualTo("POST");
        assertThat(cabeceras.get("ruta")).isEqualTo("/v1/refunds");
        assertThat(cabeceras.get("authorization")).isEqualTo("Bearer " + CLAVE);
        assertThat(cabeceras.get("idempotency-key")).isEqualTo(REVERSION.toString());
        assertThat(formulario.get("payment_intent")).isEqualTo("pi_test_456");
        assertThat(formulario.get("amount")).isEqualTo("3632");
        assertThat(formulario.get("metadata[pagoCodigo]")).isEqualTo(PAGO.toString());
        assertThat(formulario.get("metadata[reversionCodigo]")).isEqualTo(REVERSION.toString());
        assertThat(devolucion.referenciaExterna()).isEqualTo("re_test_1");
        assertThat(devolucion.estado()).isEqualTo("pending");
    }

    @Test
    void reembolsar_conErrorDeLaPasarela_esServicioNoDisponible() {
        estado = 500;
        respuesta = "{\"error\":{\"type\":\"api_error\",\"message\":\"algo fallo\"}}";

        assertThatThrownBy(() -> pasarela(CLAVE).reembolsar(solicitud()))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.PASARELA_NO_DISPONIBLE);
                assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            });
    }

    @Test
    void reembolsar_conLaPasarelaCaida_esServicioNoDisponible() {
        StripePasarela pasarela = pasarela(CLAVE);
        servidor.stop(0);

        assertThatThrownBy(() -> pasarela.reembolsar(solicitud())).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void reembolsar_sinClaveConfigurada_esServicioNoDisponibleSinLlamar() {
        assertThatThrownBy(() -> pasarela("").reembolsar(solicitud()))
            .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(llamadas.get()).isZero();
    }

    @Test
    void reembolsar_conUnaRespuestaSinId_esServicioNoDisponible() {
        respuesta = "{\"object\":\"refund\"}";

        assertThatThrownBy(() -> pasarela(CLAVE).reembolsar(solicitud()))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    // ------------------------------------------------------------------ avisos de reembolso

    private static String devolucion(String tipoDeEvento, String estadoDeLaDevolucion, String metadata) {
        return """
            {"id":"evt_9","object":"event","api_version":"2025-01-01","created":1893456000,"livemode":false,
             "pending_webhooks":1,"request":{"id":null,"idempotency_key":null},"type":"%s",
             "data":{"object":{"id":"re_test_1","object":"refund","amount":3632,"currency":"usd",
                     "payment_intent":"pi_test_456","status":"%s","metadata":%s}}}
            """.formatted(tipoDeEvento, estadoDeLaDevolucion, metadata);
    }

    private static String metadataCompleta() {
        return "{\"pagoCodigo\":\"" + PAGO + "\",\"reversionCodigo\":\"" + REVERSION + "\"}";
    }

    private static String firma(String cuerpo) {
        try {
            long marca = AHORA.getEpochSecond();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] firmado = mac.doFinal((marca + "." + cuerpo).getBytes(StandardCharsets.UTF_8));
            return "t=" + marca + ",v1=" + HexFormat.of().formatHex(firmado);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private AvisoPasarela verificar(String cuerpo) {
        return pasarela(CLAVE).verificarAviso(cuerpo, firma(cuerpo));
    }

    @Test
    void unaDevolucionExitosa_seTraduceAReembolsoEjecutado_conSusDatos() {
        AvisoPasarela aviso = verificar(devolucion("refund.updated", "succeeded", metadataCompleta()));

        assertThat(aviso.tipo()).isEqualTo(TipoAviso.REEMBOLSO_EJECUTADO);
        assertThat(aviso.esDeReembolso()).isTrue();
        assertThat(aviso.referenciaExterna()).isEqualTo("re_test_1");
        assertThat(aviso.pagoCodigo()).isEqualTo(PAGO);
        assertThat(aviso.reversionCodigo()).isEqualTo(REVERSION);
        assertThat(aviso.montoCentavos()).isEqualTo(3632L);
        assertThat(aviso.moneda()).isEqualTo("usd");
        assertThat(aviso.cobroReferencia()).isEqualTo("pi_test_456");
    }

    @Test
    void unaDevolucionFallidaOCancelada_seTraduceAReembolsoFallido() {
        assertThat(verificar(devolucion("refund.updated", "failed", metadataCompleta())).tipo())
            .isEqualTo(TipoAviso.REEMBOLSO_FALLIDO);
        assertThat(verificar(devolucion("refund.updated", "canceled", metadataCompleta())).tipo())
            .isEqualTo(TipoAviso.REEMBOLSO_FALLIDO);
        assertThat(verificar(devolucion("refund.failed", "failed", metadataCompleta())).tipo())
            .isEqualTo(TipoAviso.REEMBOLSO_FALLIDO);
    }

    @Test
    void unaDevolucionTodaviaEnCurso_seIgnora() {
        assertThat(verificar(devolucion("refund.updated", "pending", metadataCompleta())).tipo())
            .isEqualTo(TipoAviso.OTRO);
    }

    @Test
    void unAvisoSinMetadataNiId_noRompe_yQuedaComoOtro() {
        String cuerpo = """
            {"id":"evt_9","object":"event","api_version":"2025-01-01","created":1893456000,"livemode":false,
             "pending_webhooks":1,"request":{"id":null,"idempotency_key":null},"type":"refund.updated",
             "data":{"object":{"object":"refund","status":"succeeded"}}}
            """;

        assertThat(verificar(cuerpo).tipo()).isEqualTo(TipoAviso.OTRO);
    }

    @Test
    void unAvisoSinMetadataPeroConId_sePuedeEncontrarPorSuReferencia() {
        AvisoPasarela aviso = verificar(devolucion("refund.updated", "succeeded", "{}"));

        assertThat(aviso.tipo()).isEqualTo(TipoAviso.REEMBOLSO_EJECUTADO);
        assertThat(aviso.reversionCodigo()).isNull();
        assertThat(aviso.pagoCodigo()).isNull();
        assertThat(aviso.referenciaExterna()).isEqualTo("re_test_1");
    }

    @Test
    void elCobroDevuelto_traeSuDevolucionDentro() {
        String cuerpo = """
            {"id":"evt_9","object":"event","api_version":"2025-01-01","created":1893456000,"livemode":false,
             "pending_webhooks":1,"request":{"id":null,"idempotency_key":null},"type":"charge.refunded",
             "data":{"object":{"id":"ch_1","object":"charge","payment_intent":"pi_test_456",
               "refunds":{"object":"list","data":[{"id":"re_test_1","object":"refund","amount":3632,
                 "currency":"usd","payment_intent":"pi_test_456","status":"succeeded","metadata":%s}]}}}}
            """.formatted(metadataCompleta());

        AvisoPasarela aviso = verificar(cuerpo);

        assertThat(aviso.tipo()).isEqualTo(TipoAviso.REEMBOLSO_EJECUTADO);
        assertThat(aviso.reversionCodigo()).isEqualTo(REVERSION);
    }

    @Test
    void elCobroDevueltoSinDevoluciones_seIgnora() {
        String cuerpo = """
            {"id":"evt_9","object":"event","api_version":"2025-01-01","created":1893456000,"livemode":false,
             "pending_webhooks":1,"request":{"id":null,"idempotency_key":null},"type":"charge.refunded",
             "data":{"object":{"id":"ch_1","object":"charge"}}}
            """;

        assertThat(verificar(cuerpo).tipo()).isEqualTo(TipoAviso.OTRO);
    }
}
