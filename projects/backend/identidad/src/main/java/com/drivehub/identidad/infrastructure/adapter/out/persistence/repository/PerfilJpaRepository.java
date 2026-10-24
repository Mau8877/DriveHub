package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.PerfilModel;

public interface PerfilJpaRepository extends JpaRepository<PerfilModel, Long> {

    boolean existsByCi(String ci);

    /** Si otra cuenta (no la de ese código) ya usa ese CI. */
    boolean existsByCiAndUsuarioCodigoNot(String ci, UUID usuarioCodigo);
}
