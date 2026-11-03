package com.drivehub.academico.features.reservas.dto;

/**
 * Cómo quedó una cancelación hecha por el instructor (CU16).
 *
 * @param clase la clase ya `CANCELADA` (aunque la hora todavía se esté devolviendo)
 * @param horaDevuelta `DEVUELTA` o `EN_PROCESO`; nunca `PERDIDA`: al estudiante siempre se le devuelve la hora
 */
public record CancelacionInstructorResponse(ClaseAgendaResponse clase, HoraDeCancelacion horaDevuelta) {
}
