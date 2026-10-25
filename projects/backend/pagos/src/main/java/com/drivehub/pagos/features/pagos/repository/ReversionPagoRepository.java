package com.drivehub.pagos.features.pagos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.model.ReversionPago;

import jakarta.persistence.LockModeType;

/**
 * Acceso a datos de las reversiones de pago, sin lógica (CU28 las consulta; CU31 las crea y las resuelve).
 */
public interface ReversionPagoRepository extends JpaRepository<ReversionPago, Long> {

    boolean existsByPagoIdAndEstado(Long pagoId, EstadoReversion estado);

    /** Si el pago tiene una reversión vigente: cualquiera que no haya fallado. */
    boolean existsByPagoIdAndEstadoNot(Long pagoId, EstadoReversion estado);

    Optional<ReversionPago> findByCodigo(UUID codigo);

    Optional<ReversionPago> findByCodigoAndPagoId(UUID codigo, Long pagoId);

    Optional<ReversionPago> findByReferenciaExterna(String referenciaExterna);

    /** La reversión con bloqueo de escritura: dos avisos de la pasarela a la vez se atienden uno tras otro. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReversionPago r where r.codigo = :codigo")
    Optional<ReversionPago> buscarParaActualizarPorCodigo(@Param("codigo") UUID codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReversionPago r where r.referenciaExterna = :referencia")
    Optional<ReversionPago> buscarParaActualizarPorReferencia(@Param("referencia") String referencia);

    /**
     * Los ids de las compensaciones automáticas que están pedidas pero la pasarela todavía no creó su devolución,
     * las más viejas primero: lo que reintenta el proceso periódico.
     */
    @Query("select r.id from ReversionPago r where r.estado = com.drivehub.pagos.features.pagos.enums.EstadoReversion"
        + ".SOLICITADA and r.origen = com.drivehub.pagos.features.pagos.enums.OrigenReversion.SAGA"
        + " and r.referenciaExterna is null order by r.id")
    List<Long> idsDeCompensacionesPendientes(Pageable pagina);

    /** De esos pagos, los ids de los que tienen alguna reversión en ese estado (una sola consulta por página). */
    @Query("select distinct r.pago.id from ReversionPago r where r.pago.id in :pagoIds and r.estado = :estado")
    List<Long> pagoIdsConReversion(@Param("pagoIds") Collection<Long> pagoIds,
            @Param("estado") EstadoReversion estado);
}
