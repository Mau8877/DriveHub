package com.drivehub.identidad.domain.port.out;

import java.util.UUID;

/**
 * Datos de una escuela que Identidad necesita (la referencia es del servicio Escuela).
 *
 * * `activa` es false cuando la escuela está inactiva (desactivada por el Super admin): sus cuentas no pueden
 * * iniciar ni renovar sesión (CU02). Al buscar por slug (registro) Escuela
 * * solo devuelve escuelas activas, por eso el constructor corto la da por activa.
 */
public record EscuelaRef(UUID codigo, String slug, String nombre, boolean activa) {

    public EscuelaRef(UUID codigo, String slug, String nombre) {
        this(codigo, slug, nombre, true);
    }
}
