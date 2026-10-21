package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.TokenRecuperacionModel;

public interface TokenRecuperacionJpaRepository extends JpaRepository<TokenRecuperacionModel, Long> {

    @Query("select t from TokenRecuperacionModel t join fetch t.usuario where t.tokenHash = :hash")
    Optional<TokenRecuperacionModel> findConUsuarioPorHash(@Param("hash") String hash);

    @Query("select t from TokenRecuperacionModel t join fetch t.usuario where t.codigo = :codigo")
    Optional<TokenRecuperacionModel> findConUsuarioPorCodigo(@Param("codigo") UUID codigo);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenRecuperacionModel t set t.usado = true "
        + "where t.usado = false and t.tipo = :tipo "
        + "and t.usuario.id = (select u.id from UsuarioModel u where u.codigo = :usuarioCodigo)")
    int invalidarPendientes(@Param("usuarioCodigo") UUID usuarioCodigo, @Param("tipo") TipoTokenCorreo tipo);
}
