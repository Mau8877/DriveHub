package com.drivehub.academico.features.reservas.service;

/**
 * Cómo terminó el orquestador de una reserva a medias (CU44).
 *
 * * `CONFIRMADA` y `CANCELADA`: la reserva quedó resuelta.
 * * `PENDIENTE`: no se pudo resolver por una causa PROPIA (cuenta como intento fallido).
 * * `PAGOS_NO_RESPONDE`: Pagos no respondió o rechazó la credencial (NO cuenta como intento: se reintenta luego).
 * * `EN_REVISION`: se agotaron los intentos y una persona tiene que mirarla.
 */
public enum ResultadoOrquestacion {
    CONFIRMADA,
    CANCELADA,
    PENDIENTE,
    PAGOS_NO_RESPONDE,
    EN_REVISION
}
