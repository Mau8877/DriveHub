package com.drivehub.academico.features.examenes.model;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.examenes.enums.EstadoExamen;
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
public class ExamenConduccion extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoExamenConduccion;

    @Indexed
    private UUID estudianteCodigo;

    // * Sigla de categoría
    private String categoria;

    @Setter
    private Instant fechaProgramada;

    // * Instructor o evaluador (persona de Identidad)
    @Setter
    private UUID evaluadorCodigo;

    @Setter
    private UUID vehiculoCodigo;

    // * 1, 2, 3...
    private int intento;

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
