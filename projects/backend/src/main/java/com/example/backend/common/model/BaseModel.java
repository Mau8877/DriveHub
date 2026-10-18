package com.example.backend.common.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

/**
 * Base para todos los Models del proyecto: id interno + codigo (UUID)
 * publico, createdAt, updatedAt y soft delete via deletedAt.
 *
 * * `id` (Long, IDENTITY) es la PK real de la tabla -- nunca se expone en
 * * un DTO/URL/JWT, es un detalle interno de persistencia. `codigo` (UUID)
 * * es el identificador publico (URLs, DTOs, claims de JWT), generado en
 * * la aplicacion via @PrePersist -- no lo genera la base de datos, y no
 * * es la PK. Ver PERSISTENCIA_BD_BACKEND.md#identificador-de-fila-id-interno--codigo-público-patrón-basemodel.
 * * El @SQLRestriction filtra "deleted_at IS NULL" en TODA query que
 * * Hibernate genere para la entidad (findAll, findById, JPQL, fetch de
 * * relaciones) -- es estatico, no se puede "sacar" en una query puntual.
 * * Si algun modulo necesita ver soft-deleted (ej. una papelera), se le
 * * agrega un @Filter opcional a esa entidad puntual, no se cambia este
 * * mecanismo global.
 * * Getters generados por Lombok (@Getter a nivel de clase). `deletedAt`
 * * es el unico campo con setter (soft delete) -- id/codigo/createdAt/updatedAt
 * * son de solo lectura desde fuera de esta clase.
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
            codigo = UUID.randomUUID();
        }
    }
}
