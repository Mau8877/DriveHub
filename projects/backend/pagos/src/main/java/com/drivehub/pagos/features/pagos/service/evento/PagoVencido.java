package com.drivehub.pagos.features.pagos.service.evento;

import java.util.UUID;

/**
 * Un pago pendiente venció sin que el estudiante lo completara (CU26). Se publica DENTRO de la transacción que lo
 * pasa a `FALLIDO` y se escucha después del commit (el aviso al estudiante). Es interno de Pagos, no pasa por
 * RabbitMQ.
 */
public record PagoVencido(UUID pagoCodigo) {
}
