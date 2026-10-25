package com.drivehub.pagos.features.billetera.enums;

/**
 * Qué hizo un movimiento de la billetera de horas (CU27). Las horas del movimiento son siempre positivas: el
 * tipo dice si suman o restan.
 *
 * * `ACREDITACION`: suma, una compra confirmada.
 * * `CONSUMO`: resta, Académico gastó horas.
 * * `DEVOLUCION`: suma, horas que vuelven (una clase cancelada o un reembolso que falló).
 * * `REEMBOLSO`: resta, horas apartadas por un reembolso (CU31).
 */
public enum TipoMovimiento {
    ACREDITACION,
    CONSUMO,
    DEVOLUCION,
    REEMBOLSO
}
