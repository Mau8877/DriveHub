package com.drivehub.pagos.features.pagos.pasarela;

/**
 * La pasarela de pago con tarjeta (hoy Stripe). Es una interfaz porque hay una implementación real y los
 * tests usan un doble: ninguno llama a Stripe de verdad.
 *
 * * Cualquier falla de la pasarela llega como `ServicioNoDisponibleException` (`ERR_PAG_06`).
 */
public interface PasarelaPago {

    /**
     * Crea la página donde el estudiante paga la compra.
     */
    SesionCobro crearSesionCobro(SolicitudCobro solicitud);

    /**
     * Comprueba que un aviso de la pasarela es de verdad de ella (la firma) y lo traduce.
     *
     * * Una firma que falta, que no es válida, de una hora fuera de la tolerancia, o sin secreto configurado es
     * * `ValidationException` `ERR_PAG_08` (400): el aviso se rechaza sin cambiar nada.
     *
     * @param cuerpo el cuerpo EXACTO que llegó (la firma se calcula sobre esos bytes)
     * @param firma el valor de la cabecera `Stripe-Signature`
     */
    AvisoPasarela verificarAviso(String cuerpo, String firma);
}
