package com.drivehub.escuela.presentation.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.drivehub.escuela.domain.entity.EstadoInstructor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PATCH /api/v1/instructores/{codigo} (CU09). Todos los campos son opcionales: los que no
 * llegan quedan como están.
 *
 * * Solo la ficha profesional y la ubicación. Cualquier otro campo (nombres, CI, correo, escuela...)
 * * se ignora a propósito: los datos personales se corrigen en el perfil de la persona (CU06).
 * * `latitud` y `longitud` van juntas (lo comprueba el caso de uso).
 */
public record EditarInstructorRequest(
        @Size(min = 3, max = 30, message = "El número de licencia debe tener entre 3 y 30 caracteres")
        String nroLicencia,

        LocalDate licenciaVence,

        LocalDate fechaContratacion,

        List<@NotBlank(message = "La categoría no puede estar vacía")
            @Size(max = 3, message = "La categoría no puede superar 3 caracteres") String> categorias,

        EstadoInstructor estado,

        @DecimalMin(value = "-90", message = "La latitud no es válida")
        @DecimalMax(value = "90", message = "La latitud no es válida")
        BigDecimal latitud,

        @DecimalMin(value = "-180", message = "La longitud no es válida")
        @DecimalMax(value = "180", message = "La longitud no es válida")
        BigDecimal longitud
) {
}
