package com.drivehub.identidad.domain.port.out;

import com.drivehub.identidad.domain.model.EstadoFiltroEstudiante;

/**
 * Filtros del listado de estudiantes de una escuela (CU08). Ambos pueden ser `null` (sin filtrar).
 *
 * @param busqueda texto ya recortado: cada palabra debe aparecer en nombres + apellidos
 */
public record FiltroEstudiantes(String busqueda, EstadoFiltroEstudiante estado) {
}
