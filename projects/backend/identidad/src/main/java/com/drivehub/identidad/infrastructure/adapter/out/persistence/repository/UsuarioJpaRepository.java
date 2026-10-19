package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioModel, Long> {

    boolean existsByEmail(String email);

    Optional<UsuarioModel> findByEmail(String email);

    Optional<UsuarioModel> findByCodigo(UUID codigo);
}
