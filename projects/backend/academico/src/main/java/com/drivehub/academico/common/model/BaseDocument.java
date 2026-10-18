package com.drivehub.academico.common.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Getter;
import lombok.Setter;

/**
 * Base de todos los Models (documentos) de MongoDB del servicio.
 *
 * * `id` es un Long secuencial (no un ObjectId): mismo criterio que la PK
 * * interna de los servicios PostgreSQL. Lo asigna AsignarIdentificadoresCallback
 * * antes del primer guardado, a partir de la colección `contadores`
 * * (ver SecuenciaService). Nunca sale del servicio.
 * * El identificador público es el campo codigo{Coleccion} (UUID v7) de cada
 * * documento concreto, que se inicializa en inicializarCodigo().
 * ! MongoDB no tiene un equivalente a @SQLRestriction: el soft delete
 * ! (deletedAt) hay que filtrarlo explícitamente en cada query del Repository.
 */
@Getter
public abstract class BaseDocument {

    @Id
    @Setter
    private Long id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Setter
    private Instant deletedAt;

    /**
     * Genera el codigo público (UUID v7) si todavía no tiene uno.
     */
    public abstract void inicializarCodigo();
}
