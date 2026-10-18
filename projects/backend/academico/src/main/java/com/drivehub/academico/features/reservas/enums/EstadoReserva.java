package com.drivehub.academico.features.reservas.enums;

/**
 * Estado de una reserva (CU14, CU15). PENDIENTE_PAGO vence en expiraPagoEn: la saga la cancela y libera la franja.
 */
public enum EstadoReserva {
    PENDIENTE_PAGO,
    CONFIRMADA,
    CANCELADA,
    COMPLETADA,
    NO_ASISTIO
}
