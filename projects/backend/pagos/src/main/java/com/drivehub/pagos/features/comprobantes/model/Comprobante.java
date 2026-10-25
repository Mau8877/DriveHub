package com.drivehub.pagos.features.comprobantes.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.drivehub.pagos.common.model.BaseModel;
import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.ReversionPago;

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
 * Comprobante de una operación del pago (CU30): el de la compra o el del reembolso.
 *
 * * Uno por pago y por tipo (`UNIQUE (pago_id, tipo)`): pedirlo dos veces no genera dos.
 * * El de reembolso va ligado a su reversión.
 * * `monto` es lo que comprueba: lo pagado, o lo devuelto.
 * * `urlPdf` es nulo mientras el PDF está pendiente de generar; el comprobante existe desde que se numera.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "comprobante")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_comprobante"))
public class Comprobante extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoComprobante tipo;

    // * Solo en el comprobante de reembolso
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversion_pago_id")
    private ReversionPago reversionPago;

    // * Correlativo (CMP-2026-000123 en la compra, RMB-2026-000031 en el reembolso)
    @Column(name = "nro_comprobante", nullable = false, unique = true, length = 20)
    private String nroComprobante;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "emitido_en", nullable = false)
    private Instant emitidoEn;

    // * Clave del PDF en el bucket de comprobantes (privado), no una dirección pública
    @Setter
    @Column(name = "url_pdf", columnDefinition = "text")
    private String urlPdf;

    public Comprobante(Pago pago, TipoComprobante tipo, ReversionPago reversionPago, String nroComprobante,
            BigDecimal monto, Instant emitidoEn) {
        this.pago = pago;
        this.tipo = tipo;
        this.reversionPago = reversionPago;
        this.nroComprobante = nroComprobante;
        this.monto = monto;
        this.emitidoEn = emitidoEn;
    }

    /** Si el PDF ya está generado y guardado. */
    public boolean tienePdf() {
        return urlPdf != null;
    }
}
