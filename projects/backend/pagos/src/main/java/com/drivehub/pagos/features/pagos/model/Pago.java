package com.drivehub.pagos.features.pagos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.common.model.BaseEscuelaModel;
import com.drivehub.pagos.features.pagos.enums.ConceptoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.MetodoPago;
import com.drivehub.pagos.features.planes.model.PlanPago;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * Pago realizado (CU26 a CU31). Hoy es la compra de un plan de horas con tarjeta.
 *
 * * Es de una escuela (multitenant): `escuelaCodigo` sale del token al crearlo y no cambia.
 * * Guarda COPIA de lo comprado (plan, horas, montos, tipo de cambio y nombres): un cambio posterior del
 * * plan o del tipo de cambio no altera un pago ya creado, y el comprobante sale solo de estos datos.
 * * `idempotencyKey` evita cobrar dos veces si el cliente o la pasarela reintentan.
 * * `horasRestantes` son las horas de ESTA compra sin gastar: arranca en `horas` al acreditarse (CU27) y
 * * decide si el pago es reembolsable (CU31).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "pago")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_pago"))
public class Pago extends BaseEscuelaModel {

    // * = codigo de persona de Identidad (usuario.codigo_usuario), sin FK física
    @Column(name = "estudiante_codigo", nullable = false)
    private UUID estudianteCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "concepto", nullable = false, length = 10)
    private ConceptoPago concepto;

    // * Solo si concepto = HORAS
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_pago_id")
    private PlanPago planPago;

    @Column(name = "plan_nombre", length = 80)
    private String planNombre;

    // * Horas compradas, copia del plan al comprar (CHECK de 1 a 500 en la base)
    @Column(name = "horas")
    private Short horas;

    @Setter
    @Column(name = "horas_restantes")
    private Short horasRestantes;

    // * Opcional. codigoReserva de Académico (reservas), sin FK física. Solo CU44
    @Column(name = "reserva_codigo")
    private UUID reservaCodigo;

    // * Lo cobrado, en la moneda de `moneda` (USD)
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    // * Referencia: costo en bolivianos del plan al comprar y el tipo de cambio con que se fijó `monto`
    @Column(name = "monto_bs", precision = 10, scale = 2)
    private BigDecimal montoBs;

    @Column(name = "tipo_cambio", precision = 8, scale = 4)
    private BigDecimal tipoCambio;

    @Column(name = "estudiante_nombre", length = 160)
    private String estudianteNombre;

    @Column(name = "escuela_nombre", length = 120)
    private String escuelaNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = false, length = 15)
    private MetodoPago metodo;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoPago estado;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    // * Opcional. Saga reserva-pago (CU44) a la que pertenece
    @Column(name = "saga_codigo")
    private UUID sagaCodigo;

    // * Vigencia de la página de pago; vencida, el pago pasa a FALLIDO
    @Column(name = "expira_en")
    private Instant expiraEn;

    @Setter
    @Column(name = "confirmado_en")
    private Instant confirmadoEn;

    /**
     * Compra de un plan de horas, pendiente de pago, con todo lo comprado copiado.
     *
     * @param monto cobrado en USD
     */
    public Pago(UUID escuelaCodigo, UUID estudianteCodigo, PlanPago plan, BigDecimal monto, BigDecimal tipoCambio,
            String estudianteNombre, String escuelaNombre, String idempotencyKey, Instant expiraEn) {
        super(escuelaCodigo);
        this.estudianteCodigo = estudianteCodigo;
        this.concepto = ConceptoPago.HORAS;
        this.planPago = plan;
        this.planNombre = plan.getNombre();
        this.horas = plan.getHoras();
        this.monto = monto;
        this.moneda = "USD";
        this.montoBs = plan.getCostoBs();
        this.tipoCambio = tipoCambio;
        this.estudianteNombre = estudianteNombre;
        this.escuelaNombre = escuelaNombre;
        this.metodo = MetodoPago.TARJETA;
        this.estado = EstadoPago.PENDIENTE;
        this.idempotencyKey = idempotencyKey;
        this.expiraEn = expiraEn;
    }
}
