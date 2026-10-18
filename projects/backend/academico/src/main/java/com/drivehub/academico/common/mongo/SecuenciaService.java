package com.drivehub.academico.common.mongo;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Secuencias Long por colección, para el `_id` de los documentos.
 *
 * * MongoDB no tiene autoincremental. Se usa una colección `contadores` con
 * * un documento por colección ({ _id: "reservas", secuencia: 42 }) y un
 * * findAndModify atómico con $inc: dos inserts concurrentes nunca reciben
 * * el mismo número.
 */
@Service
@RequiredArgsConstructor
public class SecuenciaService {

    static final String COLECCION_CONTADORES = "contadores";

    private final MongoTemplate mongoTemplate;

    public long siguiente(String coleccion) {
        Contador contador = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(coleccion)),
                new Update().inc("secuencia", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Contador.class,
                COLECCION_CONTADORES);
        return contador.secuencia();
    }

    record Contador(String id, long secuencia) {
    }
}
