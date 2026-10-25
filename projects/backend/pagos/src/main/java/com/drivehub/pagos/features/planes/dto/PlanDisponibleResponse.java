package com.drivehub.pagos.features.planes.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un plan que el estudiante puede comprar (CU26). Sin `activo` ni la fecha de alta: solo se ofrecen los
 * activos y al estudiante no le interesa cuándo se crearon. El id interno y la escuela nunca salen.
 */
public record PlanDisponibleResponse(
        UUID codigo,
        String nombre,
        short horas,
        BigDecimal costoBs,
        BigDecimal costoUsd) {
}
