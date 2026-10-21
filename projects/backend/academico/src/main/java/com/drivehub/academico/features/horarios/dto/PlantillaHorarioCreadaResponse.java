package com.drivehub.academico.features.horarios.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * El horario recién creado más cuántas franjas se generaron con él.
 */
public record PlantillaHorarioCreadaResponse(
        UUID codigo,
        int diaSemana,
        String horaInicio,
        String horaFin,
        int duracionClaseMin,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean activa,
        int franjasGeneradas) {
}
