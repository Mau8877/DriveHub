package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;

/**
 * La persona de un instructor (CU09). Sin id interno, hash de la contraseña, roles ni escuela.
 * `fechaNacimiento` y `fechaAlta` viajan como `yyyy-MM-dd`.
 */
public record PersonaInstructorResponse(
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

    public static PersonaInstructorResponse de(PersonaInstructor p) {
        return new PersonaInstructorResponse(p.codigo(), p.nombres(), p.apellidos(), p.nombreCompleto(), p.fotoUrl(),
            p.edad(), p.fechaNacimiento(), p.sexo(), p.ci(), p.email(), p.telefono(), p.direccion(), p.latitud(),
            p.longitud(), p.fechaAlta());
    }
}
