package com.drivehub.academico.features.examenes.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.features.examenes.enums.EstadoExamen;

/**
 * Un examen de conducción en la gestión del Administrador de escuela (CU22): también cada elemento del listado.
 * `fecha` y `hora` son la fecha programada en la hora de la escuela. El id interno Long nunca sale.
 */
public record ExamenResponse(UUID codigo, UUID estudianteCodigo, String estudianteNombre, String categoria,
        Instant fechaProgramada, LocalDate fecha, String hora, UUID evaluadorCodigo, UUID vehiculoCodigo, int intento,
        EstadoExamen estado, Double puntaje, ResultadoEvaluacion resultado, String observaciones) {
}
