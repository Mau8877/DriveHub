package com.drivehub.academico.features.examenes.model;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.examenes.enums.EstadoExamen;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Examen oficial de conducción (CU22).
 */
@Getter
@NoArgsConstructor
@Document(collection = "examenes_conduccion")
// * Escuela primero: los exámenes se consultan siempre dentro de una escuela (CU22); el estudiante y la categoría
// * sirven para calcular el intento y para evitar dos exámenes programados a la vez
@CompoundIndex(name = "idx_examen_escuela_estudiante_categoria",
        def = "{'escuelaCodigo': 1, 'estudianteCodigo': 1, 'categoria': 1}")
public class ExamenConduccion extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoExamenConduccion;

    // * Escuela (tenant) dueña del examen: el claim `escuela` del JWT, sin relación física
    @Setter
    private UUID escuelaCodigo;

    @Indexed
    @Setter
    private UUID estudianteCodigo;

    // * Sigla de categoría
    @Setter
    private String categoria;

    @Setter
    private Instant fechaProgramada;

    // * Instructor o evaluador (persona de Identidad)
    @Setter
    private UUID evaluadorCodigo;

    @Setter
    private UUID vehiculoCodigo;

    // * 1, 2, 3...
    @Setter
    private int intento;

    // * Persona de Identidad que lo programó
    @Setter
    private UUID creadoPorCodigo;

    @Setter
    private EstadoExamen estado;

    // * Opcional
    @Setter
    private Double puntaje;

    // * Opcional
    @Setter
    private ResultadoEvaluacion resultado;

    // * Opcional
    @Setter
    private String observaciones;

    @Override
    public void inicializarCodigo() {
        if (codigoExamenConduccion == null) {
            codigoExamenConduccion = UuidV7.generar();
        }
    }
}
