package com.drivehub.academico.features.simulacros.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PATCH /api/v1/simulacros/{codigo} (CU20). Todo es opcional: lo que no se envía no se toca.
 * Si cambia el número de preguntas o las reglas, el servicio revalida que sumen lo mismo.
 */
public record EditarSimulacroRequest(
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @Pattern(regexp = "^(GENERAL|[A-Z]{1,3})$",
            message = "La categoría debe ser GENERAL o una sigla de hasta 3 letras mayúsculas")
        String categoria,

        @Min(value = 5, message = "La duración va de 5 a 180 minutos")
        @Max(value = 180, message = "La duración va de 5 a 180 minutos")
        Integer duracionMin,

        @Min(value = 1, message = "El número de preguntas va de 1 a 100")
        @Max(value = 100, message = "El número de preguntas va de 1 a 100")
        Integer nroPreguntas,

        @DecimalMin(value = "1", message = "El puntaje de aprobación va de 1 a 100")
        @DecimalMax(value = "100", message = "El puntaje de aprobación va de 1 a 100")
        Double puntajeAprobacion,

        @Size(max = 10, message = "Un simulacro puede tener hasta 10 temas")
        @Valid
        List<ReglaRequest> reglas
) {
}
