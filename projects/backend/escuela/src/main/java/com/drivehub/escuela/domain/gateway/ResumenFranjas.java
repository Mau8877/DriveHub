package com.drivehub.escuela.domain.gateway;

/**
 * Cuántas franjas tiene un instructor en un día y un momento del día (CU11), según Académico.
 *
 * * `total` son todas sus franjas; `libres`, las `LIBRE` que todavía no empezaron. Lo que no es libre, reservada
 * * ni bloqueada son libres que ya pasaron.
 */
public record ResumenFranjas(int total, int libres, int reservadas, int bloqueadas) {

    /** Un instructor sin ninguna franja: Académico no lo devuelve. */
    public static final ResumenFranjas VACIO = new ResumenFranjas(0, 0, 0, 0);
}
