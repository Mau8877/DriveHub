package com.drivehub.pagos.features.billetera.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Las horas gastadas con UN instructor (CU29), por estudiante: consumos menos devoluciones de ese
     * `instructor_codigo`, en una sola consulta. Cada fila es `[estudianteCodigo (UUID), horas (Number)]`; el
     * estudiante sin movimientos con ese instructor no aparece. Nunca suma lo gastado con otros instructores.
     */
    @Query("select m.billeteraHoras.estudianteCodigo, sum(case when m.tipo = "
        + "com.drivehub.pagos.features.billetera.enums.TipoMovimiento.CONSUMO then m.horas else -m.horas end)"
        + " from MovimientoHoras m where m.billeteraHoras.escuelaCodigo = :escuela"
        + " and m.billeteraHoras.estudianteCodigo in :estudiantes and m.instructorCodigo = :instructor"
        + " and m.tipo in (com.drivehub.pagos.features.billetera.enums.TipoMovimiento.CONSUMO,"
        + " com.drivehub.pagos.features.billetera.enums.TipoMovimiento.DEVOLUCION)"
        + " group by m.billeteraHoras.estudianteCodigo")
    List<Object[]> horasGastadasConInstructor(@Param("escuela") UUID escuelaCodigo,
            @Param("instructor") UUID instructorCodigo, @Param("estudiantes") Collection<UUID> estudiantes);

    /**
     * Las clases de un estudiante con UN instructor (CU29), con sus horas netas (consumidas menos devueltas) y la
     * fecha del primer consumo, la más reciente primero. Una clase cuyas horas se devolvieron por completo no
     * aparece. Cada fila es `[claseCodigo (UUID), fecha (Instant), horas (Number)]`.
     */
    @Query("select m.claseCodigo, min(case when m.tipo = "
        + "com.drivehub.pagos.features.billetera.enums.TipoMovimiento.CONSUMO then m.createdAt end),"
        + " sum(case when m.tipo = com.drivehub.pagos.features.billetera.enums.TipoMovimiento.CONSUMO"
        + " then m.horas else -m.horas end)"
        + " from MovimientoHoras m where m.billeteraHoras.escuelaCodigo = :escuela"
        + " and m.billeteraHoras.estudianteCodigo = :estudiante and m.instructorCodigo = :instructor"
        + " and m.claseCodigo is not null"
        + " and m.tipo in (com.drivehub.pagos.features.billetera.enums.TipoMovimiento.CONSUMO,"
        + " com.drivehub.pagos.features.billetera.enums.TipoMovimiento.DEVOLUCION)"
        + " group by m.claseCodigo"
        + " having sum(case when m.tipo = com.drivehub.pagos.features.billetera.enums.TipoMovimiento.CONSUMO"
        + " then m.horas else -m.horas end) > 0"
        + " order by min(case when m.tipo = "
        + "com.drivehub.pagos.features.billetera.enums.TipoMovimiento.CONSUMO then m.createdAt end) desc")
    List<Object[]> consumosPorClase(@Param("escuela") UUID escuelaCodigo,
            @Param("instructor") UUID instructorCodigo, @Param("estudiante") UUID estudianteCodigo);
}
