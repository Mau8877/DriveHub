package com.drivehub.escuela.domain.repository;

import com.drivehub.escuela.domain.entity.EstadoVehiculo;

/**
 * Filtros opcionales del listado de autos (CU07).
 *
 * * `busqueda` se aplica a placa, marca y modelo sin distinguir mayúsculas; vacía o nula no
 * * filtra. `estado` nulo trae todos los estados.
 */
public record FiltroVehiculos(String busqueda, EstadoVehiculo estado) {
}
