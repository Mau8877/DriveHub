package com.drivehub.pagos.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuración de Pagos, prefijo `app.pagos`.
 *
 * * `tipoCambioBsUsd`: bolivianos por dólar, para mostrar el costo de un plan en dólares (CU25). Por
 * * defecto 6,96, el cambio oficial. No hay servicio externo: cambiarlo es cambiar la variable de
 * * entorno `TIPO_CAMBIO_BS_USD`, y el costo en dólares de todos los planes se recalcula solo.
 * ! Si no es mayor a 0 el servicio no arranca: dividir por cero o por un negativo daría dólares absurdos.
 */
@Validated
@ConfigurationProperties(prefix = "app.pagos")
public record PagosProperties(
        @NotNull @Positive @DefaultValue("6.96") BigDecimal tipoCambioBsUsd) {
}
