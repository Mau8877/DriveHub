package com.drivehub.escuela.application.dto;

import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoInstructor;

/**
 * Parámetros del listado de instructores (CU09). Todos menos la escuela son opcionales.
 *
 * * Valores por defecto: página 1 y 20 por página (máximo 100). `busqueda` en blanco no filtra y
 * * `estado` nulo trae los tres estados.
 * * `escuelaCodigo` sale de la sesión (claim `escuela` del token): nunca del cliente.
 */
public record ListarInstructoresInput(
        UUID escuelaCodigo, Integer pagina, Integer tamano, String busqueda, EstadoInstructor estado) {
}
