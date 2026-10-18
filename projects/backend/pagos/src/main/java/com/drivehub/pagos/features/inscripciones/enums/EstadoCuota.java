package com.drivehub.pagos.features.inscripciones.enums;

/**
 * Estado de una cuota (CU25, CU28). Un job marca VENCIDA por (estado, fecha_vence).
 */
public enum EstadoCuota {
    PENDIENTE,
    PAGADA,
    VENCIDA,
    ANULADA
}
