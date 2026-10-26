package com.drivehub.academico.features.reservas.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Una franja que no se pudo reservar (CU14), con el motivo para mostrárselo al estudiante.
 *
 * * `fecha`, `horaInicio`, `horaFin` e `instructorCodigo` son nulos cuando la franja no existe en la escuela del
 * * estudiante: no hay nada que mostrar de ella.
 *
 * @param error el código catalogado del motivo (`ERR_ACA_10`, `ERR_ACA_17` a `ERR_ACA_20`)
 */
public record FranjaRechazada(UUID franjaCodigo, LocalDate fecha, String horaInicio, String horaFin,
        UUID instructorCodigo, String error, String mensaje) {
}
