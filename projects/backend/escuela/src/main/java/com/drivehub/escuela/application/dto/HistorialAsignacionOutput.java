package com.drivehub.escuela.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Una asignación del historial de un auto (CU10). `fechaFin` nula = vigente.
 */
public record HistorialAsignacionOutput(UUID codigo, InstructorAsignadoOutput instructor, LocalDate fechaInicio,
        LocalDate fechaFin) {
}
