package com.drivehub.identidad.infrastructure.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/usuarios/me/cambiar-correo (CU06).
 */
public record CambiarCorreoRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @NotBlank(message = "El correo nuevo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String correoNuevo
) {
}
