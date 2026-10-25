package com.drivehub.pagos.features.planes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.drivehub.pagos.config.PagosProperties;

import lombok.RequiredArgsConstructor;

/**
 * Pasa bolivianos a dólares con el tipo de cambio configurado (`app.pagos.tipo-cambio-bs-usd`).
 *
 * * Es solo para mostrar: el dólar no se guarda en ninguna tabla, así que cambiar el tipo de cambio
 * * recalcula todos los planes sin tocar los datos.
 * * Dos decimales y `HALF_UP`: el medio centavo sube.
 */
@Component
@RequiredArgsConstructor
public class ConversionMoneda {

    private static final int DECIMALES = 2;

    private final PagosProperties propiedades;

    public BigDecimal aDolares(BigDecimal bolivianos) {
        return bolivianos.divide(propiedades.tipoCambioBsUsd(), DECIMALES, RoundingMode.HALF_UP);
    }
}
