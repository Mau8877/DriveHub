package com.drivehub.pagos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.validation.annotation.Validated;

import com.drivehub.pagos.PagosPropertiesPrueba;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * La configuración de Pagos: lo que sale solo de variables de entorno (tipo de cambio y direcciones de retorno)
 * no tiene valor por defecto, y sin ello el servicio no arranca.
 */
class PagosPropertiesTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    /** Lo mínimo obligatorio: lo que en producción aportan las variables de entorno. */
    private static Map<String, String> obligatorias() {
        Map<String, String> propiedades = new HashMap<>();
        propiedades.put("app.pagos.tipo-cambio-bs-usd", PagosPropertiesPrueba.TIPO_CAMBIO);
        propiedades.put("app.pagos.pasarela.url-retorno", PagosPropertiesPrueba.URL_RETORNO);
        propiedades.put("app.pagos.pasarela.enlace-app", PagosPropertiesPrueba.ENLACE_APP);
        return propiedades;
    }

    private static PagosProperties enlazar(Map<String, String> propiedades) {
        return new Binder(new MapConfigurationPropertySource(propiedades))
            .bindOrCreate("app.pagos", PagosProperties.class);
    }

    private static boolean esValido(Map<String, String> propiedades) {
        return VALIDADOR.validate(enlazar(propiedades)).isEmpty();
    }

    private static boolean esValido(PagosProperties propiedades) {
        return VALIDADOR.validate(propiedades).isEmpty();
    }

    // ------------------------------------------------------------------ tipo de cambio

    @Test
    void elTipoDeCambio_salePorCompletoDeLaConfiguracion() {
        assertThat(enlazar(obligatorias()).tipoCambioBsUsd()).isEqualByComparingTo("11.77");

        Map<String, String> otra = obligatorias();
        otra.put("app.pagos.tipo-cambio-bs-usd", "9.50");
        assertThat(enlazar(otra).tipoCambioBsUsd()).isEqualByComparingTo("9.50");
    }

    @Test
    void sinTipoDeCambio_noHayValorPorDefecto_yNoEsValido() {
        Map<String, String> sin = obligatorias();
        sin.remove("app.pagos.tipo-cambio-bs-usd");

        assertThat(enlazar(sin).tipoCambioBsUsd()).isNull();
        assertThat(esValido(sin)).isFalse();
    }

    @Test
    void cero_unNegativo_yElPositivoSi() {
        assertThat(esValido(PagosPropertiesPrueba.con("0"))).isFalse();
        assertThat(esValido(PagosPropertiesPrueba.con("-1"))).isFalse();
        assertThat(esValido(PagosPropertiesPrueba.con("11.77"))).isTrue();
    }

    // ------------------------------------------------------------------ direcciones de retorno

    @Test
    void lasDireccionesDeRetorno_salenDeLaConfiguracion() {
        Map<String, String> propias = obligatorias();
        propias.put("app.pagos.pasarela.url-retorno", "https://api.drivehub.test/api/v1/pagos/retorno");
        propias.put("app.pagos.pasarela.enlace-app", "otraapp://pagos");

        PagosProperties.Pasarela pasarela = enlazar(propias).pasarela();

        assertThat(pasarela.urlRetorno()).isEqualTo("https://api.drivehub.test/api/v1/pagos/retorno");
        assertThat(pasarela.enlaceApp()).isEqualTo("otraapp://pagos");
    }

    @Test
    void sinLaUrlDeRetornoOSinElEnlaceDeLaApp_noHayValorPorDefecto_yNoEsValido() {
        Map<String, String> sinUrl = obligatorias();
        sinUrl.remove("app.pagos.pasarela.url-retorno");
        Map<String, String> sinEnlace = obligatorias();
        sinEnlace.remove("app.pagos.pasarela.enlace-app");
        Map<String, String> enBlanco = obligatorias();
        enBlanco.put("app.pagos.pasarela.url-retorno", "  ");

        assertThat(enlazar(sinUrl).pasarela().urlRetorno()).isNull();
        assertThat(esValido(sinUrl)).isFalse();
        assertThat(enlazar(sinEnlace).pasarela().enlaceApp()).isNull();
        assertThat(esValido(sinEnlace)).isFalse();
        assertThat(esValido(enBlanco)).isFalse();
    }

    @Test
    void sinNingunaConfiguracion_nadaDeLoObligatorioTieneValorEscondido() {
        PagosProperties vacia = enlazar(Map.of());

        assertThat(vacia.tipoCambioBsUsd()).isNull();
        assertThat(vacia.pasarela()).isNull();
        assertThat(esValido(Map.of())).isFalse();
    }

    // ------------------------------------------------------------------ lo que sí tiene valor por defecto

    @Test
    void laPasarela_traeSusValoresPorDefecto_queNoSonDireccionesNiDinero() {
        PagosProperties.Pasarela pasarela = enlazar(obligatorias()).pasarela();

        assertThat(pasarela.stripeSecretKey()).isEmpty();
        assertThat(pasarela.stripeWebhookSecret()).isEmpty();
        assertThat(pasarela.vigenciaSesionMinutos()).isEqualTo(30);
        assertThat(pasarela.stripeBaseUrl()).isEmpty();
    }

    @Test
    void conValoresPropios_laPasarelaLosUsa() {
        Map<String, String> propias = obligatorias();
        propias.put("app.pagos.pasarela.stripe-secret-key", "sk_test_x");
        propias.put("app.pagos.pasarela.vigencia-sesion-minutos", "60");

        PagosProperties.Pasarela pasarela = enlazar(propias).pasarela();

        assertThat(pasarela.stripeSecretKey()).isEqualTo("sk_test_x");
        assertThat(pasarela.vigenciaSesionMinutos()).isEqualTo(60);
    }

    @Test
    void laVigenciaDeLaPaginaDePago_vaDe30MinutosA24Horas() {
        java.util.function.IntFunction<Boolean> valida = minutos -> esValido(new PagosProperties(
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO),
            new PagosProperties.Pasarela("", "", minutos, PagosPropertiesPrueba.URL_RETORNO,
                PagosPropertiesPrueba.ENLACE_APP, ""),
            new PagosProperties.Vencimiento(300, 10)));

        assertThat(valida.apply(29)).isFalse();
        assertThat(valida.apply(30)).isTrue();
        assertThat(valida.apply(1440)).isTrue();
        assertThat(valida.apply(1441)).isFalse();
    }

    @Test
    void elVencimientoDeLosPagosPendientes_tieneSusValoresPorDefecto_yNoBajaDeDiezSegundos() {
        PagosProperties.Vencimiento vencimiento = enlazar(obligatorias()).vencimiento();
        assertThat(vencimiento.cadaSegundos()).isEqualTo(300);
        assertThat(vencimiento.margenMinutos()).isEqualTo(10);

        java.util.function.BiFunction<Integer, Integer, Boolean> valida = (cada, margen) -> esValido(
            new PagosProperties(new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO),
                PagosPropertiesPrueba.pasarela("", ""), new PagosProperties.Vencimiento(cada, margen)));
        assertThat(valida.apply(9, 10)).isFalse();
        assertThat(valida.apply(10, 0)).isTrue();
        assertThat(valida.apply(300, -1)).isFalse();
    }

    @Test
    void estaMarcadaParaValidarse_asiElServicioNoArrancaConUnValorInvalido() {
        assertThat(PagosProperties.class.isAnnotationPresent(Validated.class)).isTrue();
    }
}
