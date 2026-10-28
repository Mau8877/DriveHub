package com.drivehub.academico.features.simulacros.repository;

/**
 * Filtros opcionales del listado de simulacros (CU20). Todos nulos trae todos los de la escuela.
 *
 * * `busqueda` se aplica al nombre sin distinguir mayúsculas y sin interpretar comodines.
 */
public record FiltroSimulacros(String busqueda, String categoria, Boolean activo) {
}
