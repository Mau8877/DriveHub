package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioRolModel;

public interface UsuarioRolJpaRepository extends JpaRepository<UsuarioRolModel, Long> {

    /** Los roles de un usuario, con el rol ya cargado (la relación es perezosa). */
    @Query("select ur from UsuarioRolModel ur join fetch ur.rol where ur.usuario.id = :usuarioId")
    List<UsuarioRolModel> findConRolPorUsuarioId(@Param("usuarioId") Long usuarioId);

    /** Si alguna cuenta de esa escuela tiene ese rol (CU05: ¿ya tiene Administrador?). */
    @Query("select count(ur) > 0 from UsuarioRolModel ur"
        + " where ur.usuario.escuelaCodigo = :escuelaCodigo and ur.rol.nombre = :rol")
    boolean existeUsuarioConRolEnEscuela(@Param("escuelaCodigo") UUID escuelaCodigo, @Param("rol") String rol);

    /** Las cuentas de esa escuela con ese rol. */
    @Query("select ur.usuario from UsuarioRolModel ur"
        + " where ur.usuario.escuelaCodigo = :escuelaCodigo and ur.rol.nombre = :rol")
    List<UsuarioModel> findUsuariosConRolEnEscuela(@Param("escuelaCodigo") UUID escuelaCodigo, @Param("rol") String rol);
}
