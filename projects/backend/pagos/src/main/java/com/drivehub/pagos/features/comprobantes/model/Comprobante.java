package com.drivehub.pagos.features.comprobantes.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.drivehub.pagos.common.model.BaseModel;
import com.drivehub.pagos.features.pagos.model.Pago;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Comprobante de pago (CU30).
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

    // * Correlativo
    @Column(name = "nro_comprobante", nullable = false, unique = true, length = 20)
    private String nroComprobante;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "emitido_en", nullable = false)
    private Instant emitidoEn;

    // * Clave del PDF en el bucket drivehub-pagos
    @Setter
    @Column(name = "url_pdf", columnDefinition = "text")
    private String urlPdf;
}
