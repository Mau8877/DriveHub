package com.drivehub.pagos.features.pagos.pasarela;

import java.time.Instant;

/**
 * La sesión de cobro que creó la pasarela: dónde paga el estudiante y con qué referencia se la reconoce.
 *
 * @param referenciaExterna el id de la sesión del lado de la pasarela
 * @param urlPago la dirección de la página de pago
 */
public record SesionCobro(String referenciaExterna, String urlPago, Instant expiraEn) {
}
