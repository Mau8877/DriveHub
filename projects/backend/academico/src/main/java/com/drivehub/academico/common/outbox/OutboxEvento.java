package com.drivehub.academico.common.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento pendiente de publicar a RabbitMQ (mismo patrón que Pagos).
 * * Se escribe en la misma transacción que el cambio (requiere el replica set); un proceso
 * * aparte lo publica y marca publicadoEn.
 */
@Getter
@NoArgsConstructor
@Document(collection = "outbox_eventos")
@CompoundIndex(name = "idx_outbox_pendientes", def = "{'publicadoEn': 1, 'createdAt': 1}")
public class OutboxEvento extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoOutboxEvento;

    // * Ej: clase.reservada, examen.calificado
    private String tipo;

    private Map<String,Object> payload;

    // * null hasta que RabbitMQ confirma
    @Setter
    private Instant publicadoEn;

    @Override
    public void inicializarCodigo() {
        if (codigoOutboxEvento == null) {
            codigoOutboxEvento = UuidV7.generar();
        }
    }
}
