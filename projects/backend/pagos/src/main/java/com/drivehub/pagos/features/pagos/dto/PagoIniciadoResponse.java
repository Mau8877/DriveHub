package com.drivehub.pagos.features.pagos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.features.pagos.enums.EstadoPago;

/**
 * La compra recién iniciada (CU26): dónde paga el estudiante y lo que va a pagar. El id interno y la escuela
 * nunca salen.
 *
 * @param urlPago la página de pago de la pasarela; nula si el pago ya no admite pagarse (vencido o fallido)
 * @param montoUsd lo que se cobra, en dólares
 * @param montoBs el costo del plan en bolivianos, como referencia
 */
public record PagoIniciadoResponse(
        UUID codigo,
        EstadoPago estado,
        String urlPago,
        Instant expiraEn,
        String planNombre,
        short horas,
        BigDecimal montoUsd,
        BigDecimal montoBs) {
}
