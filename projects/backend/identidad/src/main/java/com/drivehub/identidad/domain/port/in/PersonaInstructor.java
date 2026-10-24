package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * Lo que Escuela ve de la persona de un instructor (CU09): datos de la cuenta y del perfil.
 *
 * * No lleva `passwordHash`, roles ni escuela: la ficha profesional es de Escuela.
 * * `fotoUrl` es la URL prefirmada de lectura (null si no tiene foto o no se pudo firmar).
 */
public record PersonaInstructor(
        UUID codigo,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String fotoUrl,
        int edad,
        LocalDate fechaNacimiento,
        SexoPersona sexo,
        String ci,
        String email,
        String telefono,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        LocalDate fechaAlta
) {
}
