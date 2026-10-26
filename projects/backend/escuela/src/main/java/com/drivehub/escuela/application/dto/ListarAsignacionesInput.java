package com.drivehub.escuela.application.dto;

import java.util.UUID;

import com.drivehub.escuela.domain.repository.FiltroAsignaciones;

/**
 * Parámetros del listado de autos con su instructor (CU10). Todos menos la escuela son opcionales.
 *
 * * Valores por defecto: página 1 y 20 por página (máximo 100).
 */
public record ListarAsignacionesInput(UUID escuelaCodigo, Integer pagina, Integer tamano, String busqueda,
        FiltroAsignaciones.Asignacion asignacion) {
}
