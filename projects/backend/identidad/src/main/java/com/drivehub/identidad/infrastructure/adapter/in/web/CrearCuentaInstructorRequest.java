package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/usuarios/instructores (alta de la cuenta de un instructor, CU09).
 *
 * * No tiene campo de rol, escuela ni dirección: el rol es siempre INSTRUCTOR, la escuela es la del
 * * Administrador y la dirección la calcula el servidor desde la ubicación. Si llegan, se ignoran.
 * * `codigo` es el de la persona y lo elige Escuela (así puede revertir la cuenta si no recibe la respuesta).
 * * Las reglas deben coincidir con las de `CrearEstudianteRequest`, la web y mobile.
 */
public record CrearCuentaInstructorRequest(
        @NotNull(message = "El código es obligatorio")
        UUID codigo,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
        String apellidos,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe estar en el pasado")
        LocalDate fechaNacimiento,

        @NotNull(message = "El sexo es obligatorio")
        SexoPersona sexo,

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
        BigDecimal longitud
) {
}
