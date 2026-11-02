package com.drivehub.academico.features.clases.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.clases.dto.PuntajeDeCriterio;
import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.model.CriterioEvaluacion;
import com.drivehub.academico.features.clases.service.CatalogoDePruebas.CriterioDelCatalogo;

/**
 * Las reglas de una prueba de clase (CU18): que los criterios enviados sean exactamente los del tipo y estén bien
 * puntuados, el total sobre 100 y el resultado. El tamaño de las observaciones lo comprueba Bean Validation
 * (`ERR_SYS_01`); lo demás es `ERR_ACA_41`.
 */
public final class ReglasDePrueba {

    /** El total desde el cual la prueba queda aprobada; el mismo corte del examen de conducción. */
    public static final double PUNTAJE_APROBACION = 70;

    private static final double PASO = 0.5;

    private ReglasDePrueba() {
    }

    /**
     * Valida los puntajes contra el catálogo del tipo y los devuelve en el orden del catálogo, con la descripción y
     * el máximo copiados de él.
     *
     * @throws ValidationException `ERR_ACA_41` si falta un criterio, sobra uno, se repite o es de otro tipo, o si un
     *         puntaje está fuera de 0 al máximo o no es múltiplo de 0,5
     */
    public static List<CriterioEvaluacion> validarYArmarCriterios(TipoPrueba tipo, List<PuntajeDeCriterio> enviados) {
        List<CriterioDelCatalogo> esperados = CatalogoDePruebas.de(tipo).criterios();
        Map<String, Double> porCodigo = new HashMap<>();
        for (PuntajeDeCriterio enviado : enviados) {
            if (porCodigo.put(enviado.codigo(), enviado.puntaje()) != null) {
                throw invalido("Un criterio está repetido: " + enviado.codigo());
            }
        }
        for (String codigo : porCodigo.keySet()) {
            if (esperados.stream().noneMatch(c -> c.codigo().equals(codigo))) {
                throw invalido("Ese criterio no es de esta prueba: " + codigo);
            }
        }
        List<CriterioEvaluacion> criterios = new ArrayList<>();
        for (CriterioDelCatalogo esperado : esperados) {
            Double puntaje = porCodigo.get(esperado.codigo());
            if (puntaje == null) {
                throw invalido("Falta puntuar: " + esperado.descripcion());
            }
            if (puntaje.isNaN() || puntaje < 0 || puntaje > esperado.maximo() || !esMultiploDelPaso(puntaje)) {
                throw invalido("El puntaje de «" + esperado.descripcion() + "» debe ser de 0 a "
                    + (int) esperado.maximo() + ", de medio en medio punto");
            }
            criterios.add(new CriterioEvaluacion(esperado.codigo(), esperado.descripcion(), puntaje,
                esperado.maximo()));
        }
        return criterios;
    }

    /** La suma de lo obtenido sobre la suma de los máximos, sobre 100 y con dos decimales. */
    public static double calcularTotal(List<CriterioEvaluacion> criterios) {
        double obtenido = criterios.stream().mapToDouble(CriterioEvaluacion::puntaje).sum();
        double maximo = criterios.stream().mapToDouble(CriterioEvaluacion::maximo).sum();
        if (maximo == 0) {
            return 0;
        }
        return BigDecimal.valueOf(obtenido * 100 / maximo).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static ResultadoEvaluacion resultadoDe(double total) {
        return total >= PUNTAJE_APROBACION ? ResultadoEvaluacion.APROBADO : ResultadoEvaluacion.REPROBADO;
    }

    /** Recorta los espacios y devuelve {@code null} si queda en blanco. */
    public static String limpiarObservaciones(String texto) {
        return ReglasDeClase.limpiar(texto);
    }

    private static boolean esMultiploDelPaso(double puntaje) {
        double pasos = puntaje / PASO;
        return Math.abs(pasos - Math.rint(pasos)) < 1e-9;
    }

    private static ValidationException invalido(String mensaje) {
        return new ValidationException(AcademicoErrorCodes.DATOS_DE_PRUEBA_INVALIDOS, mensaje);
    }
}
