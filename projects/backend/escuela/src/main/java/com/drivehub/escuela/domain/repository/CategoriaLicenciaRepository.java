package com.drivehub.escuela.domain.repository;

import java.util.Optional;

import com.drivehub.escuela.domain.entity.CategoriaLicencia;

/**
 * Lo que el negocio necesita saber del catálogo de categorías de licencia.
 */
public interface CategoriaLicenciaRepository {

    /**
     * Listado paginado en el orden del catálogo sembrado (por id).
     *
     * @param pagina 1-indexada
     */
    Pagina<CategoriaLicencia> listar(int pagina, int tamano);

    /**
     * Categoría con esa sigla, sin distinguir mayúsculas. Vacío si no existe.
     */
    Optional<CategoriaLicencia> buscarPorSigla(String sigla);
}
