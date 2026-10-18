package com.drivehub.escuela.infrastructure.persistence.model;

import java.time.LocalDate;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoEstudiante;

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
 * Perfil del estudiante (CU08).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "estudiante")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_estudiante"))
public class EstudianteModel extends BaseModel {

    // * = Identidad usuario.codigo_usuario (sin FK física, otro servicio)
    @Column(name = "usuario_codigo", nullable = false, unique = true)
    private UUID usuarioCodigo;

    // * Documento de identidad
    @Setter
    @Column(name = "ci", nullable = false, unique = true, length = 15)
    private String ci;

    @Setter
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Setter
    @Column(name = "direccion", length = 200)
    private String direccion;

    // * Categoría a la que aspira
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaLicenciaModel categoria;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoEstudiante estado;

    @Setter
    @Column(name = "fecha_inscripcion")
    private LocalDate fechaInscripcion;

    @Setter
    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;
}
