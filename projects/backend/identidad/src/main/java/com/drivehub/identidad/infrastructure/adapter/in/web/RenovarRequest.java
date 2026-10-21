package com.drivehub.identidad.infrastructure.adapter.in.web;

import com.drivehub.identidad.domain.model.Dispositivo;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de POST /api/v1/auth/refresh (CU02).
 *
 * * En la web el refresh token sale de la cookie `refreshToken`; solo en el
 * * móvil viaja en el cuerpo.
 */
public record RenovarRequest(
        @NotNull(message = "El dispositivo es obligatorio")
        Dispositivo dispositivo,

        String refreshToken
) {
}
