package com.drivehub.escuela.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.Escuela;

/**
 * Lo que el negocio necesita saber de las escuelas.
 */
public interface EscuelaRepository {

    /**
     * Escuela con ese slug, solo si está ACTIVA. Vacío si no existe o está inactiva.
     */
    Optional<Escuela> buscarActivaPorSlug(String slug);

    /**
     * Escuela con ese codigo, en cualquier estado (ACTIVA o INACTIVA).
     */
    Optional<Escuela> buscarPorCodigo(UUID codigo);
}
