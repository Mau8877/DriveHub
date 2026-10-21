package com.drivehub.academico.features.horarios.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;

/**
 * El horario recién creado más cuántas franjas se generaron con él.
 */
public record PlantillaHorarioCreadaResponse(
        UUID codigo,
        String nombre,
        List<Integer> diasSemana,
        String horaInicio,
        String horaFin,
        int duracionClaseMin,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        EstadoPlantilla estado,
        int franjasGeneradas) {
}
