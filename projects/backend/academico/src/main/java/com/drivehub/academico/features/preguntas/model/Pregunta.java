package com.drivehub.academico.features.preguntas.model;

import java.util.List;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pregunta del banco de los simulacros (CU19).
 */
@Getter
@NoArgsConstructor
@Document(collection = "banco_preguntas")
// * Escuela primero: todo el banco se consulta dentro de una escuela (CU19) y CU20 arma el simulacro
// * sacando preguntas por categoría y tema de la escuela
@CompoundIndex(name = "idx_pregunta_escuela_categoria_tema_activa",
        def = "{'escuelaCodigo': 1, 'categoria': 1, 'tema': 1, 'activa': 1}")
public class Pregunta extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoPregunta;

    // * Escuela (tenant) dueña de la pregunta: el claim `escuela` del JWT, sin relación física
    @Setter
    private UUID escuelaCodigo;

    @Setter
    private String categoria;

    // * Ej: señales, prioridad de paso
    @Setter
    private String tema;

    @Setter
    private TipoPregunta tipo;

    @Setter
    private String enunciado;

    // * Opcional
    @Setter
    private String imagenUrl;

    @Setter
    private List<OpcionPregunta> opciones;

    // * Se muestra al corregir
    @Setter
    private String explicacion;

    // * 1 a 3
    @Setter
    private int dificultad;

    @Setter
    private boolean activa;

    // * Persona de Identidad que la creó
    @Setter
    private UUID creadaPorCodigo;

    @Override
    public void inicializarCodigo() {
        if (codigoPregunta == null) {
            codigoPregunta = UuidV7.generar();
        }
    }
}
