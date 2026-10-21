package com.drivehub.escuela.presentation.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PATCH /api/v1/escuelas/{codigo} (CU05). El nombre es lo único editable.
 */
public record EditarEscuelaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombre
) {
}
