package com.drivehub.pagos.features.pagos.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;

import jakarta.persistence.LockModeType;

/**
 * Acceso a datos de los pagos, sin lógica. El id interno Long nunca sale del servicio: se busca por el codigo
 * público y siempre con el estudiante y la escuela (nadie ve el pago de otro).
 */
public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    Optional<Pago> findByIdempotencyKey(String idempotencyKey);

    Optional<Pago> findByCodigo(UUID codigo);

    Optional<Pago> findByCodigoAndEstudianteCodigoAndEscuelaCodigo(UUID codigo, UUID estudianteCodigo,
            UUID escuelaCodigo);

    /**
     * Las horas compradas de esos estudiantes (CU29): la suma de las horas de sus pagos `CONFIRMADO` de horas.
     * Los pagos pendientes, fallidos y reembolsados (`REVERTIDO`) no cuentan. Cada fila es
     * `[estudianteCodigo (UUID), horas (Number)]`; el que no compró nada no aparece.
     */
    @Query("select p.estudianteCodigo, sum(p.horas) from Pago p where p.escuelaCodigo = :escuela"
        + " and p.estudianteCodigo in :estudiantes"
        + " and p.estado = com.drivehub.pagos.features.pagos.enums.EstadoPago.CONFIRMADO"
        + " and p.concepto = com.drivehub.pagos.features.pagos.enums.ConceptoPago.HORAS"
        + " group by p.estudianteCodigo")
    List<Object[]> horasCompradasPor(@Param("escuela") UUID escuelaCodigo,
            @Param("estudiantes") Collection<UUID> estudiantes);

    /**
     * Los ids de los pagos en ese estado cuya página de pago venció antes de `limite`, los más viejos primero.
     * Devuelve ids (no entidades): el proceso de vencimiento vuelve a leer cada pago en su propia transacción.
     */
    @Query("select p.id from Pago p where p.estado = :estado and p.expiraEn < :limite order by p.expiraEn, p.id")
    List<Long> idsVencidos(@Param("estado") EstadoPago estado, @Param("limite") Instant limite, Pageable pagina);

    /**
     * El pago con bloqueo de escritura: quien lo cambia (confirmar, gastar o devolver horas) espera a que termine
     * el que lo estaba cambiando.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    Optional<Pago> buscarParaActualizar(@Param("id") Long id);

    /**
     * Las compras confirmadas del estudiante a las que todavía le quedan horas, las más antiguas primero, con
     * bloqueo de escritura: de ahí se descuentan las horas que se gastan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.escuelaCodigo = :escuela and p.estudianteCodigo = :estudiante"
        + " and p.estado = com.drivehub.pagos.features.pagos.enums.EstadoPago.CONFIRMADO and p.horasRestantes > 0"
        + " order by p.confirmadoEn, p.id")
    List<Pago> confirmadosConHorasRestantes(@Param("escuela") UUID escuelaCodigo,
            @Param("estudiante") UUID estudianteCodigo);
}
