package com.drivehub.pagos.features.comprobantes.service;

import org.springframework.stereotype.Component;

import com.drivehub.pagos.features.pagos.model.ReversionPago;

/**
 * El motivo de una reversión, ya en español, para mostrarlo en el comprobante de reembolso (CU30).
 *
 * * Hoy la reversión guarda su motivo como texto en `motivo`: se muestra tal cual. Si no tiene, es una compensación
 * * automática ("La compra no se pudo completar").
 * * CU31 (UT-CU31-BE-01) guarda el CÓDIGO del motivo (`MotivoReembolso`) y el texto que escribe el estudiante si
 * * elige "Otro" (`motivo_detalle`): esa UT cambia este componente para traducir el código a su texto o devolver el
 * * detalle. Es el único lugar que cambia; el comprobante no se entera.
 */
@Component
public class TextoDeMotivoReversion {

    static final String COMPENSACION = "La compra no se pudo completar";

    public String de(ReversionPago reversion) {
        String motivo = reversion.getMotivo();
        return motivo == null || motivo.isBlank() ? COMPENSACION : motivo.trim();
    }
}
