package com.drivehub.identidad.infrastructure.adapter.in.web;

import com.drivehub.identidad.domain.model.Dispositivo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de POST /api/v1/auth/login (CU02).
 */
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        @NotNull(message = "El dispositivo es obligatorio")
        Dispositivo dispositivo
) {
}
