package com.drivehub.academico.features.preguntas.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.preguntas.model.Pregunta;

/**
 * Acceso a la colección `banco_preguntas`, siempre dentro de una escuela: una pregunta de otra escuela
 * es, para quien consulta, una pregunta que no existe. Sin lógica de negocio.
 */
public interface PreguntaRepository extends MongoRepository<Pregunta, Long>, PreguntaRepositoryCustom {

    Optional<Pregunta> findByEscuelaCodigoAndCodigoPreguntaAndDeletedAtIsNull(UUID escuelaCodigo, UUID codigoPregunta);

    boolean existsByEscuelaCodigoAndEnunciado(UUID escuelaCodigo, String enunciado);
}
