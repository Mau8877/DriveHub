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
     * Escuela con ese codigo, en cualquier estado, también si está desactivada (borrado lógico).
     */
    Optional<Escuela> buscarPorCodigo(UUID codigo);

    /**
     * Crea la escuela con el `codigo` que trae (lo genera el caso de uso). El slug se valida
     * contra el UNIQUE en el mismo instante: una colisión falla acá y no al confirmar.
     */
    Escuela guardar(Escuela escuela);

    /**
     * Si ya hay una escuela con ese slug, activa o desactivada: el slug sigue reservado
     * aunque la escuela esté eliminada lógicamente.
     */
    boolean existePorSlug(String slug);

    /**
     * Listado paginado, de la más reciente a la más antigua, con activas e inactivas.
     *
     * @param pagina 1-indexada
     */
    Pagina<Escuela> listar(FiltroEscuelas filtro, int pagina, int tamano);

    /**
     * Cambia el nombre de una escuela ACTIVA. Vacío si no existe o está desactivada.
     */
    Optional<Escuela> actualizarNombre(UUID codigo, String nombre);

    /**
     * Pone el borrado lógico. Devuelve si cambió algo: false si no existe o ya estaba inactiva.
     */
    boolean desactivar(UUID codigo);

    /**
     * Quita el borrado lógico. Devuelve si cambió algo: false si no existe o ya estaba activa.
     */
    boolean reactivar(UUID codigo);
}
