package com.drivehub.escuela.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.escuela.infrastructure.persistence.model.AsignacionVehiculoModel;

public interface AsignacionVehiculoJpaRepository extends JpaRepository<AsignacionVehiculoModel, Long> {

    @Query("select a from AsignacionVehiculoModel a join fetch a.vehiculo join fetch a.instructor"
        + " where a.vehiculo.escuelaCodigo = :escuela and a.vehiculo.codigo = :vehiculo and a.fechaFin is null")
    Optional<AsignacionVehiculoModel> vigente(@Param("escuela") UUID escuelaCodigo,
            @Param("vehiculo") UUID vehiculoCodigo);

    @Query("select max(a.fechaFin) from AsignacionVehiculoModel a"
        + " where a.vehiculo.escuelaCodigo = :escuela and a.vehiculo.codigo = :vehiculo")
    LocalDate ultimaFechaFin(@Param("escuela") UUID escuelaCodigo, @Param("vehiculo") UUID vehiculoCodigo);

    @Query(value = "select a from AsignacionVehiculoModel a join fetch a.vehiculo join fetch a.instructor"
        + " where a.vehiculo.escuelaCodigo = :escuela and a.vehiculo.codigo = :vehiculo"
        + " order by a.fechaInicio desc, a.id desc",
        countQuery = "select count(a) from AsignacionVehiculoModel a"
        + " where a.vehiculo.escuelaCodigo = :escuela and a.vehiculo.codigo = :vehiculo")
    Page<AsignacionVehiculoModel> historial(@Param("escuela") UUID escuelaCodigo,
            @Param("vehiculo") UUID vehiculoCodigo, Pageable pagina);
}
