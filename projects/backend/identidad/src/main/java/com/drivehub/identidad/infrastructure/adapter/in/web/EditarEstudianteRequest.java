package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PATCH /api/v1/estudiantes/{codigo} (CU08, F3): solo la categoría y la ubicación.
 *
 * * Los tres campos son opcionales, pero debe llegar al menos uno y las coordenadas van juntas (lo
 * * comprueba el caso de uso). Cualquier otro nombre del cuerpo (`ci`, `nombres`, `email`...) se
 * * ignora: esos datos son del propio estudiante (CU06).
 */
public record EditarEstudianteRequest(
        @Size(max = 3, message = "La categoría no puede superar 3 caracteres")
        String categoriaSigla,

        @DecimalMin(value = "-90", message = "La latitud no es válida")
        @DecimalMax(value = "90", message = "La latitud no es válida")
        BigDecimal latitud,

        @DecimalMin(value = "-180", message = "La longitud no es válida")
        @DecimalMax(value = "180", message = "La longitud no es válida")
        BigDecimal longitud
) {
}
