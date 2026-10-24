package com.drivehub.escuela.presentation.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/instructores (alta por el Administrador de escuela, CU09).
 *
 * * No tiene campo de rol, escuela ni dirección: el rol es siempre INSTRUCTOR, la escuela es la del
 * * Administrador (token) y la dirección la calcula Identidad desde la ubicación. Si llegan, se ignoran.
 * * Las reglas de la persona son las del alta de estudiantes (CU08), la web y mobile; las de la ficha
 * * (vencimiento futuro, contratación no futura, categorías) las aplica el caso de uso.
 * * `fechaContratacion` es opcional (sin ella, hoy). `categorias` son siglas.
 */
public record CrearInstructorRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
        String apellidos,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe estar en el pasado")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El sexo es obligatorio")
        @Pattern(regexp = "MASCULINO|FEMENINO", message = "El sexo debe ser MASCULINO o FEMENINO")
        String sexo,

        @NotBlank(message = "El CI es obligatorio")
        @Size(min = 5, max = 15, message = "El CI debe tener entre 5 y 15 caracteres")
        @Pattern(regexp = "[A-Za-z0-9-]+", message = "El CI solo admite letras, números y guion")
        String ci,

        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        String telefono,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 9, max = 64, message = "La contraseña debe tener entre 9 y 64 caracteres")
        String password,

        @NotNull(message = "La ubicación es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud no es válida")
        @DecimalMax(value = "90", message = "La latitud no es válida")
        BigDecimal latitud,

        @NotNull(message = "La ubicación es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud no es válida")
        @DecimalMax(value = "180", message = "La longitud no es válida")
        BigDecimal longitud,

        @NotBlank(message = "El número de licencia es obligatorio")
        @Size(min = 3, max = 30, message = "El número de licencia debe tener entre 3 y 30 caracteres")
        String nroLicencia,

        @NotNull(message = "La fecha de vencimiento de la licencia es obligatoria")
        LocalDate licenciaVence,

        LocalDate fechaContratacion,

        @NotEmpty(message = "Elige al menos una categoría")
        List<@NotBlank(message = "La categoría no puede estar vacía")
            @Size(max = 3, message = "La categoría no puede superar 3 caracteres") String> categorias
) {
}
