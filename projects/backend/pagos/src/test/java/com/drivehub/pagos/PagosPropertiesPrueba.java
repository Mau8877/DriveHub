package com.drivehub.pagos;

import java.math.BigDecimal;

import com.drivehub.pagos.config.PagosProperties;

/**
 * La configuración de Pagos para los tests que no levantan Spring.
 *
 * * El código de producción NO tiene valores por defecto del tipo de cambio ni de las direcciones de retorno
 * * (salen de variables de entorno): los tests ponen los suyos, de prueba, acá y en
 * * `src/test/resources/config/application.properties` (los tests que sí levantan Spring).
 * * El tipo de cambio de prueba es el mismo que el de `.env.example`, pero ningún test depende de ese archivo.
 */
public final class PagosPropertiesPrueba {

    public static final String TIPO_CAMBIO = "11.77";
    public static final String URL_RETORNO = "https://retorno.drivehub.test/api/v1/pagos/retorno";
    public static final String ENLACE_APP = "drivehub://app/payments/return";
    public static final int RETENCION_REEMBOLSO = 5;

    private PagosPropertiesPrueba() {
    }

    /** Con el tipo de cambio dado y todo lo demás de prueba. */
    public static PagosProperties con(String tipoCambio) {
        return new PagosProperties(new BigDecimal(tipoCambio), pasarela("", ""),
            new PagosProperties.Vencimiento(300, 10), RETENCION_REEMBOLSO);
    }

    /** Con el tipo de cambio de prueba y la pasarela dada. */
    public static PagosProperties conPasarela(PagosProperties.Pasarela pasarela) {
        return new PagosProperties(new BigDecimal(TIPO_CAMBIO), pasarela, new PagosProperties.Vencimiento(300, 10),
            RETENCION_REEMBOLSO);
    }

    /** Con la retención del reembolso dada y todo lo demás de prueba. */
    public static PagosProperties conRetencion(int porcentaje) {
        return new PagosProperties(new BigDecimal(TIPO_CAMBIO), pasarela("", ""),
            new PagosProperties.Vencimiento(300, 10), porcentaje);
    }

    /** Una pasarela de prueba: la clave y la dirección de la API de Stripe se eligen; lo demás es de prueba. */
    public static PagosProperties.Pasarela pasarela(String stripeSecretKey, String stripeBaseUrl) {
        return new PagosProperties.Pasarela(stripeSecretKey, "", 30, URL_RETORNO, ENLACE_APP, stripeBaseUrl, 300);
    }

    /** Una pasarela de prueba con el secreto con que se firman los avisos y la tolerancia dada, en segundos. */
    public static PagosProperties.Pasarela pasarelaConAvisos(String webhookSecret, long toleranciaSegundos) {
        return new PagosProperties.Pasarela("", webhookSecret, 30, URL_RETORNO, ENLACE_APP, "", toleranciaSegundos);
    }
}
