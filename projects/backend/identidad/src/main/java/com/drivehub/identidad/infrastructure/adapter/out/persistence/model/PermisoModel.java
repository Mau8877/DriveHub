package com.drivehub.identidad.infrastructure.adapter.out.persistence.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Permiso fino (CU05).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "permiso")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_permiso"))
public class PermisoModel extends BaseModel {

    // * Ej: reserva.crear, pago.revertir (en el diseño original: "codigo")
    @Column(name = "clave", nullable = false, unique = true, length = 60)
    private String clave;

    @Setter
    @Column(name = "descripcion", length = 120)
    private String descripcion;
}
