package com.drivehub.escuela.domain.entity;

/**
 * Si un instructor puede dar clase en un día y un momento del día (CU11).
 *
 * * `NO_DISPONIBLE`: no está `ACTIVO` (licencia o inactivo), tenga o no franjas.
 * * `DISPONIBLE`: está `ACTIVO` y tiene al menos una franja libre.
 * * `COMPLETO`: está `ACTIVO` y tiene franjas, pero ninguna libre.
 * * `SIN_HORARIO`: está `ACTIVO` y no tiene ninguna franja.
 * * Se deriva, nunca se guarda.
 */
public enum Disponibilidad {
    DISPONIBLE,
    COMPLETO,
    SIN_HORARIO,
    NO_DISPONIBLE;

    /**
     * @param totalFranjas todas las franjas del instructor en ese día y momento, libres o no
     * @param libres las que están libres y todavía no empezaron
     */
    public static Disponibilidad de(EstadoInstructor estado, int totalFranjas, int libres) {
        if (estado != EstadoInstructor.ACTIVO) {
            return NO_DISPONIBLE;
        }
        if (libres > 0) {
            return DISPONIBLE;
        }
        return totalFranjas > 0 ? COMPLETO : SIN_HORARIO;
    }

    /** Posición para ordenar el listado: los disponibles primero y los no disponibles al final. */
    public int orden() {
        return ordinal();
    }
}
