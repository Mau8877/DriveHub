package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import java.time.Instant;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Roles de cada usuario (CU01, CU05). Entidad propia, no @ManyToMany.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "usuario_rol")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario_rol"))
public class UsuarioRolModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioModel usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolModel rol;

    @Column(name = "asignado_en", nullable = false)
    private Instant asignadoEn;

    public UsuarioRolModel(UsuarioModel usuario, RolModel rol, Instant asignadoEn) {
        this.usuario = usuario;
        this.rol = rol;
        this.asignadoEn = asignadoEn;
    }
}
