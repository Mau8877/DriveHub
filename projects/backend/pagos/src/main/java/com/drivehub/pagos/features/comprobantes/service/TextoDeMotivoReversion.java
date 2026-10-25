package com.drivehub.pagos.features.comprobantes.service;

import org.springframework.stereotype.Component;

import com.drivehub.pagos.features.pagos.enums.MotivoReembolso;
import com.drivehub.pagos.features.pagos.enums.OrigenReversion;
import com.drivehub.pagos.features.pagos.model.ReversionPago;

/**
 * El motivo de una reversión, ya en español, para mostrarlo en el comprobante de reembolso (CU30).
 *
 * * Reembolso del estudiante: el texto del motivo que eligió, o lo que escribió si eligió "Otro".
 * * Compensación automática (o una reversión sin motivo): "La compra no se pudo completar".
 */
@Component
public class TextoDeMotivoReversion {

    static final String COMPENSACION = "La compra no se pudo completar";

    public String de(ReversionPago reversion) {
        if (reversion.getOrigen() == OrigenReversion.ESTUDIANTE) {
            MotivoReembolso motivo = motivoDe(reversion.getMotivo());
            if (motivo == MotivoReembolso.OTRO && reversion.getMotivoDetalle() != null
                    && !reversion.getMotivoDetalle().isBlank()) {
                return reversion.getMotivoDetalle().trim();
            }
            if (motivo != null) {
                return motivo.getTexto();
            }
        }
        return COMPENSACION;
    }

    private static MotivoReembolso motivoDe(String codigo) {
        try {
            return codigo == null ? null : MotivoReembolso.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
