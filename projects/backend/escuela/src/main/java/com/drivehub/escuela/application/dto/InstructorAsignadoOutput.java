package com.drivehub.escuela.application.dto;

import java.util.UUID;

/**
 * El instructor de una asignación (CU10). `nombreCompleto` es nulo en las respuestas de las acciones
 * (no se le pide a Identidad) y cuando Identidad no devuelve a la persona.
 */
public record InstructorAsignadoOutput(UUID codigo, String nombreCompleto) {
}
