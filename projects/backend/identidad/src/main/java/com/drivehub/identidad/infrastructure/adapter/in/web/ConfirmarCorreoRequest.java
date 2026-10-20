package com.drivehub.identidad.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /api/v1/auth/verificar-correo/confirmar (CU04b): el token del enlace del correo.
 */
public record ConfirmarCorreoRequest(
        @NotBlank(message = "El enlace es obligatorio")
        String token
) {
}
