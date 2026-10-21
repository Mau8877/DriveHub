package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.escuela.infrastructure.persistence.model.EscuelaModel;

/**
 * Las consultas que necesitan ver las escuelas eliminadas lógicamente son SQL nativo a
 * propósito: el @SQLRestriction del BaseModel no se aplica a las consultas nativas.
 */
public interface EscuelaJpaRepository extends JpaRepository<EscuelaModel, Long> {

    /** Solo escuelas activas: el @SQLRestriction del BaseModel oculta las eliminadas. */
    Optional<EscuelaModel> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Solo escuelas activas (ver arriba). */
    Optional<EscuelaModel> findByCodigo(UUID codigo);

    /** La escuela con ese codigo, activa o eliminada lógicamente. */
    @Query(value = "SELECT * FROM escuela WHERE codigo_escuela = :codigo", nativeQuery = true)
    Optional<EscuelaModel> buscarPorCodigoIncluyendoEliminadas(@Param("codigo") UUID codigo);

    /** El slug sigue reservado aunque la escuela esté eliminada lógicamente. */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM escuela WHERE slug = :slug)", nativeQuery = true)
    boolean existePorSlugIncluyendoEliminadas(@Param("slug") String slug);

    /**
     * Listado con activas e inactivas. `busqueda` ya viene como patrón LIKE en minúsculas (o nulo);
     * `estado` es ACTIVA, INACTIVA o nulo.
     */
    @Query(
        value = "SELECT * FROM escuela"
            + " WHERE (CAST(:busqueda AS text) IS NULL"
            + "        OR lower(nombre) LIKE CAST(:busqueda AS text) ESCAPE '\\' OR lower(slug) LIKE CAST(:busqueda AS text) ESCAPE '\\')"
            + "   AND (CAST(:estado AS text) IS NULL"
            + "        OR (CAST(:estado AS text) = 'ACTIVA' AND deleted_at IS NULL)"
            + "        OR (CAST(:estado AS text) = 'INACTIVA' AND deleted_at IS NOT NULL))"
            + " ORDER BY created_at DESC, id DESC",
        countQuery = "SELECT count(*) FROM escuela"
            + " WHERE (CAST(:busqueda AS text) IS NULL"
            + "        OR lower(nombre) LIKE CAST(:busqueda AS text) ESCAPE '\\' OR lower(slug) LIKE CAST(:busqueda AS text) ESCAPE '\\')"
            + "   AND (CAST(:estado AS text) IS NULL"
            + "        OR (CAST(:estado AS text) = 'ACTIVA' AND deleted_at IS NULL)"
            + "        OR (CAST(:estado AS text) = 'INACTIVA' AND deleted_at IS NOT NULL))",
        nativeQuery = true)
    Page<EscuelaModel> listar(@Param("busqueda") String busqueda, @Param("estado") String estado, Pageable pageable);

    /** Pone el borrado lógico si estaba activa. Devuelve las filas cambiadas (0 o 1). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE escuela SET deleted_at = now(), updated_at = now()"
        + " WHERE codigo_escuela = :codigo AND deleted_at IS NULL", nativeQuery = true)
    int desactivar(@Param("codigo") UUID codigo);

    /** Quita el borrado lógico si estaba eliminada. Devuelve las filas cambiadas (0 o 1). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE escuela SET deleted_at = NULL, updated_at = now()"
        + " WHERE codigo_escuela = :codigo AND deleted_at IS NOT NULL", nativeQuery = true)
    int reactivar(@Param("codigo") UUID codigo);
}
