package com.drivehub.pagos.features.billetera.model;

import java.util.UUID;

import com.drivehub.pagos.common.model.BaseEscuelaModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El saldo de horas de un estudiante en una escuela (CU27). Una por estudiante y escuela; se crea con la
 * primera acreditación.
 *
 * * Es de una escuela (multitenant): `escuelaCodigo` no cambia.
 * * `horasDisponibles` nunca es negativo (CHECK en la base). Las horas que un reembolso en curso apartó (CU31)
 * * ya están descontadas.
 * * `version` es el bloqueo optimista: las operaciones sobre la billetera la leen con bloqueo de escritura, y esta
 * * columna es la última defensa si dos cambios llegaran a pisarse.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "billetera_horas")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_billetera_horas"))
public class BilleteraHoras extends BaseEscuelaModel {

    // * = codigo de persona de Identidad (usuario.codigo_usuario), sin FK física
    @Column(name = "estudiante_codigo", nullable = false, updatable = false)
    private UUID estudianteCodigo;

    @Setter
    @Column(name = "horas_disponibles", nullable = false)
    private int horasDisponibles;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * Billetera nueva, vacía.
     */
    public BilleteraHoras(UUID escuelaCodigo, UUID estudianteCodigo) {
        super(escuelaCodigo);
        this.estudianteCodigo = estudianteCodigo;
        this.horasDisponibles = 0;
    }
}
