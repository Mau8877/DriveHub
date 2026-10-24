package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.escuela.infrastructure.persistence.model.InstructorCategoriaModel;

public interface InstructorCategoriaJpaRepository extends JpaRepository<InstructorCategoriaModel, Long> {

    // * Una sola consulta para todos los instructores de la página; la categoría viaja con un JOIN FETCH
    @Query("select ic from InstructorCategoriaModel ic join fetch ic.categoria c "
        + "where ic.instructor.id in :instructorIds order by c.id")
    List<InstructorCategoriaModel> deInstructores(@Param("instructorIds") Collection<Long> instructorIds);
}
