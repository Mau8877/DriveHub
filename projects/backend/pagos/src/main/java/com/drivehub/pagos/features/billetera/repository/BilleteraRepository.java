package com.drivehub.pagos.features.billetera.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.pagos.features.billetera.model.BilleteraHoras;

import jakarta.persistence.LockModeType;

/**
 * Acceso a datos de las billeteras de horas, sin lógica. Siempre con la escuela y el estudiante: la billetera
 * de uno nunca se encuentra con el código de otro.
 */
public interface BilleteraRepository extends JpaRepository<BilleteraHoras, Long> {

    Optional<BilleteraHoras> findByEscuelaCodigoAndEstudianteCodigo(UUID escuelaCodigo, UUID estudianteCodigo);

    /**
     * Lee la billetera CON bloqueo de escritura: dos operaciones sobre la misma billetera (acreditar, consumir,
     * devolver) se hacen una después de la otra, nunca a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BilleteraHoras b where b.escuelaCodigo = :escuela and b.estudianteCodigo = :estudiante")
    Optional<BilleteraHoras> buscarParaActualizar(@Param("escuela") UUID escuelaCodigo,
            @Param("estudiante") UUID estudianteCodigo);

    /**
     * Crea la billetera vacía si todavía no existe. Si dos operaciones la crean a la vez, una sola se queda: la
     * otra no hace nada (`on conflict do nothing`) y no rompe su transacción.
     */
    @Modifying
    @Query(value = "insert into billetera_horas (codigo_billetera_horas, escuela_codigo, estudiante_codigo,"
        + " horas_disponibles, version) values (:codigo, :escuela, :estudiante, 0, 0)"
        + " on conflict (escuela_codigo, estudiante_codigo) do nothing", nativeQuery = true)
    int crearSiNoExiste(@Param("codigo") UUID codigo, @Param("escuela") UUID escuelaCodigo,
            @Param("estudiante") UUID estudianteCodigo);
}
