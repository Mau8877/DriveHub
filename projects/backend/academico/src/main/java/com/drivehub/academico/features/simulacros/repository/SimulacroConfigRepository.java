package com.drivehub.academico.features.simulacros.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.simulacros.model.SimulacroConfig;

/**
 * Acceso a la colección `simulacros_config`, siempre dentro de una escuela: un simulacro de otra escuela
 * es, para quien consulta, uno que no existe. Sin lógica de negocio.
 */
public interface SimulacroConfigRepository
        extends MongoRepository<SimulacroConfig, Long>, SimulacroConfigRepositoryCustom {

    Optional<SimulacroConfig> findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull(
        UUID escuelaCodigo, UUID codigoSimulacro);

    // * El nombre ya viaja recortado; la comparación no distingue mayúsculas
    boolean existsByEscuelaCodigoAndNombreIgnoreCaseAndDeletedAtIsNull(UUID escuelaCodigo, String nombre);

    boolean existsByEscuelaCodigoAndNombre(UUID escuelaCodigo, String nombre);
}
