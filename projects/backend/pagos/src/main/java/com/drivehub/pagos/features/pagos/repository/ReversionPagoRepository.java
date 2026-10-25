package com.drivehub.pagos.features.pagos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.model.ReversionPago;

/**
 * Acceso a datos de las reversiones de pago, sin lógica (CU28 las consulta; CU31 las crea).
 */
public interface ReversionPagoRepository extends JpaRepository<ReversionPago, Long> {

    boolean existsByPagoIdAndEstado(Long pagoId, EstadoReversion estado);

    Optional<ReversionPago> findByCodigo(UUID codigo);

    /** De esos pagos, los ids de los que tienen alguna reversión en ese estado (una sola consulta por página). */
    @Query("select distinct r.pago.id from ReversionPago r where r.pago.id in :pagoIds and r.estado = :estado")
    List<Long> pagoIdsConReversion(@Param("pagoIds") Collection<Long> pagoIds,
            @Param("estado") EstadoReversion estado);
}
