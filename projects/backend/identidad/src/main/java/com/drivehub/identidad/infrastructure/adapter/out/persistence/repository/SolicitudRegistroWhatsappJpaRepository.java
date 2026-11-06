package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.SolicitudRegistroWhatsappModel;

public interface SolicitudRegistroWhatsappJpaRepository extends JpaRepository<SolicitudRegistroWhatsappModel, Long> {

    Optional<SolicitudRegistroWhatsappModel> findByTokenHash(String tokenHash);

    // * Borrado físico (SQL nativo): son datos personales que no deben quedar con un borrado lógico
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM solicitud_registro_whatsapp WHERE telefono = :telefono", nativeQuery = true)
    int borrarDelTelefono(@Param("telefono") String telefono);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM solicitud_registro_whatsapp WHERE expira_en < :ahora", nativeQuery = true)
    int borrarVencidas(@Param("ahora") Instant ahora);
}
