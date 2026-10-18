package com.drivehub.academico.common.mongo;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import com.drivehub.academico.common.model.BaseDocument;

import lombok.RequiredArgsConstructor;

/**
 * Asigna el `_id` Long y el codigo público (UUID v7) a todo documento nuevo
 * antes de guardarlo.
 *
 * * Corre DESPUÉS del callback de auditoría de Spring Data (orden 100): la
 * * auditoría decide si el documento es nuevo mirando si el id es null, así
 * * que si este callback asignara el id antes, createdAt nunca se llenaría.
 * * SecuenciaService se pide vía ObjectProvider (no inyección directa) para
 * * no formar un ciclo MongoTemplate -> callbacks -> SecuenciaService -> MongoTemplate.
 */
@Component
@RequiredArgsConstructor
public class AsignarIdentificadoresCallback implements BeforeConvertCallback<BaseDocument>, Ordered {

    private final ObjectProvider<SecuenciaService> secuenciaService;

    @Override
    public BaseDocument onBeforeConvert(BaseDocument documento, String coleccion) {
        if (documento.getId() == null) {
            documento.setId(secuenciaService.getObject().siguiente(coleccion));
        }
        documento.inicializarCodigo();
        return documento;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
