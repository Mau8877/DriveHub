package com.drivehub.academico.features.preguntas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Una opción de respuesta al crear o editar una pregunta (CU19). El `id` (A, B, C...) lo pone el
 * servicio según el orden: el cliente solo manda el texto y si es correcta.
 */
public record OpcionRequest(
        @NotBlank(message = "El texto de cada opción es obligatorio")
        @Size(max = 200, message = "Cada opción puede tener hasta 200 caracteres")
        String texto,

        boolean correcta
) {
}
