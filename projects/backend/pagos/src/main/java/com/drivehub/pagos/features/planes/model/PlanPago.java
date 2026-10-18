package com.drivehub.pagos.features.planes.model;

import java.math.BigDecimal;

import com.drivehub.pagos.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plan de pagos que ofrece la escuela (CU25).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "plan_pago")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_plan_pago"))
public class PlanPago extends BaseModel {

    @Setter
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    // * Sigla de Escuela categoria_licencia (A, B, C...), sin FK física
    @Setter
    @Column(name = "categoria_sigla", length = 3)
    private String categoriaSigla;

    // * Clases incluidas
    @Setter
    @Column(name = "nro_clases", nullable = false)
    private Short nroClases;

    @Setter
    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Setter
    @Column(name = "nro_cuotas", nullable = false)
    private Short nroCuotas;

    @Setter
    @Column(name = "activo", nullable = false)
    private boolean activo;
}
