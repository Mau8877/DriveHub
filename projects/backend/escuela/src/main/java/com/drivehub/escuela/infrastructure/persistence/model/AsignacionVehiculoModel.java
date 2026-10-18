package com.drivehub.escuela.infrastructure.persistence.model;

import java.time.LocalDate;

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
 * Auto asignado a un instructor (CU10).
 * * Índice único parcial: un auto tiene una sola asignación vigente (fecha_fin NULL).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "asignacion_vehiculo")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_asignacion_vehiculo"))
public class AsignacionVehiculoModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private VehiculoModel vehiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private InstructorModel instructor;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    // * NULL mientras la asignación esté vigente
    @Setter
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
}
