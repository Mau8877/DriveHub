package com.drivehub.academico.features.horarios.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;

/**
 * Un horario semanal del instructor. Sin ids internos ni vehículo.
 */
public record PlantillaHorarioResponse(
        UUID codigo,
        String nombre,
        List<Integer> diasSemana,
        String horaInicio,
        String horaFin,
        int duracionClaseMin,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        EstadoPlantilla estado) {
}
