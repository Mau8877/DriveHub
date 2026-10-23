package com.drivehub.escuela.presentation.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/escuelas (CU05): la escuela y su Administrador.
 *
 * * El slug solo admite minúsculas, números y guiones. Las reglas del Administrador son las del
 * * registro del estudiante (la edad mínima la valida Identidad, que es quien la configura).
 * * CU08a: el Administrador lleva además CI, sexo y la ubicación del mapa; Identidad calcula la dirección.
 */
public record CrearEscuelaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombre,

        @NotBlank(message = "El slug es obligatorio")
        @Size(min = 3, max = 60, message = "El slug debe tener entre 3 y 60 caracteres")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "El slug solo admite minúsculas, números y guiones, sin guion al inicio ni al final")
        String slug,

        @NotNull(message = "El administrador es obligatorio")
        @Valid
        AdministradorRequest administrador
) {

    public record AdministradorRequest(
            @NotBlank(message = "Los nombres son obligatorios")
            @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
            String nombres,

            @NotBlank(message = "Los apellidos son obligatorios")
            @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
            String apellidos,

            @NotNull(message = "La fecha de nacimiento es obligatoria")
            @Past(message = "La fecha de nacimiento debe estar en el pasado")
            LocalDate fechaNacimiento,

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            @Size(max = 120, message = "El correo no puede superar 120 caracteres")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 9, max = 64, message = "La contraseña debe tener entre 9 y 64 caracteres")
            String password,

            @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
            String telefono,

            @NotBlank(message = "El CI es obligatorio")
            @Size(min = 5, max = 15, message = "El CI debe tener entre 5 y 15 caracteres")
            @Pattern(regexp = "[A-Za-z0-9-]+", message = "El CI solo admite letras, números y guion")
            String ci,

            @NotBlank(message = "El sexo es obligatorio")
            @Pattern(regexp = "MASCULINO|FEMENINO", message = "El sexo debe ser MASCULINO o FEMENINO")
            String sexo,

            @NotNull(message = "La ubicación es obligatoria")
            @DecimalMin(value = "-90", message = "La latitud no es válida")
            @DecimalMax(value = "90", message = "La latitud no es válida")
            BigDecimal latitud,

            @NotNull(message = "La ubicación es obligatoria")
            @DecimalMin(value = "-180", message = "La longitud no es válida")
            @DecimalMax(value = "180", message = "La longitud no es válida")
            BigDecimal longitud
    ) {

        @Override
        public String toString() {
            return "AdministradorRequest[email=" + email + ", password=***]";
        }
    }
}
