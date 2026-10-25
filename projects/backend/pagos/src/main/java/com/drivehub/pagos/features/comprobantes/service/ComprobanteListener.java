package com.drivehub.pagos.features.comprobantes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.drivehub.pagos.features.pagos.service.evento.PagoConfirmado;

import lombok.RequiredArgsConstructor;

/**
 * Genera el comprobante de la compra apenas el pago queda confirmado (CU30), sin que nadie lo pida.
 *
 * * Escucha `PagoConfirmado` DESPUÉS del commit: si la confirmación se deshace, no hay comprobante. El servicio
 * * trabaja en transacciones propias (`REQUIRES_NEW`).
 * * El aviso de la pasarela ya se respondió y el pago ya está confirmado: cualquier excepción se captura y se
 * * registra, nunca vuelve hacia atrás.
 * * El comprobante de REEMBOLSO no se dispara acá: lo pide CU31 cuando la devolución se ejecuta.
 */
@Component
@RequiredArgsConstructor
public class ComprobanteListener {

    private static final Logger LOG = LoggerFactory.getLogger(ComprobanteListener.class);

    private final ComprobanteService comprobanteService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarseElPago(PagoConfirmado evento) {
        try {
            comprobanteService.generarDePago(evento.pagoCodigo());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo pedir el comprobante de un pago confirmado (tipo {})",
                e.getClass().getSimpleName(), kv("module", "comprobantes"), kv("pagoCodigo", evento.pagoCodigo()));
        }
    }
}
