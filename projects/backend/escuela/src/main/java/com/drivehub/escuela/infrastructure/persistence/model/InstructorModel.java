package com.drivehub.escuela.infrastructure.persistence.model;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoInstructor;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ficha profesional del docente (CU09).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "instructor")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_instructor"))
public class InstructorModel extends BaseModel {

    // * = Identidad usuario.codigo_usuario (sin FK física, otro servicio)
    @Column(name = "usuario_codigo", nullable = false, unique = true, updatable = false)
    private UUID usuarioCodigo;

    // * Escuela (tenant) de la ficha, sin FK física: la ficha no cambia de escuela
    @Column(name = "escuela_codigo", nullable = false, updatable = false)
    private UUID escuelaCodigo;

    @Setter
    @Column(name = "nro_licencia", nullable = false, unique = true, length = 30)
    private String nroLicencia;

    // * Alerta antes del vencimiento
    @Setter
    @Column(name = "licencia_vence")
    private LocalDate licenciaVence;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoInstructor estado;

    @Setter
    @Column(name = "fecha_contratacion")
    private LocalDate fechaContratacion;

    /** El `codigo` lo genera el caso de uso: la fila nace con el código que ya tiene la entidad. */
    public InstructorModel(UUID codigo, UUID usuarioCodigo, UUID escuelaCodigo, String nroLicencia,
            LocalDate licenciaVence, EstadoInstructor estado, LocalDate fechaContratacion) {
        if (codigo != null) {
            asignarCodigo(codigo);
        }
        this.usuarioCodigo = usuarioCodigo;
        this.escuelaCodigo = escuelaCodigo;
        this.nroLicencia = nroLicencia;
        this.licenciaVence = licenciaVence;
        this.estado = estado;
        this.fechaContratacion = fechaContratacion;
    }
}
