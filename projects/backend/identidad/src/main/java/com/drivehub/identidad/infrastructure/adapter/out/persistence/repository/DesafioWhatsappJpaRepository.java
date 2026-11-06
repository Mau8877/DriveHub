package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.DesafioWhatsappModel;

public interface DesafioWhatsappJpaRepository extends JpaRepository<DesafioWhatsappModel, Long> {

    @Query("select d from DesafioWhatsappModel d left join fetch d.usuario where d.codigo = :codigo")
    Optional<DesafioWhatsappModel> findConUsuarioPorCodigo(@Param("codigo") UUID codigo);

    @Query("select d from DesafioWhatsappModel d join fetch d.usuario u "
        + "where u.codigo = :usuarioCodigo and d.telefonoHash = :telefonoHash "
        + "and d.usado = false and d.expiraEn > :ahora order by d.createdAt desc, d.id desc")
    List<DesafioWhatsappModel> vigentes(@Param("usuarioCodigo") UUID usuarioCodigo,
        @Param("telefonoHash") String telefonoHash, @Param("ahora") Instant ahora, Pageable pagina);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update DesafioWhatsappModel d set d.usado = true where d.usado = false "
        + "and d.usuario.id = (select u.id from UsuarioModel u where u.codigo = :usuarioCodigo)")
    int invalidarPendientes(@Param("usuarioCodigo") UUID usuarioCodigo);

    long countByTelefonoHashAndCreatedAtGreaterThanEqual(String telefonoHash, Instant desde);

    long countByCorreoHashAndCreatedAtGreaterThanEqual(String correoHash, Instant desde);
}
