package com.drivehub.escuela.presentation.controller;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;

/**
 * Un auto con su instructor vigente (CU10): también cada elemento del listado. `instructor` y
 * `fechaInicio` son nulos si el auto no tiene instructor.
 */
public record AsignacionVehiculoResponse(UUID vehiculoCodigo, String placa, String marca, String modelo,
        EstadoVehiculo estadoVehiculo, InstructorAsignadoResponse instructor, LocalDate fechaInicio) {
}
