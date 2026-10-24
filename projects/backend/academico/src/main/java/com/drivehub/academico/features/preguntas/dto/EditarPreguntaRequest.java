package com.drivehub.academico.features.preguntas.dto;

import java.util.List;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PATCH /api/v1/preguntas/{codigo} (CU19). Todo es opcional: lo que no se envía no se toca.
 * Si cambia el tipo o las opciones, el servicio revalida el conjunto completo.
 */
public record EditarPreguntaRequest(
        @Pattern(regexp = "^(GENERAL|[A-Z]{1,3})$",
            message = "La categoría debe ser GENERAL o una sigla de hasta 3 letras mayúsculas")
        String categoria,

        @Size(max = 80, message = "El tema no puede superar 80 caracteres")
        String tema,

        TipoPregunta tipo,

        @Size(max = 500, message = "El enunciado no puede superar 500 caracteres")
        String enunciado,

        @Size(max = 6, message = "Una pregunta puede tener hasta 6 opciones")
        @Valid
        List<OpcionRequest> opciones,

        @Size(max = 500, message = "La explicación no puede superar 500 caracteres")
        String explicacion,

        @Min(value = 1, message = "La dificultad va de 1 a 3")
        @Max(value = 3, message = "La dificultad va de 1 a 3")
        Integer dificultad
) {
}
