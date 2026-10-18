package com.drivehub.pagos.features.inscripciones.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.drivehub.pagos.common.model.BaseModel;
import com.drivehub.pagos.features.inscripciones.enums.EstadoCuota;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuota de un plan contratado (CU25, CU28).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "cuota")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_cuota"))
public class Cuota extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_plan_id", nullable = false)
    private InscripcionPlan inscripcionPlan;

    @Column(name = "nro", nullable = false)
    private Short nro;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_vence", nullable = false)
    private LocalDate fechaVence;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoCuota estado;
}
