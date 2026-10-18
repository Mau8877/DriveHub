package com.drivehub.pagos.features.pagos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.common.model.BaseModel;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;

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
 * Compensación de un pago dentro de la saga (CU31).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "reversion_pago")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_reversion_pago"))
public class ReversionPago extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Setter
    @Column(name = "motivo", length = 200)
    private String motivo;

    @Setter
    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoReversion estado;

    @Column(name = "saga_codigo")
    private UUID sagaCodigo;

    @Setter
    @Column(name = "ejecutado_en")
    private Instant ejecutadoEn;
}
