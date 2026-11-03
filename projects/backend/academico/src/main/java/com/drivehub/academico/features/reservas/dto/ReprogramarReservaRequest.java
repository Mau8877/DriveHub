package com.drivehub.academico.features.reservas.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Pide mover una clase reservada a otra franja libre (CU15).
 */
public record ReprogramarReservaRequest(@NotNull(message = "Elige la franja a la que quieres cambiar") UUID franjaCodigo) {
}
