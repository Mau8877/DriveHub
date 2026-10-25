package com.drivehub.pagos.features.pagos.pasarela;

import java.util.UUID;

/**
 * Un aviso de la pasarela ya verificado (firma válida) y traducido a lo que Pagos entiende (CU27, CU31).
 *
 * @param eventoId el id del aviso en la pasarela
 * @param referenciaExterna en un cobro, el id de la sesión de cobro (con él se encuentra el pago); en un
 *     reembolso, el id de la devolución
 * @param pagoCodigo el código del pago que Pagos le puso a la sesión o a la devolución al crearla; puede faltar
 * @param montoCentavos lo que se cobró, en centavos de la moneda; nulo si el aviso no lo trae
 * @param moneda la moneda del cobro, en minúscula (`usd`)
 * @param cobroReferencia el id del cobro en la pasarela: lo necesita CU31 para devolverlo
 * @param reversionCodigo solo en un aviso de reembolso: el código de la reversión que Pagos le puso a la devolución
 */
public record AvisoPasarela(
        String eventoId,
        TipoAviso tipo,
        String referenciaExterna,
        UUID pagoCodigo,
        Long montoCentavos,
        String moneda,
        String cobroReferencia,
        UUID reversionCodigo) {

    /** Un aviso de cobro (sin reversión). */
    public AvisoPasarela(String eventoId, TipoAviso tipo, String referenciaExterna, UUID pagoCodigo,
            Long montoCentavos, String moneda, String cobroReferencia) {
        this(eventoId, tipo, referenciaExterna, pagoCodigo, montoCentavos, moneda, cobroReferencia, null);
    }

    /** Un aviso que Pagos no necesita. */
    public static AvisoPasarela otro(String eventoId) {
        return new AvisoPasarela(eventoId, TipoAviso.OTRO, null, null, null, null, null);
    }

    /** Si el aviso trata de una devolución (y no de un cobro). */
    public boolean esDeReembolso() {
        return tipo == TipoAviso.REEMBOLSO_EJECUTADO || tipo == TipoAviso.REEMBOLSO_FALLIDO;
    }
}
