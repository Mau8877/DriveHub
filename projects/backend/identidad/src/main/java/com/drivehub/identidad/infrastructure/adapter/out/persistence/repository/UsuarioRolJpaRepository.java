package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioRolModel;

public interface UsuarioRolJpaRepository extends JpaRepository<UsuarioRolModel, Long> {
}
