package com.drivehub.escuela.application.dto;

import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;

/**
 * Parámetros del listado de autos de una escuela (CU07). Todos menos la escuela son opcionales.
 *
 * * Valores por defecto: página 1 y 20 por página (máximo 100).
 */
public record ListarVehiculosInput(UUID escuelaCodigo, Integer pagina, Integer tamano, String busqueda,
        EstadoVehiculo estado) {
}
