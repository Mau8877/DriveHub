package com.drivehub.escuela.application.dto;

import java.util.List;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.Disponibilidad;
import com.drivehub.escuela.domain.entity.EstadoAutos;
import com.drivehub.escuela.domain.entity.EstadoInstructor;

/**
 * Un instructor con su disponibilidad en un día y un momento del día (CU11).
 *
 * * `instructorCodigo` es el `codigo` de la ficha, no el `usuarioCodigo`. `nombreCompleto` es nulo si Identidad
 * * no devolvió a la persona.
 * * `libres` son las franjas libres que todavía no empezaron; `reservadas` y `bloqueadas`, las de ese estado.
 */
public record DisponibilidadInstructorOutput(UUID instructorCodigo, String nombreCompleto,
        EstadoInstructor estadoInstructor, Disponibilidad disponibilidad, EstadoAutos estadoAutos,
        List<AutoDeInstructorOutput> autos, int libres, int reservadas, int bloqueadas) {
}
