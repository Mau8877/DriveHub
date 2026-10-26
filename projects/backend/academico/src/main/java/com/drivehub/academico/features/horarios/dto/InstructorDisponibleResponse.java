package com.drivehub.academico.features.horarios.dto;

import java.util.UUID;

/**
 * Un instructor de la escuela con al menos una franja libre (CU14), para el filtro «Instructor» de la app.
 */
public record InstructorDisponibleResponse(UUID codigo, String nombre) {
}
