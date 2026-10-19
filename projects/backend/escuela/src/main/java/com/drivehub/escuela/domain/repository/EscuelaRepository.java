package com.drivehub.escuela.domain.repository;

import java.util.Optional;

import com.drivehub.escuela.domain.entity.Escuela;

/**
 * Lo que el negocio necesita saber de las escuelas.
 */
public interface EscuelaRepository {

    /**
     * Escuela con ese slug, solo si está ACTIVA. Vacío si no existe o está inactiva.
     */
    Optional<Escuela> buscarActivaPorSlug(String slug);
}
