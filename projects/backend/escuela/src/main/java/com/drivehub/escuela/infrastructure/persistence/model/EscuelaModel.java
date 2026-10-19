package com.drivehub.escuela.infrastructure.persistence.model;

import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoEscuela;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Escuela (tenant de la plataforma).
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

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoEscuela estado;

    public EscuelaModel(UUID codigo, String slug, String nombre, EstadoEscuela estado) {
        this.slug = slug;
        this.nombre = nombre;
        this.estado = estado;
        asignarCodigo(codigo);
    }
}
