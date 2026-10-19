package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.RefreshTokenModel;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenModel, Long> {

    @Query("select t from RefreshTokenModel t join fetch t.usuario where t.tokenHash = :hash")
    Optional<RefreshTokenModel> findConUsuarioPorHash(@Param("hash") String hash);

    @Query("select t from RefreshTokenModel t join fetch t.usuario where t.codigo = :codigo")
    Optional<RefreshTokenModel> findConUsuarioPorCodigo(@Param("codigo") UUID codigo);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshTokenModel t set t.revocado = true "
        + "where t.revocado = false and t.dispositivo = :dispositivo "
        + "and t.usuario.id = (select u.id from UsuarioModel u where u.codigo = :usuarioCodigo)")
    int revocarActivos(@Param("usuarioCodigo") UUID usuarioCodigo, @Param("dispositivo") Dispositivo dispositivo);
}
