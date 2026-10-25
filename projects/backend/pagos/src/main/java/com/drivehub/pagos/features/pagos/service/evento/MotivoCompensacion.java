package com.drivehub.pagos.features.pagos.service.evento;

/**
 * Por qué hay que devolver un cobro completo (CU27).
 *
 * * `MONTO_DISTINTO`: la pasarela cobró un monto o una moneda distintos de los del pago.
 * * `ACREDITACION_FALLIDA`: se cobró pero no se pudieron acreditar las horas.
 * * `COBRO_TARDIO`: se cobró un pago que ya había vencido (o que ya había fallado).
 */
public enum MotivoCompensacion {
    MONTO_DISTINTO,
    ACREDITACION_FALLIDA,
    COBRO_TARDIO
}
