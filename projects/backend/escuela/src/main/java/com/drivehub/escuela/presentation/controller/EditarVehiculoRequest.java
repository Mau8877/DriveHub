package com.drivehub.escuela.presentation.controller;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PATCH /api/v1/vehiculos/{codigo} (CU07). Todo es opcional: lo que no se envía no se
 * toca. La placa, la transmisión y el estado no se editan.
 */
public record EditarVehiculoRequest(
        @Size(max = 40, message = "La marca no puede superar 40 caracteres")
        String marca,

        @Size(max = 40, message = "El modelo no puede superar 40 caracteres")
        String modelo,

        Short anio,

        @Size(max = 20, message = "El color no puede superar 20 caracteres")
        String color,

        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Max(value = 9_999_999, message = "El kilometraje es demasiado alto")
        Integer kilometraje,

        LocalDate soatVence,

        LocalDate inspeccionVence
) {
}
