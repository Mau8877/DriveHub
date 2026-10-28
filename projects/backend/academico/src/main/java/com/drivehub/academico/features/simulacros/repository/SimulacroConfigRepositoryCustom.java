package com.drivehub.academico.features.simulacros.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.features.simulacros.model.SimulacroConfig;

/**
 * Consultas de simulacros que no se pueden expresar con un método derivado (filtros opcionales).
 */
public interface SimulacroConfigRepositoryCustom {

    /**
     * Listado de una escuela, sin los eliminados lógicamente, del más reciente al más antiguo.
     */
    Page<SimulacroConfig> listar(UUID escuelaCodigo, FiltroSimulacros filtro, Pageable pageable);
}
