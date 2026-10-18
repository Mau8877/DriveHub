package com.drivehub.escuela.infrastructure.persistence.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Categorías que puede enseñar cada instructor (CU09).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "instructor_categoria")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_instructor_categoria"))
public class InstructorCategoriaModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private InstructorModel instructor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaLicenciaModel categoria;
}
