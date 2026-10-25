package com.drivehub.pagos.features.pagos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.pagos.common.outbox.OutboxEvento;
import com.drivehub.pagos.common.outbox.OutboxEventoRepository;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;
import com.drivehub.pagos.features.pagos.pasarela.AvisoPasarela;
import com.drivehub.pagos.features.pagos.pasarela.TipoAviso;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.TransaccionPasarelaRepository;
import com.drivehub.pagos.features.pagos.service.evento.CompensacionRequerida;
import com.drivehub.pagos.features.pagos.service.evento.MotivoCompensacion;
import com.drivehub.pagos.features.pagos.service.evento.PagoConfirmado;

import tools.jackson.databind.json.JsonMapper;

/**
 * Aplica los avisos de la pasarela (CU27): confirma el pago y acredita las horas al estudiante, o lo da por
 * fallido si el cobro no se completó. Solo el aviso FIRMADO de la pasarela confirma un pago (el controller lo
 * verifica antes de llegar acá); volver a la app no confirma nada.
 *
 * * Confirmar el pago y acreditar sus horas es UNA operación: si no se pueden acreditar, el pago no queda
 * * confirmado (se deshace todo), queda fallido y se pide devolver el cobro completo.
 * * Es idempotente: el mismo aviso otra vez (la pasarela reintenta) no cambia nada ni acredita dos veces. El pago
 * * se lee con bloqueo de escritura, así dos avisos iguales a la vez se atienden uno después del otro.
 * * Un error que no es de este pago (la base caída, un bloqueo) se propaga: el aviso responde un error y la
 * * pasarela lo reintenta. Solo un fallo al acreditar las horas pide devolver el cobro.
 * * Los eventos internos (`PagoConfirmado`, `CompensacionRequerida`) se publican dentro de la transacción y se
 * * escuchan después del commit; no pasan por RabbitMQ. Para otros servicios se escribe `outbox_evento` en la misma
 * * transacción.
 * ! No se loguean montos, datos de tarjeta ni el cuerpo del aviso; solo códigos.
 */
