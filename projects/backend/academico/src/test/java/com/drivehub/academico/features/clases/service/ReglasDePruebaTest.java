package com.drivehub.academico.features.clases.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.clases.dto.PuntajeDeCriterio;
import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.model.CriterioEvaluacion;

class ReglasDePruebaTest {

    private static List<PuntajeDeCriterio> todosEn(TipoPrueba tipo, double puntaje) {
        return new ArrayList<>(CatalogoDePruebas.de(tipo).criterios().stream()
            .map(c -> new PuntajeDeCriterio(c.codigo(), puntaje)).toList());
    }

    private static void assertInvalido(List<PuntajeDeCriterio> enviados) {
        assertThatThrownBy(() -> ReglasDePrueba.validarYArmarCriterios(TipoPrueba.MANEJO, enviados))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_41"));
    }

    @Test
    void criteriosCompletos_sedevuelvenEnElOrdenDelCatalogoConDescripcionYMaximo() {
        List<PuntajeDeCriterio> enviados = todosEn(TipoPrueba.MANEJO, 8);
        java.util.Collections.reverse(enviados);

        List<CriterioEvaluacion> criterios = ReglasDePrueba.validarYArmarCriterios(TipoPrueba.MANEJO, enviados);

        assertThat(criterios).extracting(CriterioEvaluacion::codigo)
            .containsExactly("espejos", "volante", "pedales_y_cambios", "senales", "distancia_y_velocidad");
        assertThat(criterios.get(0)).isEqualTo(
            new CriterioEvaluacion("espejos", "Posición y uso de espejos", 8, 10));
    }

    @Test
    void faltaUnCriterio_esInvalido() {
        List<PuntajeDeCriterio> enviados = todosEn(TipoPrueba.MANEJO, 5);
        enviados.remove(0);
        assertInvalido(enviados);
    }

    @Test
    void sobraUnCriterio_esInvalido() {
        List<PuntajeDeCriterio> enviados = todosEn(TipoPrueba.MANEJO, 5);
        enviados.add(new PuntajeDeCriterio("inventado", 5.0));
        assertInvalido(enviados);
    }

    @Test
    void criterioRepetido_esInvalido() {
        List<PuntajeDeCriterio> enviados = todosEn(TipoPrueba.MANEJO, 5);
        enviados.set(1, new PuntajeDeCriterio("espejos", 5.0));
        assertInvalido(enviados);
    }

    @Test
    void criteriosDeOtroTipo_sonInvalidos() {
        assertInvalido(todosEn(TipoPrueba.CIUDAD, 5));
    }

    @Test
    void listaVacia_esInvalida() {
        assertInvalido(new ArrayList<>());
    }

    @Test
    void puntajesFueraDeRangoOQueNoSonMediosPuntos_sonInvalidos() {
        for (double malo : new double[] {-1, -0.5, 10.5, 11, 7.3, Double.NaN}) {
            List<PuntajeDeCriterio> enviados = todosEn(TipoPrueba.MANEJO, 5);
            enviados.set(2, new PuntajeDeCriterio(enviados.get(2).codigo(), malo));
            assertInvalido(enviados);
        }
    }

    @Test
    void puntajesLimiteYMediosPuntos_sonValidos() {
        for (double bueno : new double[] {0, 0.5, 9.5, 10}) {
            assertThat(ReglasDePrueba.validarYArmarCriterios(TipoPrueba.MANEJO, todosEn(TipoPrueba.MANEJO, bueno)))
                .hasSize(5);
        }
    }

    private static List<CriterioEvaluacion> conPuntajes(double... puntajes) {
        List<CriterioEvaluacion> criterios = new ArrayList<>();
        for (int i = 0; i < puntajes.length; i++) {
            criterios.add(new CriterioEvaluacion("c" + i, "Criterio " + i, puntajes[i], 10));
        }
        return criterios;
    }

    @Test
    void total_todoEnDiezEs100_todoEnCeroEs0_todoEnSieteEs70() {
        assertThat(ReglasDePrueba.calcularTotal(conPuntajes(10, 10, 10, 10, 10))).isEqualTo(100.0);
        assertThat(ReglasDePrueba.calcularTotal(conPuntajes(0, 0, 0, 0, 0))).isEqualTo(0.0);
        assertThat(ReglasDePrueba.calcularTotal(conPuntajes(7, 7, 7, 7, 7))).isEqualTo(70.0);
    }

    @Test
    void total_sobreCienConDosDecimales() {
        // * 3 criterios: 7 + 7 + 7,5 = 21,5 de 30 = 71,666... -> 71,67
        assertThat(ReglasDePrueba.calcularTotal(conPuntajes(7, 7, 7.5))).isEqualTo(71.67);
    }

    @Test
    void total_sinCriteriosEsCero() {
        assertThat(ReglasDePrueba.calcularTotal(List.of())).isEqualTo(0.0);
    }

    @Test
    void resultado_apruebaDesdeSetentaYReprobaPorDebajo() {
        assertThat(ReglasDePrueba.resultadoDe(70)).isEqualTo(ResultadoEvaluacion.APROBADO);
        assertThat(ReglasDePrueba.resultadoDe(100)).isEqualTo(ResultadoEvaluacion.APROBADO);
        assertThat(ReglasDePrueba.resultadoDe(69.99)).isEqualTo(ResultadoEvaluacion.REPROBADO);
        assertThat(ReglasDePrueba.resultadoDe(0)).isEqualTo(ResultadoEvaluacion.REPROBADO);
    }

    @Test
    void observaciones_enBlancoSonNuloYSeRecortan() {
        assertThat(ReglasDePrueba.limpiarObservaciones(null)).isNull();
        assertThat(ReglasDePrueba.limpiarObservaciones("   ")).isNull();
        assertThat(ReglasDePrueba.limpiarObservaciones("  Muy bien  ")).isEqualTo("Muy bien");
    }
}
