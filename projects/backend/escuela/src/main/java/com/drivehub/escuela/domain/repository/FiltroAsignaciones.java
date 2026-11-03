package com.drivehub.escuela.domain.repository;

import java.util.Collection;
import java.util.UUID;

/**
 * Filtros opcionales del listado de autos con su instructor (CU10).
 *
 * * `busqueda` se aplica a placa, marca y modelo sin distinguir mayúsculas; vacía o nula no filtra.
 * * `instructorUsuarioCodigos` son las personas cuyo nombre coincide con la búsqueda (ya resueltas por
 * * el caso de uso con Identidad): suman los autos de esos instructores a los que coinciden por
 * * placa, marca o modelo. Nulo o vacío no suma nada.
 * * `asignacion` nulo trae los autos con y sin instructor.
 */
public record FiltroAsignaciones(String busqueda, Collection<UUID> instructorUsuarioCodigos, Asignacion asignacion) {

    public enum Asignacion {
        ASIGNADO,
        SIN_ASIGNAR
    }
}
