package com.drivehub.pagos.features.pagos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.drivehub.pagos.features.pagos.service.evento.CompensacionRequerida;

import lombok.RequiredArgsConstructor;

/**
 * Atiende el pedido de devolver un cobro completo (CU27 lo publica, CU31 lo devuelve), después del commit de quien
 * lo pidió.
 *
 * * Nunca propaga un error: si algo falla se registra y la compensación queda para el proceso de reintento (o
 * * para revisión), pero el aviso de la pasarela que la disparó ya se aplicó.
 */
@Component
@RequiredArgsConstructor
public class CompensacionListener {

    private static final Logger LOG = LoggerFactory.getLogger(CompensacionListener.class);

    private final ReversionService reversionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRequerirseUnaCompensacion(CompensacionRequerida evento) {
        try {
            reversionService.compensar(evento.pagoCodigo(), evento.motivo(), evento.cobroReferencia());
        } catch (RuntimeException e) {
            LOG.error("No se pudo registrar la compensación de un cobro (tipo {})", e.getClass().getSimpleName(),
                kv("module", "pagos"), kv("pagoCodigo", evento.pagoCodigo()), e);
        }
    }
}
