package com.drivehub.escuela.presentation.controller;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Una asignación del historial de un auto (CU10). `fechaFin` nula = vigente.
 */
public record HistorialAsignacionResponse(UUID codigo, InstructorAsignadoResponse instructor, LocalDate fechaInicio,
        LocalDate fechaFin) {
}
