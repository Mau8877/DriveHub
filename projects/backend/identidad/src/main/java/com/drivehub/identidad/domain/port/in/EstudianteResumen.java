package com.drivehub.identidad.domain.port.in;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.identidad.domain.model.EstadoEstudiante;

/**
 * Una fila del listado de estudiantes de la escuela (CU08).
 *
 * * `edad` se calcula al momento de la consulta; no se guarda.
 * * `fechaIncorporacion` es la fecha de alta de la cuenta.
 * * `fotoUrl` es una URL prefirmada de lectura (vale 60 minutos), o null si no tiene foto.
 */
public record EstudianteResumen(
        UUID codigo,
        String nombreCompleto,
        String fotoUrl,
        int edad,
        String categoriaSigla,
        EstadoEstudiante estado,
        LocalDate fechaIncorporacion) {
}
