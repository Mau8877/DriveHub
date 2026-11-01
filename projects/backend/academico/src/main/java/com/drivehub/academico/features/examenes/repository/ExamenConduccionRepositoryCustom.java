package com.drivehub.academico.features.examenes.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.features.examenes.model.ExamenConduccion;

/**
 * Consultas de exámenes que no se pueden expresar con un método derivado (filtros opcionales).
 */
public interface ExamenConduccionRepositoryCustom {

    /**
     * Listado de una escuela, sin los eliminados lógicamente, del examen más próximo (o reciente) al más antiguo.
     */
    Page<ExamenConduccion> listar(UUID escuelaCodigo, FiltroExamenes filtro, Pageable pageable);
}
