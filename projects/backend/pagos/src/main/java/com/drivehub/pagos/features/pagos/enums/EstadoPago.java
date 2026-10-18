package com.drivehub.pagos.features.pagos.enums;

/**
 * Estado del pago (CU26, CU27, CU31). REVERTIDO = compensado por la saga.
 */
public enum EstadoPago {
    PENDIENTE,
    CONFIRMADO,
    FALLIDO,
    REVERTIDO
}
