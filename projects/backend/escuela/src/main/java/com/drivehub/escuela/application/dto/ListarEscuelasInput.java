package com.drivehub.escuela.application.dto;

import com.drivehub.escuela.domain.entity.EstadoEscuela;

/**
 * Parámetros del listado de escuelas (CU05). Todos son opcionales.
 *
 * * Valores por defecto: página 1 y 20 por página (máximo 100). `busqueda` vacía no filtra y
 * * `estado` nulo trae activas e inactivas.
 */
public record ListarEscuelasInput(Integer pagina, Integer tamano, String busqueda, EstadoEscuela estado) {
}
