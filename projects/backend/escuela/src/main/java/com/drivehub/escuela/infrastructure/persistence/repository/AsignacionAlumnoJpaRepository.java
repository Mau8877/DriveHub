package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.escuela.infrastructure.persistence.model.AsignacionAlumnoModel;

public interface AsignacionAlumnoJpaRepository extends JpaRepository<AsignacionAlumnoModel, Long> {

    @Query(value = "select a.estudianteCodigo from AsignacionAlumnoModel a"
        + " where a.instructor.escuelaCodigo = :escuela and a.instructor.usuarioCodigo = :instructor"
        + " and a.fechaFin is null order by a.createdAt, a.id",
        countQuery = "select count(a) from AsignacionAlumnoModel a"
        + " where a.instructor.escuelaCodigo = :escuela and a.instructor.usuarioCodigo = :instructor"
        + " and a.fechaFin is null")
    Page<UUID> estudiantesVigentes(@Param("escuela") UUID escuelaCodigo,
            @Param("instructor") UUID instructorUsuarioCodigo, Pageable pagina);

    @Query("select count(a) > 0 from AsignacionAlumnoModel a"
        + " where a.instructor.escuelaCodigo = :escuela and a.instructor.usuarioCodigo = :instructor"
        + " and a.estudianteCodigo = :estudiante and a.fechaFin is null")
    boolean existeVigente(@Param("escuela") UUID escuelaCodigo, @Param("instructor") UUID instructorUsuarioCodigo,
            @Param("estudiante") UUID estudianteCodigo);
}
