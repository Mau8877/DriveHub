package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.EstadoEstudiante;
import com.drivehub.identidad.domain.model.SexoPersona;

/**
 * La ficha de un estudiante para su escuela (CU08).
 *
 * * No lleva el hash de la contraseña, los roles ni la escuela: la escuela es la de quien consulta.
 */
public record EstudianteDetalle(
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
        LocalDate fechaIncorporacion) {
}
