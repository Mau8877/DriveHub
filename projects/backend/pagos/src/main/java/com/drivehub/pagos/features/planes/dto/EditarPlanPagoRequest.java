package com.drivehub.pagos.features.planes.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Datos para corregir un plan (CU25a, PATCH): cada campo es opcional y lo que no se envía no se toca. Las
 * reglas de cada campo, cuando viene, son las mismas que al crear. El estado no se cambia por acá.
 */
public record EditarPlanPagoRequest(
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @Min(value = 1, message = "Las horas deben ser un entero de 1 a 500")
        @Max(value = 500, message = "Las horas deben ser un entero de 1 a 500")
        Integer horas,

        @DecimalMin(value = "0.01", message = "El costo debe ser mayor a 0")
        @DecimalMax(value = "99999999.99", message = "El costo no puede superar 99999999,99")
        @Digits(integer = 8, fraction = 2, message = "El costo admite hasta 2 decimales")
        BigDecimal costoBs) {
}
