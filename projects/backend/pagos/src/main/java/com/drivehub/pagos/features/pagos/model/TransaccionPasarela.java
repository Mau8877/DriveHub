package com.drivehub.pagos.features.pagos.model;

import com.drivehub.pagos.common.model.BaseModel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * Intercambio con la pasarela de pagos (CU26, CU27).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "transaccion_pasarela")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_transaccion_pasarela"))
public class TransaccionPasarela extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Column(name = "proveedor", nullable = false, length = 30)
    private String proveedor;

    // * Id que devuelve la pasarela; evita procesar dos veces el webhook
    @Setter
    @Column(name = "referencia_externa", unique = true, length = 80)
    private String referenciaExterna;

    @Setter
    @Column(name = "qr_payload", columnDefinition = "text")
    private String qrPayload;

    @Setter
    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    // * Respuesta cruda de la pasarela (jsonb), para auditoría
    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "respuesta")
    private String respuesta;
}
