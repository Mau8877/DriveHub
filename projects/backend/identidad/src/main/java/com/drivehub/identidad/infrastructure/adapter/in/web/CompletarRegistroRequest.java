package com.drivehub.identidad.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/v1/registros-whatsapp/completar (CU48). */
public record CompletarRegistroRequest(
        @NotBlank(message = "El enlace es obligatorio")
        String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 9, max = 64, message = "La contraseña debe tener entre 9 y 64 caracteres")
        String password
) {
}
