package com.drivehub.pagos.features.planes.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un plan de horas (CU25a). La escuela NO viaja acá: sale del token.
 *
 * * Las reglas de cada campo son las de la base (`CHECK`) y las de la pantalla; el nombre único por
 * * escuela lo comprueba el service.
 */
public record CrearPlanPagoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotNull(message = "Las horas son obligatorias")
        @Min(value = 1, message = "Las horas deben ser un entero de 1 a 500")
        @Max(value = 500, message = "Las horas deben ser un entero de 1 a 500")
        Integer horas,

        @NotNull(message = "El costo es obligatorio")
        @DecimalMin(value = "0.01", message = "El costo debe ser mayor a 0")
        @DecimalMax(value = "99999999.99", message = "El costo no puede superar 99999999,99")
        @Digits(integer = 8, fraction = 2, message = "El costo admite hasta 2 decimales")
        BigDecimal costoBs) {
}
