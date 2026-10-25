package com.drivehub.pagos.features.billetera.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drivehub.pagos.features.billetera.enums.TipoMovimiento;
import com.drivehub.pagos.features.billetera.model.MovimientoHoras;

/**
 * Acceso a datos del libro de la billetera, sin lógica.
 */
public interface MovimientoHorasRepository extends JpaRepository<MovimientoHoras, Long> {

    /** Si esa operación (clave) ya movió horas de esa compra: sirve para no repetirla. */
    boolean existsByIdempotencyKeyAndPagoId(String idempotencyKey, Long pagoId);

    /** Los movimientos de una operación (clave) de una billetera: una operación puede tocar varias compras. */
    List<MovimientoHoras> findByBilleteraHorasIdAndIdempotencyKeyAndTipo(Long billeteraId, String idempotencyKey,
            TipoMovimiento tipo);

    /** Los movimientos de un tipo ligados a una clase (para devolver lo que esa clase consumió). */
    List<MovimientoHoras> findByBilleteraHorasIdAndClaseCodigoAndTipo(Long billeteraId, UUID claseCodigo,
            TipoMovimiento tipo);

    List<MovimientoHoras> findByBilleteraHorasIdOrderByCreatedAtDescIdDesc(Long billeteraId);
}
