package com.drivehub.pagos.features.pagos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.drivehub.pagos.features.pagos.client.NotificacionesClient;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.service.evento.CompensacionRequerida;
import com.drivehub.pagos.features.pagos.service.evento.PagoConfirmado;
import com.drivehub.pagos.features.pagos.service.evento.PagoVencido;
import com.drivehub.pagos.features.pagos.service.evento.ReembolsoEjecutado;
import com.drivehub.pagos.features.pagos.service.evento.ReembolsoFallido;

import lombok.RequiredArgsConstructor;

/**
 * Avisa al estudiante lo que le pasó a su pago (CU36): acreditado, no acreditado, vencido y reembolso ejecutado o
 * fallido.
 *
 * * Escucha los eventos internos DESPUÉS del commit: si la operación se deshace, no se avisa nada.
 * * Nunca propaga un error: el aviso es de mejor esfuerzo y la operación que lo originó ya terminó.
 * * Cada aviso usa un `eventoCodigo` determinista por (evento, pago), así los reintentos de la pasarela no lo
 * * duplican.
 * ! No se escriben montos ni datos de pago en los registros; en el aviso solo viaja el nombre del plan y las horas.
 */
@Component
@RequiredArgsConstructor
public class AvisosPagoListener {

    private static final Logger LOG = LoggerFactory.getLogger(AvisosPagoListener.class);
    private static final String TIPO = "PAGO";

    private final PagoRepository pagoRepository;
    private final NotificacionesClient notificaciones;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarseElPago(PagoConfirmado evento) {
        avisar("pago.confirmado", evento.pagoCodigo(), "Compra confirmada",
            pago -> "Acreditamos %d hora(s) de tu plan %s. Ya puedes reservar tus clases."
                .formatted(pago.getHoras(), pago.getPlanNombre()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRequerirseUnaCompensacion(CompensacionRequerida evento) {
        avisar("pago.compensado", evento.pagoCodigo(), "No pudimos completar tu compra",
            pago -> "Tu compra del plan %s no se pudo acreditar. Te devolveremos el cobro completo."
                .formatted(pago.getPlanNombre()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alVencerElPago(PagoVencido evento) {
        avisar("pago.vencido", evento.pagoCodigo(), "Tu pago venció",
            pago -> "La página de pago del plan %s venció sin completarse. Puedes volver a comprarlo cuando quieras."
                .formatted(pago.getPlanNombre()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEjecutarseElReembolso(ReembolsoEjecutado evento) {
        avisar("pago.reembolsado", evento.pagoCodigo(), "Reembolso realizado",
            pago -> "Devolvimos el dinero de tu plan %s. Puede tardar unos días en reflejarse en tu tarjeta."
                .formatted(pago.getPlanNombre()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alFallarElReembolso(ReembolsoFallido evento) {
        avisar("pago.reembolso-fallido", evento.pagoCodigo(), "No pudimos reembolsarte",
            pago -> "La devolución de tu plan %s falló y tus horas volvieron a tu billetera. Inténtalo más tarde."
                .formatted(pago.getPlanNombre()));
    }

    private void avisar(String evento, UUID pagoCodigo, String titulo, Function<Pago, String> mensaje) {
        try {
            Pago pago = pagoRepository.findByCodigo(pagoCodigo).orElse(null);
            if (pago == null) {
                return;
            }
            notificaciones.avisar(NotificacionesClient.eventoCodigo(evento, pagoCodigo), pago.getEstudianteCodigo(),
                TIPO, titulo, mensaje.apply(pago), Map.of("pagoCodigo", pagoCodigo.toString(), "evento", evento));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo armar un aviso de pago (tipo {})", e.getClass().getSimpleName(),
                kv("module", "pagos"), kv("pagoCodigo", pagoCodigo), kv("evento", evento));
        }
    }
}
