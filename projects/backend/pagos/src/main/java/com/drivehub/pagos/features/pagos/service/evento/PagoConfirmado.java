package com.drivehub.pagos.features.pagos.service.evento;

import java.util.UUID;

/**
 * El pago quedó confirmado y sus horas acreditadas (CU27). Se publica DENTRO de la transacción que lo confirma y
 * se escucha después del commit (CU30: el comprobante). Es un evento interno de Pagos, no pasa por RabbitMQ.
 */
public record PagoConfirmado(UUID pagoCodigo) {
}
