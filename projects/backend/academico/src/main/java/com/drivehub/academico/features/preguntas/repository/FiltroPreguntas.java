package com.drivehub.academico.features.preguntas.repository;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;

/**
 * Filtros opcionales del listado de preguntas (CU19). Todos nulos trae todo el banco de la escuela.
 *
 * * `busqueda` se aplica al enunciado y al tema sin distinguir mayúsculas y sin interpretar
 * * comodines: la persona busca texto, no escribe patrones.
 */
public record FiltroPreguntas(String busqueda, String categoria, String tema, TipoPregunta tipo,
        Integer dificultad, Boolean activa) {
}
