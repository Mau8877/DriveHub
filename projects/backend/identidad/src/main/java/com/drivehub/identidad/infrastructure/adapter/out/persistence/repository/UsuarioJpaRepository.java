package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;

public interface UsuarioJpaRepository
        extends JpaRepository<UsuarioModel, Long>, JpaSpecificationExecutor<UsuarioModel> {

    boolean existsByEmail(String email);

    // * El perfil viaja en la misma consulta (una sola, sin N+1)
    @EntityGraph(attributePaths = "perfil")
    Optional<UsuarioModel> findByEmail(String email);

    @EntityGraph(attributePaths = "perfil")
    Optional<UsuarioModel> findByCodigo(UUID codigo);

    // * Borrado físico en orden, de las filas hijas al usuario (el esquema no tiene ON DELETE CASCADE).
    // * SQL nativo: tiene que alcanzar también las filas con borrado lógico.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM usuario_rol WHERE usuario_id IN"
        + " (SELECT id FROM usuario WHERE codigo_usuario = :codigo)", nativeQuery = true)
    int borrarRolesDe(@Param("codigo") UUID codigo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM refresh_token WHERE usuario_id IN"
        + " (SELECT id FROM usuario WHERE codigo_usuario = :codigo)", nativeQuery = true)
    int borrarSesionesDe(@Param("codigo") UUID codigo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM token_recuperacion WHERE usuario_id IN"
        + " (SELECT id FROM usuario WHERE codigo_usuario = :codigo)", nativeQuery = true)
    int borrarEnlacesDeCorreoDe(@Param("codigo") UUID codigo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM perfil WHERE usuario_id IN"
        + " (SELECT id FROM usuario WHERE codigo_usuario = :codigo)", nativeQuery = true)
    int borrarPerfilDe(@Param("codigo") UUID codigo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM usuario WHERE codigo_usuario = :codigo", nativeQuery = true)
    int borrarUsuario(@Param("codigo") UUID codigo);
}
