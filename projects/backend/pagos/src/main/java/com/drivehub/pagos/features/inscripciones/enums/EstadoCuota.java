package com.drivehub.pagos.features.inscripciones.enums;

/**
 * Estado de una cuota (CU28 y la contratación del plan). Un job marca VENCIDA por (estado, fecha_vence).
 */
public enum EstadoCuota {
    PENDIENTE,
    PAGADA,
    VENCIDA,
    ANULADA
}
