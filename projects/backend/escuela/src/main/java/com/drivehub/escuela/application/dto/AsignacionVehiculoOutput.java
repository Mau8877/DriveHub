package com.drivehub.escuela.application.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;

/**
 * Un auto con su instructor vigente (CU10). `instructor` y `fechaInicio` son nulos si el auto no
 * tiene instructor.
 */
public record AsignacionVehiculoOutput(UUID vehiculoCodigo, String placa, String marca, String modelo,
        EstadoVehiculo estadoVehiculo, InstructorAsignadoOutput instructor, LocalDate fechaInicio) {
}
