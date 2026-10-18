package com.drivehub.pagos.features.pagos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.drivehub.pagos.common.model.BaseModel;
import com.drivehub.pagos.features.inscripciones.model.Cuota;
import com.drivehub.pagos.features.pagos.enums.ConceptoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.MetodoPago;
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
 * Pago realizado (CU26, CU27, CU28, CU29).
 * * idempotency_key evita cobrar dos veces si el cliente o la pasarela reintentan.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "pago")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_pago"))
public class Pago extends BaseModel {

    // * = codigo de persona de Identidad (usuario.codigo_usuario), sin FK física
    @Column(name = "estudiante_codigo", nullable = false)
    private UUID estudianteCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "concepto", nullable = false, length = 10)
    private ConceptoPago concepto;

    // * Opcional: solo si concepto = CUOTA
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuota_id")
    private Cuota cuota;

    // * Opcional. codigoReserva de Académico (reservas), sin FK física
    @Column(name = "reserva_codigo")
    private UUID reservaCodigo;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    // * BOB por defecto
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

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

    @Setter
    @Column(name = "confirmado_en")
    private Instant confirmadoEn;
}
