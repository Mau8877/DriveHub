package com.drivehub.pagos.features.pagos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * El proceso periódico que vence los pagos pendientes cuya página de pago ya venció (CU26). La frecuencia es
 * `app.pagos.vencimiento.cada-segundos`; arranca después de esa espera, no al levantar el servicio.
 *
 * * Un error se registra y no detiene el proceso: la próxima vuelta lo reintenta.
 */
@Component
@RequiredArgsConstructor
public class PagosVencidosJob {

    private static final Logger LOG = LoggerFactory.getLogger(PagosVencidosJob.class);

    private final PagoService pagoService;
    private final Clock reloj;

    @Scheduled(initialDelayString = "${app.pagos.vencimiento.cada-segundos:300}s",
        fixedDelayString = "${app.pagos.vencimiento.cada-segundos:300}s")
    public void ejecutar() {
        try {
            int cantidad = pagoService.vencerPendientes(reloj.instant());
            if (cantidad > 0) {
                LOG.info("Pagos pendientes vencidos", kv("module", "pagos"), kv("cantidad", cantidad));
            }
        } catch (RuntimeException e) {
            LOG.error("Falló el proceso de vencimiento de pagos", kv("module", "pagos"), e);
        }
    }
}
