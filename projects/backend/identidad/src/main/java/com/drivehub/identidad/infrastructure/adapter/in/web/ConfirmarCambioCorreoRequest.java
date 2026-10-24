package com.drivehub.identidad.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /api/v1/auth/cambiar-correo/confirmar (CU06): el token del enlace del correo nuevo.
 */
public record ConfirmarCambioCorreoRequest(
        @NotBlank(message = "El enlace es obligatorio")
        String token
) {
}
