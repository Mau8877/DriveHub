package com.drivehub.academico.features.resultados.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.resultados.model.Resultado;

/**
 * Acceso a la colección `resultados` (CU23). Sin lógica de negocio.
 */
public interface ResultadoRepository extends MongoRepository<Resultado, Long> {
}
