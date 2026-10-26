package com.drivehub.academico.features.horarios.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Una franja que el estudiante puede reservar (CU14), con el nombre del instructor. Sin ids internos ni vehículo.
 */
public record FranjaDisponibleResponse(
        UUID codigo,
        LocalDate fecha,
        String horaInicio,
        String horaFin,
        UUID instructorCodigo,
        String instructorNombre) {
}
