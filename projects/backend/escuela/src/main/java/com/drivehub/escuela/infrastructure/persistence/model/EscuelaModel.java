package com.drivehub.escuela.infrastructure.persistence.model;

import java.util.UUID;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Escuela (tenant de la plataforma).
 *
 * * No tiene columna de estado: está activa mientras `deletedAt` sea nulo
 * * (ver BaseModel). Desactivarla y reactivarla es poner y quitar `deletedAt`.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "escuela")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_escuela"))
public class EscuelaModel extends BaseModel {

    // * Identificador legible del enlace de registro; minúsculas, números y guiones
    @Column(name = "slug", nullable = false, unique = true, length = 60)
    private String slug;

    @Setter
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    public EscuelaModel(UUID codigo, String slug, String nombre) {
        this.slug = slug;
        this.nombre = nombre;
        asignarCodigo(codigo);
    }
}
