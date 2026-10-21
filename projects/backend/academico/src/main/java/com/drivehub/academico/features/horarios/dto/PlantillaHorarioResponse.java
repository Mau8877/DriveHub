package com.drivehub.academico.features.horarios.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Un horario semanal del instructor. Sin ids internos ni vehículo.
 */
public record PlantillaHorarioResponse(
        UUID codigo,
        int diaSemana,
        String horaInicio,
        String horaFin,
        int duracionClaseMin,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean activa) {
}
