package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

import com.drivehub.identidad.domain.model.EstadoFiltroEstudiante;

/**
 * Parámetros del listado de estudiantes (CU08). Todos menos el administrador son opcionales.
 *
 * * Valores por defecto: página 1 y 20 por página (máximo 100). `busqueda` en blanco no filtra y
 * * `estado` nulo trae activos e inactivos.
 * * `administradorCodigo` sale de la sesión: de él se toma la escuela, nunca del cliente.
 */
public record ListarEstudiantesComando(
        UUID administradorCodigo, Integer pagina, Integer tamano, String busqueda, EstadoFiltroEstudiante estado) {
}
