package com.drivehub.pagos.features.billetera.model;

import java.util.UUID;

import com.drivehub.pagos.common.model.BaseModel;
import com.drivehub.pagos.features.billetera.enums.TipoMovimiento;
import com.drivehub.pagos.features.pagos.model.Pago;

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

/**
 * Una fila del libro de la billetera (CU27): mueve horas de UNA compra. Un consumo que cruza dos compras
 * genera dos filas. No se modifica ni se borra: un error se corrige con otro movimiento.
 *
 * * `horas` es siempre positivo; el `tipo` dice si suma o resta.
 * * `instructorCodigo` y `claseCodigo` solo van en `CONSUMO` (los manda Académico) y en su `DEVOLUCION`
 * * (copiados del consumo): de ahí sale cuántas horas gastó cada alumno con cada instructor (CU29).
 * * `idempotencyKey` con el pago es único: un reintento no acredita ni descuenta dos veces.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "movimiento_horas")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_movimiento_horas"))
public class MovimientoHoras extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billetera_horas_id", nullable = false, updatable = false)
    private BilleteraHoras billeteraHoras;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false, updatable = false)
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false, length = 13)
    private TipoMovimiento tipo;

    @Column(name = "horas", nullable = false, updatable = false)
    private short horas;

    @Column(name = "instructor_codigo", updatable = false)
    private UUID instructorCodigo;

    @Column(name = "clase_codigo", updatable = false)
    private UUID claseCodigo;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    public MovimientoHoras(BilleteraHoras billeteraHoras, Pago pago, TipoMovimiento tipo, int horas,
            UUID instructorCodigo, UUID claseCodigo, String idempotencyKey) {
        this.billeteraHoras = billeteraHoras;
        this.pago = pago;
        this.tipo = tipo;
        this.horas = (short) horas;
        this.instructorCodigo = instructorCodigo;
        this.claseCodigo = claseCodigo;
        this.idempotencyKey = idempotencyKey;
    }
}
