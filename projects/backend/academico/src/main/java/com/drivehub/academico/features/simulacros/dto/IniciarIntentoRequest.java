package com.drivehub.academico.features.simulacros.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de POST /api/v1/intentos-simulacro (CU21).
 */
public record IniciarIntentoRequest(
        @NotNull(message = "El simulacro es obligatorio")
        UUID simulacroCodigo
) {
}
