package com.drivehub.pagos.features.pagos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.drivehub.pagos.config.PagosProperties;
import com.drivehub.pagos.features.pagos.dto.ReembolsoPrevisto;

import lombok.RequiredArgsConstructor;

/**
 * Calcula cuánto se devuelve y cuánto se retiene al reembolsar una compra (CU28; CU31 la reutiliza).
 *
 * * El porcentaje retenido es configuración (`REEMBOLSO_RETENCION_PORCENTAJE`).
 * * Lo devuelto se redondea a centavos y lo retenido es `pagado − devuelto`: así la suma da siempre
 * * exactamente lo pagado y no se pierde un centavo por redondeo.
 */
@Component
@RequiredArgsConstructor
public class ReembolsoCalculadora {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final PagosProperties propiedades;

    public ReembolsoPrevisto calcular(BigDecimal montoPagado) {
        int retencion = propiedades.retencionReembolsoPorcentaje();
        BigDecimal devuelto = montoPagado.multiply(BigDecimal.valueOf(100L - retencion))
            .divide(CIEN, 2, RoundingMode.HALF_UP);
        return new ReembolsoPrevisto(devuelto, montoPagado.subtract(devuelto), retencion);
    }
}
