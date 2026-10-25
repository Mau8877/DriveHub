package com.drivehub.pagos.features.pagos.pasarela;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.config.PagosProperties;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link PasarelaPago} sobre Stripe Checkout: Stripe aloja la página de pago, así que ningún dato de tarjeta
 * pasa por DriveHub.
 *
 * * Cobra en dólares (`usd`): Stripe no cobra en bolivianos.
 * * La clave de idempotencia de Stripe es la del pago: reintentar no crea dos sesiones.
 * * Sin clave configurada, un error de la pasarela o un corte de red son lo mismo para el cliente: 503.
 * * Los avisos de la pasarela se verifican con su firma y el secreto del aviso (`STRIPE_WEBHOOK_SECRET`) antes de
 * * mirar nada de su contenido.
 * ! Nunca se loguean las claves, el secreto, la firma, el cuerpo del aviso ni datos de tarjeta; solo el tipo del error.
 */
@Component
@RequiredArgsConstructor
public class StripePasarela implements PasarelaPago {

    private static final Logger LOG = LoggerFactory.getLogger(StripePasarela.class);
    private static final String MODULO = "pagos";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos conectar con el pago. Inténtalo de nuevo más tarde.";
    private static final int TIMEOUT_CONEXION_MS = 3_000;
    private static final int TIMEOUT_LECTURA_MS = 10_000;

    private final PagosProperties propiedades;
    private final Clock reloj;

    @Override
    public SesionCobro crearSesionCobro(SolicitudCobro solicitud) {
        PagosProperties.Pasarela config = propiedades.pasarela();
        if (config.stripeSecretKey() == null || config.stripeSecretKey().isBlank()) {
            LOG.warn("No hay clave de la pasarela configurada", kv("module", MODULO));
            throw noDisponible();
        }
        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setClientReferenceId(solicitud.pagoCodigo().toString())
            .putMetadata("pagoCodigo", solicitud.pagoCodigo().toString())
            .setExpiresAt(solicitud.expiraEn().getEpochSecond())
            .setSuccessUrl(solicitud.urlExito())
            .setCancelUrl(solicitud.urlCancelacion())
            .addLineItem(SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("usd")
                    .setUnitAmount(solicitud.montoCentavosUsd())
                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(solicitud.nombreProducto())
                        .build())
                    .build())
                .build())
            .build();
        RequestOptions.RequestOptionsBuilder opciones = RequestOptions.builder()
            .setApiKey(config.stripeSecretKey())
            .setIdempotencyKey(solicitud.idempotencyKey())
            .setConnectTimeout(TIMEOUT_CONEXION_MS)
            .setReadTimeout(TIMEOUT_LECTURA_MS)
            .setMaxNetworkRetries(0);
        if (config.stripeBaseUrl() != null && !config.stripeBaseUrl().isBlank()) {
            opciones.setBaseUrl(config.stripeBaseUrl());
        }
        Session sesion;
        try {
            sesion = Session.create(params, opciones.build());
        } catch (StripeException | RuntimeException e) {
            LOG.warn("Falló la llamada a la pasarela para crear la sesión de cobro (tipo {})",
                e.getClass().getSimpleName(), kv("module", MODULO));
            throw noDisponible();
        }
        if (sesion.getId() == null || sesion.getUrl() == null) {
            LOG.warn("La pasarela respondió una sesión incompleta", kv("module", MODULO));
            throw noDisponible();
        }
        Instant expira = sesion.getExpiresAt() == null ? solicitud.expiraEn()
            : Instant.ofEpochSecond(sesion.getExpiresAt());
        return new SesionCobro(sesion.getId(), sesion.getUrl(), expira);
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(PagosErrorCodes.PASARELA_NO_DISPONIBLE, MENSAJE_NO_DISPONIBLE);
    }

    @Override
    public AvisoPasarela verificarAviso(String cuerpo, String firma) {
        PagosProperties.Pasarela config = propiedades.pasarela();
        if (cuerpo == null || firma == null || firma.isBlank() || config.stripeWebhookSecret() == null
                || config.stripeWebhookSecret().isBlank()) {
            LOG.warn("Aviso de la pasarela sin firma o sin secreto configurado para verificarla", kv("module", MODULO));
            throw avisoInvalido();
        }
        Event evento;
        try {
            evento = Webhook.constructEvent(cuerpo, firma, config.stripeWebhookSecret(),
                config.toleranciaFirmaSegundos(), reloj);
        } catch (SignatureVerificationException | RuntimeException e) {
            LOG.warn("Aviso de la pasarela con firma inválida (tipo {})", e.getClass().getSimpleName(),
                kv("module", MODULO));
            throw avisoInvalido();
        }
        // * La firma ya está verificada: se lee el contenido del cuerpo como un árbol, sin depender de la versión
        // * de la API con que la pasarela armó el aviso
        JsonNode objeto = objetoDelAviso(cuerpo);
        return switch (evento.getType() == null ? "" : evento.getType()) {
            case "checkout.session.completed" -> "paid".equals(texto(objeto, "payment_status"))
                ? deLaSesion(evento.getId(), TipoAviso.COBRO_COMPLETADO, objeto) : AvisoPasarela.otro(evento.getId());
            case "checkout.session.expired" -> deLaSesion(evento.getId(), TipoAviso.COBRO_VENCIDO, objeto);
            default -> AvisoPasarela.otro(evento.getId());
        };
    }

    private static AvisoPasarela deLaSesion(String eventoId, TipoAviso tipo, JsonNode sesion) {
        return new AvisoPasarela(eventoId, tipo, texto(sesion, "id"), uuid(texto(sesion, "client_reference_id")),
            numero(sesion, "amount_total"), texto(sesion, "currency"), texto(sesion, "payment_intent"));
    }

    private static JsonNode objetoDelAviso(String cuerpo) {
        return JSON.readTree(cuerpo).path("data").path("object");
    }

    private static String texto(JsonNode objeto, String campo) {
        JsonNode valor = objeto.path(campo);
        return valor.isTextual() ? valor.asText() : null;
    }

    private static Long numero(JsonNode objeto, String campo) {
        JsonNode valor = objeto.path(campo);
        return valor.isNumber() ? valor.asLong() : null;
    }

    private static UUID uuid(String texto) {
        try {
            return texto == null ? null : UUID.fromString(texto);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ValidationException avisoInvalido() {
        return new ValidationException(PagosErrorCodes.AVISO_FIRMA_INVALIDA, "El aviso no se pudo verificar");
    }
}
