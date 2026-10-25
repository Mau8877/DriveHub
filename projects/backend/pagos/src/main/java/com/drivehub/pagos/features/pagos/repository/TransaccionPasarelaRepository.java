package com.drivehub.pagos.features.pagos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;

/**
 * Acceso a datos de los intercambios con la pasarela, sin lógica.
 */
public interface TransaccionPasarelaRepository extends JpaRepository<TransaccionPasarela, Long> {

    /** El último intercambio de un pago (por id interno del pago). */
    Optional<TransaccionPasarela> findFirstByPagoIdOrderByIdDesc(Long pagoId);

    Optional<TransaccionPasarela> findByReferenciaExterna(String referenciaExterna);
}
