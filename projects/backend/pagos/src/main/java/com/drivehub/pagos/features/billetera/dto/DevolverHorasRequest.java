package com.drivehub.pagos.features.billetera.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Devolver a la billetera las horas que gastó una clase (CU27).
 */
public record DevolverHorasRequest(
        @NotNull(message = "Indica el estudiante") UUID estudianteCodigo,
        @NotNull(message = "Indica la clase") UUID claseCodigo) {
}
