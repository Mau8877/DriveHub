package com.drivehub.escuela.presentation.controller;

import java.time.LocalDate;

import com.drivehub.escuela.domain.entity.Transmision;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/vehiculos (CU07).
 *
 * * La escuela NO viaja en el cuerpo: sale del token. El rango del año depende de la fecha de hoy
 * * y lo valida el caso de uso.
 */
public record CrearVehiculoRequest(
        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "^[A-Za-z0-9-]{5,10}$",
            message = "La placa debe tener entre 5 y 10 caracteres: letras, números y guiones")
        String placa,

        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 40, message = "La marca no puede superar 40 caracteres")
        String marca,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 40, message = "El modelo no puede superar 40 caracteres")
        String modelo,

        Short anio,

        @Size(max = 20, message = "El color no puede superar 20 caracteres")
        String color,

        @NotNull(message = "La transmisión es obligatoria")
        Transmision transmision,

        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Max(value = 9_999_999, message = "El kilometraje es demasiado alto")
        Integer kilometraje,

        LocalDate soatVence,

        LocalDate inspeccionVence
) {
}
