package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.VinculoWhatsappModel;

public interface VinculoWhatsappJpaRepository extends JpaRepository<VinculoWhatsappModel, Long> {

    @Query("select v from VinculoWhatsappModel v join fetch v.usuario where v.telefonoHash = :hash")
    Optional<VinculoWhatsappModel> findConUsuarioPorTelefonoHash(@Param("hash") String hash);

    @Query("select v from VinculoWhatsappModel v join fetch v.usuario u where u.codigo = :codigo")
    Optional<VinculoWhatsappModel> findConUsuarioPorUsuarioCodigo(@Param("codigo") UUID codigo);

    // * Borrado físico: `telefono_hash` y `usuario_id` son únicos y un borrado lógico seguiría ocupándolos
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM vinculo_whatsapp WHERE telefono_hash = :hash"
        + " OR usuario_id IN (SELECT id FROM usuario WHERE codigo_usuario = :usuarioCodigo)", nativeQuery = true)
    int borrarDelNumeroOdeLaCuenta(@Param("hash") String hash, @Param("usuarioCodigo") UUID usuarioCodigo);
}
