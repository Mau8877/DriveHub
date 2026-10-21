package com.drivehub.identidad.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/auth/restablecer (CU04): el token del enlace del correo y
 * la contraseña nueva, con las mismas reglas que el registro (CU01).
 */
public record RestablecerRequest(
        @NotBlank(message = "El enlace es obligatorio")
        String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 9, max = 64, message = "La contraseña debe tener entre 9 y 64 caracteres")
        String password
) {
}
