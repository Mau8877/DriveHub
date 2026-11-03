package com.drivehub.escuela.domain.repository;

import java.util.UUID;

import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.entity.Vehiculo;

/**
 * Un renglón del listado de CU10: un auto y su asignación vigente (nula si no tiene instructor).
 *
 * * `instructorUsuarioCodigo` es el codigo_usuario de Identidad del instructor asignado, para pedirle
 * * su nombre; nulo si el auto no tiene instructor.
 */
public record AsignacionDeAuto(Vehiculo vehiculo, AsignacionVehiculo vigente, UUID instructorUsuarioCodigo) {
}
