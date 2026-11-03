package com.drivehub.escuela.domain.repository;

import java.util.UUID;

import com.drivehub.escuela.domain.entity.AsignacionVehiculo;

/**
 * Una asignación del historial de un auto (CU10), vigente o cerrada, con el codigo_usuario de
 * Identidad de su instructor para pedirle el nombre.
 */
public record AsignacionHistorial(AsignacionVehiculo asignacion, UUID instructorUsuarioCodigo) {
}
