package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.EstadoEstudiante;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;

/**
 * La ficha de un estudiante (CU08). Sin id interno, hash de la contraseña, roles ni escuela.
 * `fechaNacimiento` y `fechaIncorporacion` viajan como `yyyy-MM-dd`.
 */
public record EstudianteDetalleResponse(
        UUID codigo,
        String nombres,
        String apellidos,
        String nombreCompleto,
        int edad,
        LocalDate fechaNacimiento,
        SexoPersona sexo,
        String ci,
        String email,
        String telefono,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        String categoriaSigla,
        EstadoEstudiante estado,
        LocalDate fechaIncorporacion
) {

    public static EstudianteDetalleResponse de(EstudianteDetalle e) {
        return new EstudianteDetalleResponse(e.codigo(), e.nombres(), e.apellidos(), e.nombreCompleto(), e.edad(),
            e.fechaNacimiento(), e.sexo(), e.ci(), e.email(), e.telefono(), e.direccion(), e.latitud(),
            e.longitud(), e.categoriaSigla(), e.estado(), e.fechaIncorporacion());
    }
}
