package com.drivehub.pagos.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuración de Pagos, prefijo `app.pagos`.
 *
 * * `tipoCambioBsUsd`: bolivianos por dólar, para mostrar el costo de un plan en dólares (CU25) y fijar el
 * * monto a cobrar en dólares (CU26). Sale SOLO de la variable de entorno `TIPO_CAMBIO_BS_USD`: no tiene valor
 * * por defecto en el código ni en `application.properties`, así que no hay un tipo de cambio escondido.
 * * No hay servicio externo: cambiarlo es cambiar la variable, y el costo en dólares de todos los planes se
 * * recalcula solo (los pagos ya creados conservan el que tenían).
 * ! Sin la variable, o si no es mayor a 0, el servicio no arranca: dividir por cero o por un negativo daría
 * ! dólares absurdos.
 * * `pasarela`: la pasarela de pago (Stripe), ver {@link Pasarela}.
 * * `vencimiento`: cada cuánto se vencen los pagos pendientes, ver {@link Vencimiento}.
 * * `retencionReembolsoPorcentaje`: qué porcentaje de lo pagado se retiene al reembolsar (CU28, CU31), de 0
 * * a 50 y 5 por defecto (`REEMBOLSO_RETENCION_PORCENTAJE`); fuera de ese rango el servicio no arranca.
 */
@Validated
@ConfigurationProperties(prefix = "app.pagos")
public record PagosProperties(
        @NotNull @Positive BigDecimal tipoCambioBsUsd,
        @Valid @NotNull Pasarela pasarela,
        @Valid @NotNull @DefaultValue Vencimiento vencimiento,
        @Min(0) @Max(50) @DefaultValue("5") int retencionReembolsoPorcentaje) {

    /**
     * La pasarela de pago, prefijo `app.pagos.pasarela`.
     *
     * * `stripeSecretKey` y `stripeWebhookSecret` salen de variables de entorno (`STRIPE_SECRET_KEY`,
     * * `STRIPE_WEBHOOK_SECRET`). Pueden estar vacías (tests, desarrollo sin cuenta): el servicio arranca
     * * igual y falla recién al intentar cobrar. NUNCA se escriben en el repo.
     * * `vigenciaSesionMinutos`: cuánto dura la página de pago. Stripe exige al menos 30 minutos y admite
     * * hasta 24 horas; fuera de ese rango el servicio no arranca.
     * * `urlRetorno`: la dirección pública de la página a la que vuelve el estudiante al terminar de pagar
     * * (`PAGOS_URL_RETORNO`). Cambia con cada entorno (en el teléfono no sirve `localhost`), por eso es una
     * * variable obligatoria y no tiene valor por defecto.
     * * `enlaceApp`: el enlace con el que esa página abre la app (`PAGOS_ENLACE_APP`); también obligatorio.
     * * `stripeBaseUrl`: vacía = la API real de Stripe; solo la cambian los tests, con un servidor falso.
     * * `toleranciaFirmaSegundos`: cuánto puede diferir la hora de un aviso de la pasarela de la hora del servicio
     * * para que se acepte (por defecto 5 minutos): frena que alguien repita un aviso viejo y bien firmado.
     */
    public record Pasarela(
            @DefaultValue("") String stripeSecretKey,
            @DefaultValue("") String stripeWebhookSecret,
            @Min(30) @Max(1440) @DefaultValue("30") int vigenciaSesionMinutos,
            @NotBlank String urlRetorno,
            @NotBlank String enlaceApp,
            @DefaultValue("") String stripeBaseUrl,
            @Min(1) @DefaultValue("300") long toleranciaFirmaSegundos) {
    }

    /**
     * El proceso que pasa a `FALLIDO` los pagos pendientes cuya página de pago ya venció, prefijo
     * `app.pagos.vencimiento`.
     *
     * * `cadaSegundos`: cada cuánto corre (por defecto 5 minutos; al menos 10 segundos).
     * * `margenMinutos`: cuánto se espera después del vencimiento antes de darlo por fallido, para que un
     * * aviso tardío de la pasarela llegue primero (por defecto 10).
     */
    public record Vencimiento(
            @Min(10) @DefaultValue("300") int cadaSegundos,
            @Min(0) @DefaultValue("10") int margenMinutos) {
    }
}
