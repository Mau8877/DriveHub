package com.drivehub.pagos.features.pagos.pasarela;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.PagosProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;

/**
 * {@link PasarelaPago} sobre Stripe Checkout: Stripe aloja la página de pago, así que ningún dato de tarjeta
 * pasa por DriveHub.
 *
 * * Cobra en dólares (`usd`): Stripe no cobra en bolivianos.
 * * La clave de idempotencia de Stripe es la del pago: reintentar no crea dos sesiones.
 * * Sin clave configurada, un error de la pasarela o un corte de red son lo mismo para el cliente: 503.
 * ! Nunca se loguean las claves ni datos de tarjeta; solo el tipo del error.
 */
@Component
@RequiredArgsConstructor
public class StripePasarela implements PasarelaPago {

    private static final Logger LOG = LoggerFactory.getLogger(StripePasarela.class);
    private static final String MODULO = "pagos";
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos conectar con el pago. Inténtalo de nuevo más tarde.";
    private static final int TIMEOUT_CONEXION_MS = 3_000;
    private static final int TIMEOUT_LECTURA_MS = 10_000;

    private final PagosProperties propiedades;

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
}
