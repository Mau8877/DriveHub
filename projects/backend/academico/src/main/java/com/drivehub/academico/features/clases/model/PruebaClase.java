package com.drivehub.academico.features.clases.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.model.BaseDocument;
import com.drivehub.academico.common.util.UuidV7;
import com.drivehub.academico.features.clases.enums.TipoPrueba;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evaluación práctica durante la clase (CU18).
 */
@Getter
@NoArgsConstructor
@Document(collection = "pruebas_clase")
// * Una prueba por tipo en cada clase: dos peticiones a la vez no pueden duplicarla. Cubre también la búsqueda por clase
@CompoundIndex(name = "uq_prueba_clase_tipo", def = "{'claseId': 1, 'tipo': 1}", unique = true)
public class PruebaClase extends BaseDocument {

    // * Identificador público (UUID v7). El _id Long es interno, ver BaseDocument.
    @Indexed(unique = true)
    private UUID codigoPruebaClase;

    // * _id de clases
    private Long claseId;

    @Indexed
    private UUID estudianteCodigo;

    private UUID instructorCodigo;

    private TipoPrueba tipo;

    private List<CriterioEvaluacion> criterios;

    private double puntajeTotal;

    private ResultadoEvaluacion resultado;

    // * Opcional
    @Setter
    private String observaciones;

    private LocalDate fecha;

    /** Una prueba que se registra (CU18). Una vez guardada no se corrige. */
    public PruebaClase(Long claseId, UUID estudianteCodigo, UUID instructorCodigo, TipoPrueba tipo,
            List<CriterioEvaluacion> criterios, double puntajeTotal, ResultadoEvaluacion resultado,
            String observaciones, LocalDate fecha) {
        this.claseId = claseId;
        this.estudianteCodigo = estudianteCodigo;
        this.instructorCodigo = instructorCodigo;
        this.tipo = tipo;
        this.criterios = criterios;
        this.puntajeTotal = puntajeTotal;
        this.resultado = resultado;
        this.observaciones = observaciones;
        this.fecha = fecha;
    }

    @Override
    public void inicializarCodigo() {
        if (codigoPruebaClase == null) {
            codigoPruebaClase = UuidV7.generar();
        }
    }
}
