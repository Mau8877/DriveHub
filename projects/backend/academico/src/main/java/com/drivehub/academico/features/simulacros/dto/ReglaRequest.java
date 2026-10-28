package com.drivehub.academico.features.simulacros.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Una regla del simulacro (CU20): cuántas preguntas de un tema entran en cada intento.
 */
public record ReglaRequest(
        @NotBlank(message = "El tema es obligatorio")
        @Size(max = 80, message = "El tema no puede superar 80 caracteres")
        String tema,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad va de 1 a 100")
        @Max(value = 100, message = "La cantidad va de 1 a 100")
        Integer cantidad
) {
}
