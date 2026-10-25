package com.drivehub.pagos.features.comprobantes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.pagos.service.evento.ReembolsoEjecutado;

import lombok.RequiredArgsConstructor;

/**
 * Pide el comprobante de reembolso cuando la devolución se ejecutó (CU31 → CU30), después del commit que dejó el
 * pago reembolsado.
 *
 * * Nunca propaga un error: si falla, el comprobante queda pendiente y se reintenta (o se pide de nuevo); un pago
 * * ya reembolsado jamás se deshace por esto.
 */
@Component
@RequiredArgsConstructor
public class ComprobanteReembolsoListener {

    private static final Logger LOG = LoggerFactory.getLogger(ComprobanteReembolsoListener.class);

    private final ComprobanteService comprobanteService;
    private final ReversionPagoRepository reversionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEjecutarseElReembolso(ReembolsoEjecutado evento) {
        try {
            reversionRepository.findByCodigo(evento.reversionCodigo()).ifPresent(reversion ->
                comprobanteService.generarDeReembolso(evento.pagoCodigo(), evento.reversionCodigo(),
                    reversion.getMonto(), reversion.getMontoRetenido()));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo pedir el comprobante de un reembolso ejecutado (tipo {})",
                e.getClass().getSimpleName(), kv("module", "comprobantes"),
                kv("reversionCodigo", evento.reversionCodigo()));
        }
    }
}
