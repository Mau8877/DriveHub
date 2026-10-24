package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.escuela.infrastructure.persistence.model.CategoriaLicenciaModel;

public interface CategoriaLicenciaJpaRepository extends JpaRepository<CategoriaLicenciaModel, Long> {

    Optional<CategoriaLicenciaModel> findBySiglaIgnoreCase(String sigla);
}
