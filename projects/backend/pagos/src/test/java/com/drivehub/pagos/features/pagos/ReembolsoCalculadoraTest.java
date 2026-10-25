package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.features.pagos.dto.ReembolsoPrevisto;
import com.drivehub.pagos.features.pagos.service.ReembolsoCalculadora;

class ReembolsoCalculadoraTest {

    private static ReembolsoPrevisto calcular(int retencion, String monto) {
        return new ReembolsoCalculadora(PagosPropertiesPrueba.conRetencion(retencion)).calcular(new BigDecimal(monto));
    }

    @Test
    void con5PorCiento_de100_devuelve95_yRetiene5() {
        ReembolsoPrevisto previsto = calcular(5, "100.00");

        assertThat(previsto.montoDevuelto()).isEqualByComparingTo("95.00");
        assertThat(previsto.montoRetenido()).isEqualByComparingTo("5.00");
        assertThat(previsto.porcentajeRetenido()).isEqualTo(5);
    }

    @Test
    void elRedondeoNoPierdeCentavos_losDosMontosSumanLoPagado() {
        ReembolsoPrevisto previsto = calcular(5, "7.13");

        assertThat(previsto.montoDevuelto()).isEqualByComparingTo("6.77");
        assertThat(previsto.montoRetenido()).isEqualByComparingTo("0.36");
        assertThat(previsto.montoDevuelto().add(previsto.montoRetenido())).isEqualByComparingTo("7.13");
    }

    @Test
    void sinRetencion_devuelveTodo() {
        ReembolsoPrevisto previsto = calcular(0, "38.23");

        assertThat(previsto.montoDevuelto()).isEqualByComparingTo("38.23");
        assertThat(previsto.montoRetenido()).isEqualByComparingTo("0.00");
    }

    @Test
    void conLaRetencionMaxima_devuelveLaMitad() {
        ReembolsoPrevisto previsto = calcular(50, "10.01");

        assertThat(previsto.montoDevuelto().add(previsto.montoRetenido())).isEqualByComparingTo("10.01");
        assertThat(previsto.porcentajeRetenido()).isEqualTo(50);
    }
}
