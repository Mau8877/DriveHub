package com.drivehub.escuela.presentation.controller;

import java.util.UUID;

/**
 * El instructor de una asignación (CU10). `nombreCompleto` es nulo en las respuestas de las acciones.
 */
public record InstructorAsignadoResponse(UUID codigo, String nombreCompleto) {
}
