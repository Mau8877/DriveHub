package com.drivehub.identidad.domain.model;

import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Rol del sistema (CU05). Los nombres conocidos (ver {@link RolNombres})
 * son datos de la tabla rol, no un enum cerrado.
 */
@Getter
@Builder
public class Rol {

    private final UUID codigo;
    private final String nombre;
    private final String descripcion;
    private final Set<Permiso> permisos;
}
