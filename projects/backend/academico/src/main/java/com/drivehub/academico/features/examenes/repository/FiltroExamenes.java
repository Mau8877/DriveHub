package com.drivehub.academico.features.examenes.repository;

import java.util.UUID;

import com.drivehub.academico.features.examenes.enums.EstadoExamen;

/**
 * Filtros opcionales del listado de exámenes de conducción (CU22). Todos nulos trae todos los de la escuela.
 */
public record FiltroExamenes(String categoria, EstadoExamen estado, UUID estudianteCodigo) {
}
