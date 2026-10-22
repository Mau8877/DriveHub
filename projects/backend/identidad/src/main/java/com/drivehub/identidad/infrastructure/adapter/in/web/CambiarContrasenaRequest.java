package com.drivehub.identidad.infrastructure.adapter.in.web;

import com.drivehub.identidad.domain.model.Dispositivo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/usuarios/me/cambiar-contrasena (CU06). La confirmación de la contraseña
 * nueva la comprueba el cliente: el backend no la recibe.
 *
 * * `dispositivo` es el del cliente: su sesión se conserva y la del otro se cierra.
 * * La contraseña nueva sigue la regla del registro (9 a 64 caracteres).
 */
public record CambiarContrasenaRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 9, max = 64, message = "La contraseña debe tener entre 9 y 64 caracteres")
        String passwordNueva,

        @NotNull(message = "El dispositivo es obligatorio")
        Dispositivo dispositivo
) {
}
