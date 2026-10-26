package com.drivehub.academico.features.horarios.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.horarios.model.Franja;

/**
 * Acceso a las franjas. Las consultas con filtros opcionales, el cambio de estado atómico
 * y los borrados masivos están en {@link FranjaRepositoryCustom}.
 */
public interface FranjaRepository extends MongoRepository<Franja, Long>, FranjaRepositoryCustom {

    Optional<Franja> findByCodigoFranjaAndInstructorCodigoAndDeletedAtIsNull(UUID codigoFranja, UUID instructorCodigo);

    /**
     * La franja de una escuela, sea cual sea su estado (CU14): sirve para distinguir «no existe» de «no está
     * libre».
     */
    Optional<Franja> findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(UUID codigoFranja, UUID escuelaCodigo);
}
