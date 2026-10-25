package com.drivehub.pagos.features.inscripciones.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.pagos.common.model.BaseModel;
import com.drivehub.pagos.features.inscripciones.enums.EstadoInscripcion;
import com.drivehub.pagos.features.planes.model.PlanPago;

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
 * Plan contratado por un estudiante (CU28 y la contratación del plan).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "inscripcion_plan")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_inscripcion_plan"))
public class InscripcionPlan extends BaseModel {

    // * = codigo de persona de Identidad (usuario.codigo_usuario), sin FK física
    @Column(name = "estudiante_codigo", nullable = false)
    private UUID estudianteCodigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_pago_id", nullable = false)
    private PlanPago planPago;

    // * Copia del monto al contratar: el plan puede cambiar después
    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Setter
    @Column(name = "saldo", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldo;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoInscripcion estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;
}
