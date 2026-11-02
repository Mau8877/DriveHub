package com.drivehub.academico.features.clases.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.model.CriterioEvaluacion;
import com.drivehub.academico.features.clases.model.PruebaClase;

/**
 * Una prueba de clase (CU18). Sin ids internos.
 *
 * @param codigo el `codigoPruebaClase`
 * @param claseCodigo el `codigoClase` de la clase evaluada
 * @param puntajeTotal sobre 100, con dos decimales
 * @param fecha la de la clase
 */
public record PruebaClaseResponse(
        UUID codigo,
        UUID claseCodigo,
        TipoPrueba tipo,
        List<CriterioEvaluacion> criterios,
        double puntajeTotal,
        ResultadoEvaluacion resultado,
        String observaciones,
        LocalDate fecha) {

    public static PruebaClaseResponse de(PruebaClase prueba, UUID claseCodigo) {
        return new PruebaClaseResponse(prueba.getCodigoPruebaClase(), claseCodigo, prueba.getTipo(),
            prueba.getCriterios(), prueba.getPuntajeTotal(), prueba.getResultado(), prueba.getObservaciones(),
            prueba.getFecha());
    }
}
