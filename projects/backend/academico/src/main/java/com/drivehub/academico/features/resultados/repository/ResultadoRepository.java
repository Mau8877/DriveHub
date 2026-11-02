package com.drivehub.academico.features.resultados.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;

/**
 * Acceso a la colección `resultados` (CU23). Sin lógica de negocio.
 */
public interface ResultadoRepository extends MongoRepository<Resultado, Long>, ResultadoRepositoryCustom {

    /** Si la evaluación de ese origen ya tiene su nota (CU18: una prueba guardada puede haberse quedado sin ella). */
    boolean existsByOrigenAndOrigenIdAndDeletedAtIsNull(OrigenResultado origen, Long origenId);
}
