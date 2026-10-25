package com.drivehub.pagos.features.planes;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.features.planes.service.ConversionMoneda;

class ConversionMonedaTest {

    private static ConversionMoneda conCambio(String tipoCambio) {
        return new ConversionMoneda(PagosPropertiesPrueba.con(tipoCambio));
    }

    @Test
    void aDolares_dividePorElTipoDeCambio() {
        assertThat(conCambio("11.77").aDolares(new BigDecimal("1177.00"))).isEqualByComparingTo("100.00");
    }

    @Test
    void aDolares_redondeaADosDecimales() {
        assertThat(conCambio("11.77").aDolares(new BigDecimal("100.00"))).isEqualByComparingTo("8.50");
        assertThat(conCambio("11.77").aDolares(new BigDecimal("100.00")).scale()).isEqualTo(2);
    }

    @Test
    void aDolares_elMedioCentavoSube() {
        // * 1,00 / 8 = 0,125 -> 0,13 (HALF_UP); 1,00 / 16 = 0,0625 -> 0,06
        assertThat(conCambio("8").aDolares(new BigDecimal("1.00"))).isEqualByComparingTo("0.13");
        assertThat(conCambio("16").aDolares(new BigDecimal("1.00"))).isEqualByComparingTo("0.06");
    }

    @Test
    void aDolares_conOtroTipoDeCambioCambiaElResultado() {
        BigDecimal costo = new BigDecimal("450.00");

        assertThat(conCambio("11.77").aDolares(costo)).isEqualByComparingTo("38.23");
        assertThat(conCambio("10.00").aDolares(costo)).isEqualByComparingTo("45.00");
    }

    @Test
    void aDolares_unCostoConCentavos() {
        assertThat(conCambio("11.77").aDolares(new BigDecimal("0.01"))).isEqualByComparingTo("0.00");
        assertThat(conCambio("11.77").aDolares(new BigDecimal("1250.50"))).isEqualByComparingTo("106.24");
    }
}
