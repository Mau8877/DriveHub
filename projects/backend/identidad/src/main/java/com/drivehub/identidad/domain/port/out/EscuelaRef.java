package com.drivehub.identidad.domain.port.out;

import java.util.UUID;

/**
 * Datos de una escuela que Identidad necesita (la referencia es del servicio Escuela).
 */
public record EscuelaRef(UUID codigo, String slug, String nombre) {
}
