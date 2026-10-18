package com.drivehub.pagos.common.outbox;

import java.time.Instant;

import com.drivehub.pagos.common.model.BaseModel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento pendiente de publicar a RabbitMQ (CU27, CU44).
 * * Se escribe en la MISMA transacción que el cambio de estado del pago; un proceso
 * * aparte lo publica y marca publicado_en. Si el broker cae, el evento no se pierde.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "outbox_evento")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_outbox_evento"))
public class OutboxEvento extends BaseModel {

    // * Ej: pago.confirmado, pago.revertido
    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    // * NULL hasta que RabbitMQ confirma
    @Setter
    @Column(name = "publicado_en")
    private Instant publicadoEn;
}
