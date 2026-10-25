package com.drivehub.pagos.features.pagos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;

/**
 * Una compra en el historial del estudiante (CU28). `montoUsd` es lo cobrado; `montoBs`, el costo del plan en
 * bolivianos al comprar. Nunca lleva el id interno ni la escuela.
 */
public record PagoResumenResponse(
        UUID codigo,
        String planNombre,
        Short horas,
        BigDecimal montoUsd,
        BigDecimal montoBs,
        EstadoPagoVisible estado,
        Instant creadoEn) {
}
