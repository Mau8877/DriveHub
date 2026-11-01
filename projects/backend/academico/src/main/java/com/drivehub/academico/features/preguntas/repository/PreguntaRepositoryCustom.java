package com.drivehub.academico.features.preguntas.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.drivehub.academico.features.preguntas.model.Pregunta;

/**
 * Consultas del banco que no se pueden expresar con un método derivado (filtros opcionales).
 */
public interface PreguntaRepositoryCustom {

    /**
     * Listado de una escuela, sin las eliminadas lógicamente, de la más reciente a la más antigua.
     */
    Page<Pregunta> listar(UUID escuelaCodigo, FiltroPreguntas filtro, Pageable pageable);

    /**
     * Las preguntas activas de un tema (sin distinguir mayúsculas) en esas categorías, para armar un intento de
     * simulacro (CU21).
     */
    java.util.List<Pregunta> activasDelTema(UUID escuelaCodigo, java.util.Collection<String> categorias, String tema);
}
