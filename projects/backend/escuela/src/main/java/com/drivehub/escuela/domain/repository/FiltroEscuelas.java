package com.drivehub.escuela.domain.repository;

import com.drivehub.escuela.domain.entity.EstadoEscuela;

/**
 * Filtros opcionales del listado de escuelas (CU05).
 *
 * * `busqueda` se aplica al nombre y al slug sin distinguir mayúsculas; vacía o
 * * nula no filtra. `estado` nulo trae activas e inactivas.
 */
public record FiltroEscuelas(String busqueda, EstadoEscuela estado) {

    public static FiltroEscuelas sinFiltros() {
        return new FiltroEscuelas(null, null);
    }
}
