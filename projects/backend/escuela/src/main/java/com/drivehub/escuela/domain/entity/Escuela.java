package com.drivehub.escuela.domain.entity;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Escuela (autoescuela cliente de la plataforma, el tenant).
 *
 * * `slug` es el identificador legible del enlace de registro. El `codigo`
 * * UUID es el que guardan los demás servicios como `escuela_codigo`.
 */
@Getter
@Builder
public class Escuela {

    private final UUID codigo;
    private final String slug;
    private final String nombre;
    // * Nulo mientras la escuela está activa; con fecha, está desactivada (borrado lógico)
    private final Instant eliminadaEn;
    private final Instant creadoEn;
    private final Instant actualizadoEn;

    public boolean activa() {
        return eliminadaEn == null;
    }

    public EstadoEscuela estado() {
        return activa() ? EstadoEscuela.ACTIVA : EstadoEscuela.INACTIVA;
    }
}
