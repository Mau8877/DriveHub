package com.drivehub.identidad.domain.port.in;

import java.util.UUID;

/**
 * Parámetros de la búsqueda de instructores por nombre (CU09). Todos menos el administrador son
 * opcionales.
 *
 * * Valores por defecto: página 1 y 20 por página (máximo 100). `busqueda` en blanco no filtra.
 * * `administradorCodigo` sale de la sesión: de él se toma la escuela, nunca del cliente.
 */
public record BuscarInstructoresComando(UUID administradorCodigo, Integer pagina, Integer tamano, String busqueda) {
}
