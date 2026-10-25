package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.features.pagos.pasarela.AvisoPasarela;
import com.drivehub.pagos.features.pagos.pasarela.StripePasarela;
import com.drivehub.pagos.features.pagos.pasarela.TipoAviso;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * La verificación de los avisos firmados de Stripe (CU27): con cuerpos que el propio test firma con HMAC-SHA256
 * y un secreto de prueba, y la hora fijada por un reloj de prueba.
 */
class StripePasarelaAvisoTest {

    private static final String SECRETO = "whsec_secreto_de_prueba";
    private static final Instant AHORA = Instant.parse("2030-01-01T00:00:00Z");
    private static final UUID PAGO = UUID.randomUUID();

    private StripePasarela pasarela;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void preparar() {
        pasarela = pasarelaCon(SECRETO);
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(StripePasarela.class)).addAppender(logs);
    }

    @AfterEach
    void limpiarLogs() {
        ((Logger) LoggerFactory.getLogger(StripePasarela.class)).detachAppender(logs);
    }

    private static StripePasarela pasarelaCon(String secreto) {
        return new StripePasarela(
            PagosPropertiesPrueba.conPasarela(PagosPropertiesPrueba.pasarelaConAvisos(secreto, 300)),
            Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    /** El cuerpo de un aviso de la sesión de cobro, con los campos que le importan a Pagos. */
    private static String aviso(String tipo, String estadoDelPago, String referenciaDelPago) {
        return """
            {"id":"evt_1","object":"event","api_version":"2025-01-01","created":1893456000,"livemode":false,
             "pending_webhooks":1,"request":{"id":null,"idempotency_key":null},"type":"%s",
             "data":{"object":{"id":"cs_test_123","object":"checkout.session","amount_total":3823,
                     "currency":"usd","payment_status":"%s","client_reference_id":"%s",
                     "payment_intent":"pi_test_456","status":"complete"}}}
            """.formatted(tipo, estadoDelPago, referenciaDelPago);
    }

    private static String firma(String cuerpo, String secreto, Instant momento) {
        try {
            long marca = momento.getEpochSecond();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] firmado = mac.doFinal((marca + "." + cuerpo).getBytes(StandardCharsets.UTF_8));
            return "t=" + marca + ",v1=" + HexFormat.of().formatHex(firmado);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private AvisoPasarela verificar(String cuerpo) {
        return pasarela.verificarAviso(cuerpo, firma(cuerpo, SECRETO, AHORA));
    }

    private void assertRechazado(Runnable accion) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ValidationException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.AVISO_FIRMA_INVALIDA);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        });
    }

    // ------------------------------------------------------------------ avisos válidos

    @Test
    void unCobroCompletadoYPagado_seTraduceConTodosSusDatos() {
        AvisoPasarela aviso = verificar(aviso("checkout.session.completed", "paid", PAGO.toString()));

        assertThat(aviso.tipo()).isEqualTo(TipoAviso.COBRO_COMPLETADO);
        assertThat(aviso.eventoId()).isEqualTo("evt_1");
        assertThat(aviso.referenciaExterna()).isEqualTo("cs_test_123");
        assertThat(aviso.pagoCodigo()).isEqualTo(PAGO);
        assertThat(aviso.montoCentavos()).isEqualTo(3823L);
        assertThat(aviso.moneda()).isEqualTo("usd");
        assertThat(aviso.cobroReferencia()).isEqualTo("pi_test_456");
    }

    @Test
    void unCobroCompletadoPeroSinPagar_noEsUnCobroCompletado() {
        AvisoPasarela aviso = verificar(aviso("checkout.session.completed", "unpaid", PAGO.toString()));

        assertThat(aviso.tipo()).isEqualTo(TipoAviso.OTRO);
    }

    @Test
    void unaPaginaDePagoVencida_seTraduceComoCobroVencido() {
        AvisoPasarela aviso = verificar(aviso("checkout.session.expired", "unpaid", PAGO.toString()));

        assertThat(aviso.tipo()).isEqualTo(TipoAviso.COBRO_VENCIDO);
        assertThat(aviso.referenciaExterna()).isEqualTo("cs_test_123");
        assertThat(aviso.pagoCodigo()).isEqualTo(PAGO);
    }

    @Test
    void cualquierOtroAviso_seIgnoraComoOtro() {
        for (String tipo : new String[] {"charge.refunded", "payment_intent.created", "customer.created", ""}) {
            AvisoPasarela aviso = verificar(aviso(tipo, "paid", PAGO.toString()));

            assertThat(aviso.tipo()).as(tipo).isEqualTo(TipoAviso.OTRO);
            assertThat(aviso.referenciaExterna()).isNull();
        }
    }

    @Test
    void unaReferenciaDePagoQueNoEsUuid_dejaElCodigoDelPagoNulo_yAunAsiTrae_laSesion() {
        AvisoPasarela aviso = verificar(aviso("checkout.session.completed", "paid", "no-es-un-uuid"));

        assertThat(aviso.tipo()).isEqualTo(TipoAviso.COBRO_COMPLETADO);
        assertThat(aviso.pagoCodigo()).isNull();
        assertThat(aviso.referenciaExterna()).isEqualTo("cs_test_123");
    }

    @Test
    void unAvisoSinElObjetoDeLaSesion_noRompe() {
        String cuerpo = """
            {"id":"evt_9","object":"event","api_version":"2025-01-01","created":1893456000,"livemode":false,
             "pending_webhooks":1,"type":"checkout.session.expired","data":{}}
            """;

        AvisoPasarela aviso = verificar(cuerpo);

        assertThat(aviso.tipo()).isEqualTo(TipoAviso.COBRO_VENCIDO);
        assertThat(aviso.referenciaExterna()).isNull();
        assertThat(aviso.montoCentavos()).isNull();
    }

    // ------------------------------------------------------------------ avisos que se rechazan

    @Test
    void unaFirmaDeOtroSecreto_seRechaza() {
        String cuerpo = aviso("checkout.session.completed", "paid", PAGO.toString());

        assertRechazado(() -> pasarela.verificarAviso(cuerpo, firma(cuerpo, "whsec_de_otro", AHORA)));
    }

    @Test
    void unCuerpoAlterado_seRechaza() {
        String cuerpo = aviso("checkout.session.completed", "paid", PAGO.toString());
        String firma = firma(cuerpo, SECRETO, AHORA);

        assertRechazado(() -> pasarela.verificarAviso(cuerpo.replace("3823", "1"), firma));
    }

    @Test
    void unaFirmaConUnCaracterCambiado_seRechaza() {
        String cuerpo = aviso("checkout.session.completed", "paid", PAGO.toString());
        String firma = firma(cuerpo, SECRETO, AHORA);
        String alterada = firma.substring(0, firma.length() - 1) + (firma.endsWith("0") ? "1" : "0");

        assertRechazado(() -> pasarela.verificarAviso(cuerpo, alterada));
    }

    @Test
    void sinCabeceraDeFirma_conUnaVaciaOMalFormada_seRechaza() {
        String cuerpo = aviso("checkout.session.completed", "paid", PAGO.toString());

        assertRechazado(() -> pasarela.verificarAviso(cuerpo, null));
        assertRechazado(() -> pasarela.verificarAviso(cuerpo, ""));
        assertRechazado(() -> pasarela.verificarAviso(cuerpo, "   "));
        assertRechazado(() -> pasarela.verificarAviso(cuerpo, "esto no es una firma"));
        assertRechazado(() -> pasarela.verificarAviso(cuerpo, "t=abc,v1=xyz"));
    }

    @Test
    void sinCuerpo_seRechaza() {
        assertRechazado(() -> pasarela.verificarAviso(null, "t=1,v1=abc"));
    }

    @Test
    void unAvisoViejo_fueraDeLaTolerancia_seRechaza_aunqueEsteBienFirmado() {
        String cuerpo = aviso("checkout.session.completed", "paid", PAGO.toString());

        assertRechazado(() -> pasarela.verificarAviso(cuerpo, firma(cuerpo, SECRETO, AHORA.minusSeconds(301))));
        assertRechazado(() -> pasarela.verificarAviso(cuerpo, firma(cuerpo, SECRETO, AHORA.minusSeconds(3600))));
    }

    @Test
    void dentroDeLaTolerancia_seAcepta() {
        String cuerpo = aviso("checkout.session.completed", "paid", PAGO.toString());

        assertThat(pasarela.verificarAviso(cuerpo, firma(cuerpo, SECRETO, AHORA.minusSeconds(299))).tipo())
            .isEqualTo(TipoAviso.COBRO_COMPLETADO);
    }

    @Test
    void sinElSecretoConfigurado_noSePuedeVerificarNada_yTodoSeRechaza() {
        String cuerpo = aviso("checkout.session.completed", "paid", PAGO.toString());

        String firma = firma(cuerpo, SECRETO, AHORA);

        assertRechazado(() -> pasarelaCon("").verificarAviso(cuerpo, firma));
        assertRechazado(() -> pasarelaCon("   ").verificarAviso(cuerpo, firma));
    }

    @Test
    void unCuerpoQueNoEsJson_aunqueEsteFirmado_seRechaza() {
        String cuerpo = "esto no es json";

        assertRechazado(() -> pasarela.verificarAviso(cuerpo, firma(cuerpo, SECRETO, AHORA)));
    }

    // ------------------------------------------------------------------ registros

    @Test
    void unRechazo_registraUnWarn_sinElCuerpoNiLaFirmaNiElSecreto() {
        String cuerpo = aviso("checkout.session.completed", "paid", PAGO.toString());
        String firma = "t=1893456000,v1=firma-falsa";

        assertRechazado(() -> pasarela.verificarAviso(cuerpo, firma));

        assertThat(logs.list).isNotEmpty();
        String todo = logs.list.stream().map(e -> e.getFormattedMessage() + java.util.Arrays.toString(
            e.getArgumentArray())).reduce("", String::concat);
        assertThat(todo).doesNotContain("cs_test_123").doesNotContain("firma-falsa").doesNotContain(SECRETO)
            .doesNotContain(PAGO.toString()).doesNotContain("3823");
    }
}
