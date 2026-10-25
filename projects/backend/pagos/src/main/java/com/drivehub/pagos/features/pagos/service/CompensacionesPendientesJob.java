package com.drivehub.pagos.features.pagos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * El proceso periódico que reintenta las compensaciones que la pasarela todavía no pudo devolver (CU31). La
 * frecuencia es `app.pagos.compensacion-reintento-segundos` (5 minutos por defecto); arranca después de esa espera.
 *
 * * Un error se registra y no detiene el proceso: la próxima vuelta lo reintenta.
 */
@Component
@RequiredArgsConstructor
public class CompensacionesPendientesJob {

    private static final Logger LOG = LoggerFactory.getLogger(CompensacionesPendientesJob.class);

    private final ReversionService reversionService;

    @Scheduled(initialDelayString = "${app.pagos.compensacion-reintento-segundos:300}s",
        fixedDelayString = "${app.pagos.compensacion-reintento-segundos:300}s")
    public void ejecutar() {
        try {
            int cantidad = reversionService.reintentarCompensacionesPendientes();
            if (cantidad > 0) {
                LOG.info("Compensaciones pendientes logradas", kv("module", "pagos"), kv("cantidad", cantidad));
            }
        } catch (RuntimeException e) {
            LOG.error("Falló el reintento de compensaciones pendientes", kv("module", "pagos"), e);
        }
    }
}
