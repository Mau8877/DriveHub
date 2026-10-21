package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

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
 * Permisos de cada rol (sin caso de uso asignado todavía).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "rol_permiso")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_rol_permiso"))
public class RolPermisoModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolModel rol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permiso_id", nullable = false)
    private PermisoModel permiso;
}
