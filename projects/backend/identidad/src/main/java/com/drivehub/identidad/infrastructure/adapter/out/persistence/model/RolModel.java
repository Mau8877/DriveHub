package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rol del sistema. Los nombres son datos, no un enum.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "rol")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_rol"))
public class RolModel extends BaseModel {

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    @Setter
    @Column(name = "descripcion", length = 120)
    private String descripcion;
}
