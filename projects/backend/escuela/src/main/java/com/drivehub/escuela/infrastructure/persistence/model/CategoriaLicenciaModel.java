package com.drivehub.escuela.infrastructure.persistence.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de categorías de licencia.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "categoria_licencia")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_categoria_licencia"))
public class CategoriaLicenciaModel extends BaseModel {

    // * A, B, C... Valor que usan los demás servicios (en el diseño original: codigo)
    @Column(name = "sigla", nullable = false, unique = true, length = 3)
    private String sigla;

    @Setter
    @Column(name = "descripcion", nullable = false, length = 100)
    private String descripcion;

    @Setter
    @Column(name = "edad_minima")
    private Short edadMinima;

    // * Mínimo de clases prácticas para rendir el examen
    @Setter
    @Column(name = "clases_minimas")
    private Short clasesMinimas;
}
