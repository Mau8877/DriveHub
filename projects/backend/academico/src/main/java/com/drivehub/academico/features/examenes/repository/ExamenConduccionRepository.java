package com.drivehub.academico.features.examenes.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.examenes.enums.EstadoExamen;
import com.drivehub.academico.features.examenes.model.ExamenConduccion;

/**
 * Acceso a la colección `examenes_conduccion`, siempre dentro de una escuela: un examen de otra escuela es, para
 * quien consulta, uno que no existe. Sin lógica de negocio.
 */
public interface ExamenConduccionRepository
        extends MongoRepository<ExamenConduccion, Long>, ExamenConduccionRepositoryCustom {

    Optional<ExamenConduccion> findByEscuelaCodigoAndCodigoExamenConduccionAndDeletedAtIsNull(
        UUID escuelaCodigo, UUID codigoExamenConduccion);

    /** Cuántos exámenes tuvo el estudiante en esa categoría sin contar los cancelados: el intento es ese número + 1. */
    long countByEscuelaCodigoAndEstudianteCodigoAndCategoriaAndEstadoNotAndDeletedAtIsNull(
        UUID escuelaCodigo, UUID estudianteCodigo, String categoria, EstadoExamen estado);

    boolean existsByEscuelaCodigoAndEstudianteCodigoAndCategoriaAndEstadoAndDeletedAtIsNull(
        UUID escuelaCodigo, UUID estudianteCodigo, String categoria, EstadoExamen estado);
}
