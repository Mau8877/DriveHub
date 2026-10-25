package com.drivehub.pagos.features.pagos.service.evento;

import java.util.UUID;

/**
 * La pasarela devolvió el dinero y el pago quedó reembolsado (CU31). Se publica DENTRO de la transacción que lo
 * resuelve y se escucha después del commit (CU30: el comprobante de reembolso). Es un evento interno de Pagos, no
 * pasa por RabbitMQ.
 */
public record ReembolsoEjecutado(UUID pagoCodigo, UUID reversionCodigo) {
}
