package com.drivehub.academico.features.horarios.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.features.horarios.enums.EstadoFranja;

/**
 * Una franja reservable. Sin ids internos ni vehículo.
 */
public record FranjaResponse(
        UUID codigo,
        LocalDate fecha,
        String horaInicio,
        String horaFin,
        EstadoFranja estado) {
}
