package com.drivehub.pagos.features.pagos.service.evento;

import java.util.UUID;

/**
 * La pasarela no pudo devolver el dinero de un reembolso pedido por el estudiante y sus horas le volvieron (CU31).
 * Se publica DENTRO de la transacción que lo resuelve y se escucha después del commit (el aviso al estudiante).
 * Es interno de Pagos, no pasa por RabbitMQ.
 */
public record ReembolsoFallido(UUID pagoCodigo, UUID reversionCodigo) {
}
