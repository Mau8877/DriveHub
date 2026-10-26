package com.drivehub.academico.features.reservas.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Una franja que no se pudo reservar (CU14), con el motivo para mostrárselo al estudiante.
 *
 * @param error el código catalogado del motivo (`ERR_ACA_10`, `ERR_ACA_17` a `ERR_ACA_20`)
 */
public record FranjaRechazadaResponse(
        UUID franjaCodigo,
        LocalDate fecha,
        String horaInicio,
        String horaFin,
        String instructorNombre,
        String error,
        String mensaje) {
}
