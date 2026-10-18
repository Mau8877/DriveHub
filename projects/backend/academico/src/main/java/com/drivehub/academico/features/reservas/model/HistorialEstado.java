package com.drivehub.academico.features.reservas.model;

import java.time.Instant;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;

/**
 * Entrada del historial de estados de una reserva (CU15).
 */
public record HistorialEstado(EstadoReserva estado, Instant en, String motivo) {
}
