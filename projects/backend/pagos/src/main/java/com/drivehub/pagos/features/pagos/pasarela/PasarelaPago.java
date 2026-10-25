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
}
