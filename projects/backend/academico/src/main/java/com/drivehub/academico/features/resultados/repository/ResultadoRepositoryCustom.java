package com.drivehub.academico.features.resultados.repository;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.features.resultados.model.Resultado;

/**
 * Consultas de resultados que no se pueden expresar con un método derivado (filtros opcionales).
 */
public interface ResultadoRepositoryCustom {

    /** Las notas de una escuela, sin las eliminadas lógicamente, de la más reciente a la más antigua. */
    Page<Resultado> listarDeEscuela(UUID escuelaCodigo, FiltroResultados filtro, Pageable pageable);

    /** Las notas de un estudiante, de la más reciente a la más antigua. */
    Page<Resultado> listarDeEstudiante(UUID estudianteCodigo, FiltroResultados filtro, Pageable pageable);

    /** Las notas de varios estudiantes (los alumnos de un instructor), de la más reciente a la más antigua. */
    Page<Resultado> listarDeEstudiantes(Collection<UUID> estudiantes, FiltroResultados filtro, Pageable pageable);
}