@Service
public class ConfirmarPagoService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmarPagoService.class);
    private static final String MODULO = "pagos";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRANSACCION_COMPLETADA = "COMPLETADA";
    private static final String TRANSACCION_VENCIDA = "VENCIDA";

    private final PagoRepository pagoRepository;
    private final TransaccionPasarelaRepository transaccionRepository;
    private final BilleteraService billeteraService;
    private final OutboxEventoRepository outboxRepository;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final ReversionService reversionService;
    private final TransactionTemplate escritura;
    private final TransactionTemplate lectura;

    public ConfirmarPagoService(PagoRepository pagoRepository, TransaccionPasarelaRepository transaccionRepository,
            BilleteraService billeteraService, OutboxEventoRepository outboxRepository,
            ApplicationEventPublisher eventos, Clock reloj, PlatformTransactionManager transactionManager,
            ReversionService reversionService) {
        this.reversionService = reversionService;
        this.pagoRepository = pagoRepository;
        this.transaccionRepository = transaccionRepository;
        this.billeteraService = billeteraService;
        this.outboxRepository = outboxRepository;
        this.eventos = eventos;
        this.reloj = reloj;
        this.escritura = new TransactionTemplate(transactionManager);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
    }

    /** No se pudieron acreditar las horas: se deshace la confirmación y se pide devolver el cobro. */
    private static final class AcreditacionFallida extends RuntimeException {
        AcreditacionFallida(Throwable causa) {
            super(causa);
        }
    }

    /**
     * Aplica un aviso ya verificado de la pasarela. Los avisos de tipo `OTRO` y los de un cobro que Pagos no
     * conoce se ignoran sin error (la pasarela no debe reintentarlos).
     */
    public void procesar(AvisoPasarela aviso) {
        if (aviso.tipo() == TipoAviso.OTRO) {
            return;
        }
        if (aviso.esDeReembolso()) {
            reversionService.aplicarAvisoReembolso(aviso);
            return;
        }
        Long pagoId = lectura.execute(estado -> aviso.referenciaExterna() == null ? null
            : transaccionRepository.findByReferenciaExterna(aviso.referenciaExterna())
                .map(transaccion -> transaccion.getPago().getId()).orElse(null));
        if (pagoId == null) {
            LOG.warn("Aviso de la pasarela de un cobro que Pagos no conoce", kv("module", MODULO),
                kv("tipoAviso", aviso.tipo()), kv("eventoId", aviso.eventoId()));
            return;
        }
        if (aviso.tipo() == TipoAviso.COBRO_COMPLETADO) {
            cobroCompletado(pagoId, aviso);
        } else {
            cobroVencido(pagoId);
        }
    }

    // ------------------------------------------------------------------ cobro completado

    private void cobroCompletado(Long pagoId, AvisoPasarela aviso) {
        try {
            escritura.executeWithoutResult(estado -> confirmar(pagoId, aviso));
        } catch (AcreditacionFallida e) {
            LOG.error("No se pudieron acreditar las horas de un pago cobrado: se pide devolver el cobro",
                kv("module", MODULO), kv("eventoId", aviso.eventoId()), e.getCause());
            escritura.executeWithoutResult(estado -> fallarPorAcreditacion(pagoId, aviso));
        }
    }

    private void confirmar(Long pagoId, AvisoPasarela aviso) {
        Pago pago = pagoRepository.buscarParaActualizar(pagoId).orElseThrow();
        switch (pago.getEstado()) {
            case CONFIRMADO, REVERTIDO -> LOG.info("Aviso repetido de un pago que ya estaba resuelto",
                kv("module", MODULO), kv("pagoCodigo", pago.getCodigo()));
            case FALLIDO -> {
                // * Pagó después de que la página venció (o de que el pago ya había fallado): el cobro sobra
                LOG.warn("Se cobró un pago que ya estaba fallido: se pide devolver el cobro", kv("module", MODULO),
                    kv("pagoCodigo", pago.getCodigo()));
                eventos.publishEvent(new CompensacionRequerida(pago.getCodigo(), MotivoCompensacion.COBRO_TARDIO,
                    aviso.cobroReferencia()));
            }
            case PENDIENTE -> confirmarPendiente(pago, aviso);
            default -> LOG.warn("Aviso de cobro para un pago en un estado inesperado", kv("module", MODULO),
                kv("pagoCodigo", pago.getCodigo()));
        }
    }

    private void confirmarPendiente(Pago pago, AvisoPasarela aviso) {
        if (!coincideElMonto(pago, aviso)) {
            LOG.warn("El monto cobrado no coincide con el del pago: se pide devolver el cobro", kv("module", MODULO),
                kv("pagoCodigo", pago.getCodigo()));
            pago.setEstado(EstadoPago.FALLIDO);
            pagoRepository.save(pago);
            actualizarTransaccion(pago, TRANSACCION_COMPLETADA, aviso.cobroReferencia());
            eventos.publishEvent(new CompensacionRequerida(pago.getCodigo(), MotivoCompensacion.MONTO_DISTINTO,
                aviso.cobroReferencia()));
            return;
        }
        pago.setEstado(EstadoPago.CONFIRMADO);
        pago.setConfirmadoEn(reloj.instant());
        try {
            billeteraService.acreditar(pago);
        } catch (TransientDataAccessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AcreditacionFallida(e);
        }
        pagoRepository.save(pago);
        actualizarTransaccion(pago, TRANSACCION_COMPLETADA, aviso.cobroReferencia());
        outboxRepository.save(new OutboxEvento("pago.confirmado", JSON.writeValueAsString(Map.of(
            "pagoCodigo", pago.getCodigo().toString(),
            "escuelaCodigo", pago.getEscuelaCodigo().toString(),
            "estudianteCodigo", pago.getEstudianteCodigo().toString(),
            "horas", pago.getHoras()))));
        eventos.publishEvent(new PagoConfirmado(pago.getCodigo()));

        LOG.info("Pago confirmado", kv("module", MODULO), kv("pagoCodigo", pago.getCodigo()),
            kv("escuelaCodigo", pago.getEscuelaCodigo()));
    }

    private void fallarPorAcreditacion(Long pagoId, AvisoPasarela aviso) {
        Pago pago = pagoRepository.buscarParaActualizar(pagoId).orElseThrow();
        if (pago.getEstado() == EstadoPago.PENDIENTE) {
            pago.setEstado(EstadoPago.FALLIDO);
            pagoRepository.save(pago);
        }
        actualizarTransaccion(pago, TRANSACCION_COMPLETADA, aviso.cobroReferencia());
        eventos.publishEvent(new CompensacionRequerida(pago.getCodigo(), MotivoCompensacion.ACREDITACION_FALLIDA,
            aviso.cobroReferencia()));
    }

    /** Lo cobrado es lo del pago: el mismo monto en centavos y la misma moneda. */
    private static boolean coincideElMonto(Pago pago, AvisoPasarela aviso) {
        if (aviso.montoCentavos() == null || aviso.moneda() == null) {
            return false;
        }
        BigDecimal esperado = pago.getMonto().movePointRight(2);
        return esperado.compareTo(BigDecimal.valueOf(aviso.montoCentavos())) == 0
            && aviso.moneda().equalsIgnoreCase(pago.getMoneda());
    }

    // ------------------------------------------------------------------ cobro vencido

    private void cobroVencido(Long pagoId) {
        escritura.executeWithoutResult(estado -> {
            Pago pago = pagoRepository.buscarParaActualizar(pagoId).orElseThrow();
            if (pago.getEstado() != EstadoPago.PENDIENTE) {
                return;
            }
            pago.setEstado(EstadoPago.FALLIDO);
            pagoRepository.save(pago);
            actualizarTransaccion(pago, TRANSACCION_VENCIDA, null);
            LOG.info("Pago vencido por la pasarela", kv("module", MODULO), kv("pagoCodigo", pago.getCodigo()));
        });
    }

    // ------------------------------------------------------------------ utilidades

    /** Deja en la transacción con la pasarela cómo terminó y, si hay, el id del cobro (para devolverlo luego). */
    private void actualizarTransaccion(Pago pago, String estado, String cobroReferencia) {
        TransaccionPasarela transaccion = transaccionRepository.findFirstByPagoIdOrderByIdDesc(pago.getId())
            .orElse(null);
        if (transaccion == null) {
            return;
        }
        transaccion.setEstado(estado);
        if (cobroReferencia != null) {
            transaccion.setRespuesta(JSON.writeValueAsString(Map.of("cobroReferencia", cobroReferencia)));
        }
        transaccionRepository.save(transaccion);
    }
}
