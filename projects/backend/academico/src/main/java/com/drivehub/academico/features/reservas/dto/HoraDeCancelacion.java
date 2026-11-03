package com.drivehub.academico.features.reservas.dto;

/**
 * Qué pasó con la hora del estudiante al cancelar una clase (CU15).
 *
 * * `DEVUELTA`: la hora volvió a su billetera.
 * * `PERDIDA`: cancelación tardía (menos del plazo): la franja se liberó pero la hora no se devuelve.
 * * `EN_PROCESO`: Pagos no respondió; la cancelación se aceptó y el sistema devuelve la hora solo cuando vuelva.
 */
public enum HoraDeCancelacion {
    DEVUELTA,
    PERDIDA,
    EN_PROCESO
}
