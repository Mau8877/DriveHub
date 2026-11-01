package com.drivehub.escuela.domain.entity;

import java.util.Collection;

/**
 * Cómo están los autos que un instructor usa hoy (CU11).
 *
 * * `DISPONIBLE`: al menos uno está `DISPONIBLE`.
 * * `NO_DISPONIBLE`: tiene autos, pero ninguno está disponible (mantenimiento o de baja).
 * * `SIN_AUTO`: no tiene ninguno asignado.
 * * Se deriva, nunca se guarda.
 */
public enum EstadoAutos {
    DISPONIBLE,
    NO_DISPONIBLE,
    SIN_AUTO;

    /** @param estados el estado de cada auto vigente del instructor */
    public static EstadoAutos de(Collection<EstadoVehiculo> estados) {
        if (estados == null || estados.isEmpty()) {
            return SIN_AUTO;
        }
        return estados.contains(EstadoVehiculo.DISPONIBLE) ? DISPONIBLE : NO_DISPONIBLE;
    }
}
