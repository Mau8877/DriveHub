package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.RolModel;

public interface RolJpaRepository extends JpaRepository<RolModel, Long> {

    Optional<RolModel> findByNombre(String nombre);
}
