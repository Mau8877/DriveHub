package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.drivehub.escuela.infrastructure.persistence.model.VehiculoModel;

public interface VehiculoJpaRepository extends JpaRepository<VehiculoModel, Long> {

    boolean existsByPlaca(String placa);

    Optional<VehiculoModel> findByEscuelaCodigoAndCodigo(UUID escuelaCodigo, UUID codigo);

    /**
     * Listado de una escuela. `busqueda` ya viene como patrón LIKE en minúsculas (o nulo) y
     * `estado` es el nombre del estado (o nulo). SQL nativo para castear los nulos a texto: sin
     * eso, PostgreSQL no puede deducir el tipo de un parámetro nulo.
     */
    @Query(
        value = "SELECT * FROM vehiculo"
            + " WHERE escuela_codigo = :escuela AND deleted_at IS NULL"
            + "   AND (CAST(:estado AS text) IS NULL OR estado = CAST(:estado AS text))"
            + "   AND (CAST(:busqueda AS text) IS NULL"
            + "        OR lower(placa) LIKE CAST(:busqueda AS text) ESCAPE '\\'"
            + "        OR lower(marca) LIKE CAST(:busqueda AS text) ESCAPE '\\'"
            + "        OR lower(modelo) LIKE CAST(:busqueda AS text) ESCAPE '\\')"
            + " ORDER BY created_at DESC, id DESC",
        countQuery = "SELECT count(*) FROM vehiculo"
            + " WHERE escuela_codigo = :escuela AND deleted_at IS NULL"
            + "   AND (CAST(:estado AS text) IS NULL OR estado = CAST(:estado AS text))"
            + "   AND (CAST(:busqueda AS text) IS NULL"
            + "        OR lower(placa) LIKE CAST(:busqueda AS text) ESCAPE '\\'"
            + "        OR lower(marca) LIKE CAST(:busqueda AS text) ESCAPE '\\'"
            + "        OR lower(modelo) LIKE CAST(:busqueda AS text) ESCAPE '\\')",
        nativeQuery = true)
    Page<VehiculoModel> listar(@Param("escuela") UUID escuela, @Param("busqueda") String busqueda,
            @Param("estado") String estado, Pageable pageable);
}
