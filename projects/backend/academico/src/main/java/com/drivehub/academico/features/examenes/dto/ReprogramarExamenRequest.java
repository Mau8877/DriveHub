package com.drivehub.academico.features.examenes.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de PATCH /api/v1/examenes-conduccion/{codigo} (CU22). Todo es opcional: lo que no se envía no se toca. Solo
 * un examen programado se puede reprogramar; el estudiante y la categoría no cambian (se cancela y se programa otro).
 */
public record ReprogramarExamenRequest(
        LocalDate fecha,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora debe ser HH:mm")
        String hora,

        UUID evaluadorCodigo,

        UUID vehiculoCodigo
) {
}
