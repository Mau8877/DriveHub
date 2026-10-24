package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.drivehub.escuela.infrastructure.persistence.model.InstructorModel;

public interface InstructorJpaRepository
        extends JpaRepository<InstructorModel, Long>, JpaSpecificationExecutor<InstructorModel> {

    Optional<InstructorModel> findByCodigoAndEscuelaCodigo(UUID codigo, UUID escuelaCodigo);

    boolean existsByNroLicencia(String nroLicencia);

    boolean existsByNroLicenciaAndCodigoNot(String nroLicencia, UUID codigo);
}
