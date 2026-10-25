package com.drivehub.pagos.features.planes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un plan de horas tal como lo ve el cliente (CU25). `costoUsd` es solo una referencia, calculada al
 * responder con el tipo de cambio configurado: no existe en la base. El id interno y la escuela no salen.
 */
public record PlanPagoResponse(
        UUID codigo,
        String nombre,
        int horas,
        BigDecimal costoBs,
        BigDecimal costoUsd,
        boolean activo,
        Instant creadoEn) {
}
