package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioModel, Long> {

    boolean existsByEmail(String email);
}
