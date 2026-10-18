package com.drivehub.escuela.infrastructure.persistence.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import com.drivehub.escuela.common.util.UuidV7;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

/**
 * Base de todos los Models JPA del servicio: id interno + codigo público,
 * createdAt, updatedAt y soft delete vía deletedAt.
 *
 * * `id` (Long, IDENTITY) es la PK real -- nunca sale de la capa de
 * * persistencia (ni DTOs, ni URLs, ni JWT, ni eventos a otros servicios).
 * * `codigo` (UUID v7, ver UuidV7) es el identificador público. Su columna
 * * se llama codigo_{tabla} en cada tabla: cada Model concreto la fija con
 * * @AttributeOverride(name = "codigo", column = @Column(name = "codigo_xxx")).
 * * Ver .claude/rules/backend/PERSISTENCIA_BD_BACKEND.md.
 * * @SQLRestriction filtra "deleted_at IS NULL" en TODA query que Hibernate
 * * genere para la entidad (findAll, JPQL, fetch de relaciones).
 */
@Getter
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
public abstract class BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "codigo", updatable = false, nullable = false, unique = true)
    private UUID codigo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Setter
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void generarCodigoSiFalta() {
        if (codigo == null) {
            codigo = UuidV7.generar();
        }
    }
}
