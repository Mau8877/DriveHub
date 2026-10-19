package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioRolModel;

public interface UsuarioRolJpaRepository extends JpaRepository<UsuarioRolModel, Long> {

    /** Los roles de un usuario, con el rol ya cargado (la relación es perezosa). */
    @Query("select ur from UsuarioRolModel ur join fetch ur.rol where ur.usuario.id = :usuarioId")
    List<UsuarioRolModel> findConRolPorUsuarioId(@Param("usuarioId") Long usuarioId);
}
