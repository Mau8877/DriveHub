package com.drivehub.academico.features.resultados.model;

import java.time.Instant;
import java.util.UUID;

import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vista consolidada de notas (CU23).
 * * Se escribe al cerrar cada evaluación (simulacro, prueba de clase, examen) y publica
 * * examen.calificado. Estudiante, instructor y escuela ven las notas con una sola consulta.
 */
@Getter
@NoArgsConstructor
@Document(collection = "resultados")
@CompoundIndex(name = "idx_resultado_estudiante_fecha", def = "{'estudianteCodigo': 1, 'fecha': -1}")
public class Resultado extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoResultado;

    // * Escuela (tenant) dueña del resultado: la del estudiante, sin relación física
    @Setter
    private UUID escuelaCodigo;

    @Setter
    private UUID estudianteCodigo;

    @Setter
    private OrigenResultado origen;

    // * _id del documento de origen
    @Setter
    private Long origenId;

    @Setter
    private String titulo;

    @Setter
    private double puntaje;

    @Setter
    private boolean aprobado;

    @Setter
    private Instant fecha;

    @Override
    public void inicializarCodigo() {
        if (codigoResultado == null) {
            codigoResultado = UuidV7.generar();
        }
    }
}
