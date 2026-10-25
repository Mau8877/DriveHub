package com.drivehub.pagos.features.comprobantes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * El proceso periódico que reintenta los comprobantes que quedaron sin PDF (CU30). La frecuencia es
 * `app.pagos.comprobantes-reintento-segundos` (5 minutos por defecto); arranca después de esa espera.
 *
 * * Un error se registra y no detiene el proceso: la próxima vuelta lo reintenta.
 */
@Component
@RequiredArgsConstructor
public class ComprobantesPendientesJob {

    private static final Logger LOG = LoggerFactory.getLogger(ComprobantesPendientesJob.class);

    private final ComprobanteService comprobanteService;

    @Scheduled(initialDelayString = "${app.pagos.comprobantes-reintento-segundos:300}s",
        fixedDelayString = "${app.pagos.comprobantes-reintento-segundos:300}s")
    public void ejecutar() {
        try {
            int cantidad = comprobanteService.reintentarPendientes();
            if (cantidad > 0) {
                LOG.info("Comprobantes pendientes completados", kv("module", "comprobantes"),
                    kv("cantidad", cantidad));
            }
        } catch (RuntimeException e) {
            LOG.error("Falló el reintento de comprobantes pendientes", kv("module", "comprobantes"), e);
        }
    }
}
