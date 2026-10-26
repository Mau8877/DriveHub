package com.drivehub.escuela.presentation.controller;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Asignar un auto a un instructor, o cambiarlo a otro (CU10). `fecha` es opcional: hoy por defecto.
 */
public record AsignacionInstructorRequest(
        @NotNull(message = "El instructor es obligatorio") UUID instructorCodigo,
        LocalDate fecha) {
}
