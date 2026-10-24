package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de PATCH /api/v1/usuarios/instructores/{codigo}/ubicacion (CU09). La dirección la calcula
 * el servidor; si llega, se ignora.
 */
public record EditarUbicacionRequest(
        @NotNull(message = "La ubicación es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud no es válida")
        @DecimalMax(value = "90", message = "La latitud no es válida")
        BigDecimal latitud,

        @NotNull(message = "La ubicación es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud no es válida")
        @DecimalMax(value = "180", message = "La longitud no es válida")
        BigDecimal longitud
) {
}
