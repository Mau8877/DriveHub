package com.drivehub.pagos.features.pagos.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Qué plan quiere comprar el estudiante (CU26). La escuela y el estudiante salen del token, no del cuerpo.
 */
public record IniciarPagoRequest(
        @NotNull(message = "Elige el plan que quieres comprar") UUID planCodigo) {
}
