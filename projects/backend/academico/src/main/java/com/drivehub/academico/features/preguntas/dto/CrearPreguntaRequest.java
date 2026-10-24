package com.drivehub.academico.features.preguntas.dto;

import java.util.List;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/preguntas (CU19). La escuela NO viaja en el cuerpo: sale del token.
 *
 * * Las reglas que cruzan campos (cuántas opciones y cuántas correctas según el tipo) las valida el
 * * servicio: las anotaciones solo cubren cada campo por separado.
 */
public record CrearPreguntaRequest(
        @NotBlank(message = "La categoría es obligatoria")
        @Pattern(regexp = "^(GENERAL|[A-Z]{1,3})$",
            message = "La categoría debe ser GENERAL o una sigla de hasta 3 letras mayúsculas")
        String categoria,

        @NotBlank(message = "El tema es obligatorio")
        @Size(max = 80, message = "El tema no puede superar 80 caracteres")
        String tema,

        @NotNull(message = "El tipo de pregunta es obligatorio")
        TipoPregunta tipo,

        @NotBlank(message = "El enunciado es obligatorio")
        @Size(max = 500, message = "El enunciado no puede superar 500 caracteres")
        String enunciado,

        @NotEmpty(message = "Agrega las opciones de respuesta")
        @Size(max = 6, message = "Una pregunta puede tener hasta 6 opciones")
        @Valid
        List<OpcionRequest> opciones,

        @Size(max = 500, message = "La explicación no puede superar 500 caracteres")
        String explicacion,

        @NotNull(message = "La dificultad es obligatoria")
        @Min(value = 1, message = "La dificultad va de 1 a 3")
        @Max(value = 3, message = "La dificultad va de 1 a 3")
        Integer dificultad
) {
}
