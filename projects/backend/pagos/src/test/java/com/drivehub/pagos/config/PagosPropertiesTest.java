package com.drivehub.pagos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Validation;

class PagosPropertiesTest {

    private static PagosProperties enlazar(Map<String, String> propiedades) {
        return new Binder(new MapConfigurationPropertySource(propiedades))
            .bindOrCreate("app.pagos", PagosProperties.class);
    }

    private static boolean esValido(BigDecimal tipoCambio) {
        var validador = Validation.buildDefaultValidatorFactory().getValidator();
        return validador.validate(new PagosProperties(tipoCambio)).isEmpty();
    }

    @Test
    void sinConfigurar_elTipoDeCambioEs696() {
        assertThat(enlazar(Map.of()).tipoCambioBsUsd()).isEqualByComparingTo("6.96");
    }

    @Test
    void conUnValorPropio_loUsa() {
        assertThat(enlazar(Map.of("app.pagos.tipo-cambio-bs-usd", "9.50")).tipoCambioBsUsd())
            .isEqualByComparingTo("9.50");
    }

    @Test
    void cero_unNegativoOUnNulo_noSonValidos_yElPositivoSi() {
        assertThat(esValido(BigDecimal.ZERO)).isFalse();
        assertThat(esValido(new BigDecimal("-1"))).isFalse();
        assertThat(esValido(null)).isFalse();
        assertThat(esValido(new BigDecimal("6.96"))).isTrue();
    }

    @Test
    void estaMarcadaParaValidarse_asiElServicioNoArrancaConUnValorInvalido() {
        assertThat(PagosProperties.class.isAnnotationPresent(Validated.class)).isTrue();
    }
}
