package com.drivehub.pagos.features.pagos.pasarela;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que Pagos le pide a la pasarela para cobrar una compra.
 *
 * @param idempotencyKey la clave del pago: repetir la solicitud no crea dos sesiones de cobro
 * @param montoCentavosUsd el monto en centavos de dólar (la pasarela no cobra en bolivianos)
 */
public record SolicitudCobro(
        UUID pagoCodigo,
        String idempotencyKey,
        String nombreProducto,
        long montoCentavosUsd,
        Instant expiraEn,
        String urlExito,
        String urlCancelacion) {
}
