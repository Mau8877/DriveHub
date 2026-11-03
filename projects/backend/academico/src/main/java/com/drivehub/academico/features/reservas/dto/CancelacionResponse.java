package com.drivehub.academico.features.reservas.dto;

/**
 * Cómo quedó una cancelación (CU15).
 *
 * @param reserva la clase cancelada (ya con `estado: CANCELADA`, aunque la hora todavía se esté devolviendo)
 * @param horaDevuelta qué pasó con la hora del estudiante
 * @param horasDisponibles el saldo real tras cancelar; `null` si Pagos no respondió o la hora aún está en proceso
 */
public record CancelacionResponse(ReservaResponse reserva, HoraDeCancelacion horaDevuelta, Integer horasDisponibles) {
}
