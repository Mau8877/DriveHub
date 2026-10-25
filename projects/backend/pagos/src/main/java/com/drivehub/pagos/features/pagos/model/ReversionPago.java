package com.drivehub.pagos.features.pagos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.common.model.BaseModel;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.enums.OrigenReversion;

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
 * Devolución del cobro de un pago (CU31): la compensación automática de la saga o el reembolso que pide el
 * estudiante.
 *
 * * Un pago tiene a lo sumo UNA reversión vigente (solicitada o ejecutada): lo garantiza un índice único parcial.
 * * `monto` es lo que se devuelve y `montoRetenido` lo que se queda la escuela (cero en una compensación).
 * * `motivo` guarda el CÓDIGO (`MotivoReembolso` o `MotivoCompensacion`); `motivoDetalle` es lo que escribe el
 * * estudiante cuando elige `OTRO`.
 * * `referenciaExterna` es la devolución en la pasarela; es nula hasta que la pasarela la crea.
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

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 10)
    private OrigenReversion origen;

    @Column(name = "motivo", nullable = false, length = 30)
    private String motivo;

    @Column(name = "motivo_detalle", length = 200)
    private String motivoDetalle;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_retenido", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoRetenido;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoReversion estado;

    @Setter
    @Column(name = "referencia_externa", length = 80)
    private String referenciaExterna;

    @Column(name = "saga_codigo")
    private UUID sagaCodigo;

    @Setter
    @Column(name = "ejecutado_en")
    private Instant ejecutadoEn;

    /**
     * Una reversión nueva, recién solicitada y sin devolución en la pasarela todavía.
     */
    public ReversionPago(Pago pago, OrigenReversion origen, String motivo, String motivoDetalle, BigDecimal monto,
            BigDecimal montoRetenido) {
        this.pago = pago;
        this.origen = origen;
        this.motivo = motivo;
        this.motivoDetalle = motivoDetalle;
        this.monto = monto;
        this.montoRetenido = montoRetenido;
        this.estado = EstadoReversion.SOLICITADA;
    }
}
