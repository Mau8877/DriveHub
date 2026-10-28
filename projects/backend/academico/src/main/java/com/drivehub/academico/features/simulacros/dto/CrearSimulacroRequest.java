package com.drivehub.academico.features.simulacros.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/simulacros (CU20). La escuela NO viaja en el cuerpo: sale del token.
 *
 * * Las reglas que cruzan campos (la suma de las reglas debe ser `nroPreguntas`, sin temas repetidos) las
 * * valida el servicio: las anotaciones solo cubren cada campo por separado.
 */
public record CrearSimulacroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "La categoría es obligatoria")
        @Pattern(regexp = "^(GENERAL|[A-Z]{1,3})$",
            message = "La categoría debe ser GENERAL o una sigla de hasta 3 letras mayúsculas")
        String categoria,

        @NotNull(message = "La duración es obligatoria")
        @Min(value = 5, message = "La duración va de 5 a 180 minutos")
        @Max(value = 180, message = "La duración va de 5 a 180 minutos")
        Integer duracionMin,

        @NotNull(message = "El número de preguntas es obligatorio")
        @Min(value = 1, message = "El número de preguntas va de 1 a 100")
        @Max(value = 100, message = "El número de preguntas va de 1 a 100")
        Integer nroPreguntas,

        @NotNull(message = "El puntaje de aprobación es obligatorio")
        @DecimalMin(value = "1", message = "El puntaje de aprobación va de 1 a 100")
        @DecimalMax(value = "100", message = "El puntaje de aprobación va de 1 a 100")
        Double puntajeAprobacion,

        @NotEmpty(message = "Agrega al menos un tema")
        @Size(max = 10, message = "Un simulacro puede tener hasta 10 temas")
        @Valid
        List<ReglaRequest> reglas
) {
}
