package com.drivehub.academico.features.examenes.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/examenes-conduccion/{codigo}/registrar-resultado (CU22). El resultado (aprobado o
 * reprobado) lo decide el servicio con el puntaje: no viaja.
 */
public record RegistrarResultadoExamenRequest(
        @NotNull(message = "El puntaje es obligatorio")
        @DecimalMin(value = "0", message = "El puntaje va de 0 a 100")
        @DecimalMax(value = "100", message = "El puntaje va de 0 a 100")
        Double puntaje,

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones
) {
}
