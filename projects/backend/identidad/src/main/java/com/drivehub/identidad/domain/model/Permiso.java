package com.drivehub.identidad.domain.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Permiso fino (CU05).
 *
 * * `clave` es el identificador legible del permiso (ej. "reserva.crear",
 * * "pago.revertir"); en el diseño original se llamaba "codigo", se renombró
 * * para no chocar con el `codigo` UUID público que tiene toda entidad.
 */
@Getter
@Builder
public class Permiso {

    private final UUID codigo;
    private final String clave;
    private final String descripcion;
}
