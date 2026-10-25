package com.drivehub.pagos.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de los eventos pendientes de publicar, sin lógica. Quien escribe un evento lo guarda en la MISMA
 * transacción que el cambio que lo origina; el proceso que los publica a RabbitMQ todavía no existe (no hay
 * consumidores).
 */
public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, Long> {
}
