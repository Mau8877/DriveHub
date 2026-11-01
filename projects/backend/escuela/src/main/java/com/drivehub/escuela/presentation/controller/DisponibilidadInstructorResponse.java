package com.drivehub.escuela.presentation.controller;

import java.util.List;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.Disponibilidad;
import com.drivehub.escuela.domain.entity.EstadoAutos;
import com.drivehub.escuela.domain.entity.EstadoInstructor;

/**
 * Un instructor con su disponibilidad en un día y un momento del día (CU11). Sin `usuarioCodigo` ni ids internos.
 * `nombreCompleto` es nulo si Identidad no devolvió a la persona.
 */
public record DisponibilidadInstructorResponse(UUID instructorCodigo, String nombreCompleto,
        EstadoInstructor estadoInstructor, Disponibilidad disponibilidad, EstadoAutos estadoAutos,
        List<AutoDeInstructorResponse> autos, int libres, int reservadas, int bloqueadas) {
}
