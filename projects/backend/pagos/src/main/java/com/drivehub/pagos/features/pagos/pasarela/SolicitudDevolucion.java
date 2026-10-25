package com.drivehub.pagos.features.pagos.pasarela;

import java.util.UUID;

/**
 * Lo que Pagos le pide a la pasarela para devolver un cobro (CU31).
 *
 * @param cobroReferencia el id del cobro en la pasarela (el que guardó CU27)
 * @param montoCentavosUsd lo que se devuelve, en centavos de dólar (puede ser menos de lo cobrado)
 * @param idempotencyKey el código de la reversión: repetir la llamada con la misma clave no devuelve dos veces
 */
public record SolicitudDevolucion(
        String cobroReferencia,
        long montoCentavosUsd,
        String idempotencyKey,
        UUID pagoCodigo,
        UUID reversionCodigo) {
}
