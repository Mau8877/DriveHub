package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.infrastructure.persistence.model.EscuelaModel;

public interface EscuelaJpaRepository extends JpaRepository<EscuelaModel, Long> {

    Optional<EscuelaModel> findBySlugAndEstado(String slug, EstadoEscuela estado);

    boolean existsBySlug(String slug);

    Optional<EscuelaModel> findByCodigo(UUID codigo);
}
