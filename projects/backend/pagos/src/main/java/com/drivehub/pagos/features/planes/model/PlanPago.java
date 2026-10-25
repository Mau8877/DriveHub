package com.drivehub.pagos.features.planes.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.drivehub.pagos.common.model.BaseEscuelaModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plan de horas que ofrece una escuela (CU25a): un nombre, una cantidad de horas y un costo en bolivianos.
 *
 * * El costo en dólares NO se guarda: lo calcula el servicio con el tipo de cambio configurado.
 * * Se desactiva (`activo = false`), no se borra: conserva su historia.
 * * El nombre es único dentro de la escuela sin distinguir mayúsculas (índice único en la base).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "plan_pago")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_plan_pago"))
public class PlanPago extends BaseEscuelaModel {

    @Setter
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    // * Entre 1 y 500 (CHECK en la base). Solo enteros
    @Setter
    @Column(name = "horas", nullable = false)
    private Short horas;

    // * Bolivianos, hasta 99 999 999,99 y mayor a 0 (CHECK en la base)
    @Setter
    @Column(name = "costo_bs", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoBs;

    @Setter
    @Column(name = "activo", nullable = false)
    private boolean activo;

    /**
     * Plan nuevo, activo, de la escuela dada.
     */
    public PlanPago(UUID escuelaCodigo, String nombre, short horas, BigDecimal costoBs) {
        super(escuelaCodigo);
        this.nombre = nombre;
        this.horas = horas;
        this.costoBs = costoBs;
        this.activo = true;
    }
}
