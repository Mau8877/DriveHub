package com.drivehub.escuela.infrastructure.persistence.model;

import java.time.LocalDate;
import java.util.UUID;

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
 * Instructor titular de cada estudiante (CU11, CU29). El estudiante es la persona de Identidad (CU08a).
 * * Índice único parcial: un estudiante tiene un solo instructor vigente (fecha_fin NULL).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "asignacion_alumno")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_asignacion_alumno"))
public class AsignacionAlumnoModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private InstructorModel instructor;

    // * = Identidad usuario.codigo_usuario de la persona (sin FK física, otro servicio)
    @Column(name = "estudiante_codigo", nullable = false)
    private UUID estudianteCodigo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    // * NULL mientras esté vigente
    @Setter
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
}
