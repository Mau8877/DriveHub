package com.drivehub.pagos.features.pagos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.common.outbox.OutboxEvento;
import com.drivehub.pagos.common.outbox.OutboxEventoRepository;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.dto.PagoDetalleResponse;
import com.drivehub.pagos.features.pagos.dto.ReembolsoPrevisto;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.enums.MotivoReembolso;
import com.drivehub.pagos.features.pagos.enums.OrigenReversion;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.ReversionPago;
import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;
import com.drivehub.pagos.features.pagos.pasarela.AvisoPasarela;
import com.drivehub.pagos.features.pagos.pasarela.DevolucionCreada;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.pasarela.SolicitudDevolucion;
import com.drivehub.pagos.features.pagos.pasarela.TipoAviso;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.pagos.repository.TransaccionPasarelaRepository;
import com.drivehub.pagos.features.pagos.service.evento.MotivoCompensacion;
import com.drivehub.pagos.features.pagos.service.evento.ReembolsoEjecutado;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Devolver el dinero de un pago (CU31): el reembolso que pide el estudiante y la compensación automática de un
 * cobro que no se pudo completar.
 *
 * * Nunca se llama a la pasarela con una transacción abierta: cada paso de base de datos tiene la suya
 * * (`REQUIRES_NEW`, también para poder correr desde un oyente que se ejecuta después de un commit).
 * * La devolución es asíncrona: la reversión queda `SOLICITADA` hasta el aviso firmado de la pasarela. Solo ahí el
 * * pago pasa a `REVERTIDO` y se emite el comprobante.
 * * Las horas de un reembolso se apartan ANTES de pedir la devolución, para que nadie las gaste en el medio; si la
 * * devolución falla vuelven (`restituirApartadas`).
 * * La clave de idempotencia con la pasarela es el código de la reversión: reintentar no devuelve dos veces.
 * * Una sola reversión vigente por pago (índice único parcial); pedir otra es `ERR_PAG_10`.
 * ! No se loguean montos ni datos del estudiante; solo códigos.
 */
@Service
public class ReversionService {

    private static final Logger LOG = LoggerFactory.getLogger(ReversionService.class);
    private static final String MODULO = "pagos";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PROVEEDOR_COBRO = "STRIPE";
    private static final String PROVEEDOR_DEVOLUCION = "STRIPE_REEMBOLSO";
    private static final int DETALLE_MINIMO = 5;
    private static final int DETALLE_MAXIMO = 200;
    private static final int LOTE_DE_REINTENTOS = 50;
    private static final Duration PLAZO_DE_COMPENSACION = Duration.ofHours(24);

    private final PagoRepository pagoRepository;
    private final ReversionPagoRepository reversionRepository;
    private final TransaccionPasarelaRepository transaccionRepository;
    private final BilleteraService billeteraService;
    private final PasarelaPago pasarela;
    private final ReembolsoCalculadora calculadora;
    private final ConsultaPagosService consultaPagosService;
    private final OutboxEventoRepository outboxRepository;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final TransactionTemplate escritura;
    private final TransactionTemplate lectura;

    public ReversionService(PagoRepository pagoRepository, ReversionPagoRepository reversionRepository,
            TransaccionPasarelaRepository transaccionRepository, BilleteraService billeteraService,
            PasarelaPago pasarela, ReembolsoCalculadora calculadora, ConsultaPagosService consultaPagosService,
            OutboxEventoRepository outboxRepository, ApplicationEventPublisher eventos, Clock reloj,
            PlatformTransactionManager transactionManager) {
        this.pagoRepository = pagoRepository;
        this.reversionRepository = reversionRepository;
        this.transaccionRepository = transaccionRepository;
        this.billeteraService = billeteraService;
        this.pasarela = pasarela;
        this.calculadora = calculadora;
        this.consultaPagosService = consultaPagosService;
        this.outboxRepository = outboxRepository;
        this.eventos = eventos;
        this.reloj = reloj;
        this.escritura = new TransactionTemplate(transactionManager);
        this.escritura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.lectura.setReadOnly(true);
    }

    /** Lo que hace falta para pedir la devolución a la pasarela, ya fuera de toda transacción. */
    private record Devolucion(Long reversionId, UUID reversionCodigo, UUID pagoCodigo, BigDecimal monto,
            String cobroReferencia, Instant creadaEn) {
    }

    // ------------------------------------------------------------------ reembolso del estudiante

    /**
     * El estudiante pide el reembolso de una compra que no usó: se retiene el porcentaje configurado, se apartan
     * sus horas y se pide la devolución a la pasarela. Devuelve el detalle de la compra ya como "reembolso en
     * proceso".
     *
     * * Es "todo o nada": solo si no se usó ninguna hora de la compra (`ERR_PAG_11`).
     * * Un pago ajeno o inexistente responde igual (`ERR_PAG_07`).
     * * Si la pasarela no responde, la reversión queda fallida, las horas vuelven y responde `ERR_PAG_06`.
     *
     * @param motivoDetalle solo con `OTRO`: lo que cuenta el estudiante, de 5 a 200 caracteres
     */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public PagoDetalleResponse solicitarReembolso(UUID escuelaCodigo, UUID estudianteCodigo, UUID pagoCodigo,
            MotivoReembolso motivo, String motivoDetalle) {
        String detalle = validarMotivo(motivo, motivoDetalle);

        Devolucion devolucion = escritura.execute(estado -> {
            Pago pago = pagoRepository.buscarDeEstudianteParaActualizar(pagoCodigo, estudianteCodigo, escuelaCodigo)
                .orElseThrow(() -> new NotFoundException(PagosErrorCodes.PAGO_NO_ENCONTRADO, "El pago no existe"));
            if (pago.getEstado() != EstadoPago.CONFIRMADO
                    || reversionRepository.existsByPagoIdAndEstadoNot(pago.getId(), EstadoReversion.FALLIDA)) {
                throw new ConflictException(PagosErrorCodes.REEMBOLSO_NO_DISPONIBLE,
                    "Esta compra no se puede reembolsar");
            }
            if (pago.getHoras() == null || !pago.getHoras().equals(pago.getHorasRestantes())) {
                throw new ConflictException(PagosErrorCodes.COMPRA_CON_HORAS_USADAS,
                    "Ya usaste horas de esta compra: no se puede reembolsar");
            }
            String cobro = cobroDe(pago);
            if (cobro == null) {
                LOG.error("Un pago confirmado no tiene el cobro de la pasarela guardado", kv("module", MODULO),
                    kv("pagoCodigo", pago.getCodigo()));
                throw noDisponible();
            }
            ReembolsoPrevisto calculo = calculadora.calcular(pago.getMonto());
            ReversionPago reversion = new ReversionPago(pago, OrigenReversion.ESTUDIANTE, motivo.name(), detalle,
                calculo.montoDevuelto(), calculo.montoRetenido());
            try {
                reversionRepository.saveAndFlush(reversion);
            } catch (DataIntegrityViolationException e) {
                // * Otro pedido del mismo reembolso ganó la carrera (índice único parcial)
                throw new ConflictException(PagosErrorCodes.REEMBOLSO_NO_DISPONIBLE,
                    "Esta compra no se puede reembolsar");
            }
            billeteraService.apartarPorReembolso(pago, reversion.getCodigo());
            LOG.info("Reembolso solicitado", kv("module", MODULO), kv("pagoCodigo", pago.getCodigo()),
                kv("reversionCodigo", reversion.getCodigo()));
            return new Devolucion(reversion.getId(), reversion.getCodigo(), pago.getCodigo(), reversion.getMonto(),
                cobro, reversion.getCreatedAt());
        });

        try {
            pedirDevolucion(devolucion);
        } catch (RuntimeException e) {
            LOG.warn("La pasarela no pudo crear la devolución de un reembolso: se restituyen las horas",
                kv("module", MODULO), kv("pagoCodigo", pagoCodigo), kv("reversionCodigo", devolucion.reversionCodigo()));
            escritura.executeWithoutResult(estado -> fallarReembolsoNoCreado(devolucion.reversionCodigo()));
            throw e instanceof ServicioNoDisponibleException ? e : noDisponible();
        }
        return consultaPagosService.detalle(escuelaCodigo, estudianteCodigo, pagoCodigo);
    }

    /** Valida el motivo antes de abrir ninguna transacción; devuelve el detalle a guardar (solo con `OTRO`). */
    private static String validarMotivo(MotivoReembolso motivo, String motivoDetalle) {
        if (motivo == null) {
            throw new ValidationException(PagosErrorCodes.VALIDACION, "Elige el motivo del reembolso");
        }
        if (motivo != MotivoReembolso.OTRO) {
            return null;
        }
        String detalle = motivoDetalle == null ? "" : motivoDetalle.trim();
        if (detalle.length() < DETALLE_MINIMO || detalle.length() > DETALLE_MAXIMO) {
            throw new ValidationException(PagosErrorCodes.VALIDACION,
                "Cuéntanos el motivo, de " + DETALLE_MINIMO + " a " + DETALLE_MAXIMO + " caracteres");
        }
        return detalle;
    }

    /** La pasarela no creó la devolución: la reversión falla y las horas apartadas vuelven al estudiante. */
    private void fallarReembolsoNoCreado(UUID reversionCodigo) {
        ReversionPago reversion = reversionRepository.buscarParaActualizarPorCodigo(reversionCodigo).orElseThrow();
        Pago pago = pagoRepository.buscarParaActualizar(reversion.getPago().getId()).orElseThrow();
        reversion.setEstado(EstadoReversion.FALLIDA);
        reversionRepository.save(reversion);
        billeteraService.restituirApartadas(pago, pago.getHoras(), reversionCodigo);
    }

    // ------------------------------------------------------------------ compensación automática

    /**
     * Devuelve el cobro COMPLETO de un pago que no se pudo completar (CU27 lo pide con `CompensacionRequerida`).
     *
     * * Idempotente: si el pago ya tiene una reversión vigente no hace nada.
     * * Si la pasarela no responde, la reversión queda `SOLICITADA` sin devolución y un proceso la reintenta
     * * (`reintentarCompensacionesPendientes`): no se pierde.
     * * Lo llama el sistema, sin sesión: no lleva `@PreAuthorize`.
     */
    public void compensar(UUID pagoCodigo, MotivoCompensacion motivo, String cobroReferencia) {
        Devolucion devolucion = escritura.execute(estado -> {
            Optional<Pago> encontrado = pagoRepository.buscarPorCodigoParaActualizar(pagoCodigo);
            if (encontrado.isEmpty()) {
                LOG.warn("Se pidió devolver el cobro de un pago que no existe", kv("module", MODULO),
                    kv("pagoCodigo", pagoCodigo));
                return null;
            }
            Pago pago = encontrado.get();
            if (reversionRepository.existsByPagoIdAndEstadoNot(pago.getId(), EstadoReversion.FALLIDA)) {
                LOG.info("El pago ya tiene una devolución vigente: no se crea otra", kv("module", MODULO),
                    kv("pagoCodigo", pagoCodigo));
                return null;
            }
            guardarCobroSiFalta(pago, cobroReferencia);
            ReversionPago reversion = new ReversionPago(pago, OrigenReversion.SAGA, motivo.name(), null,
                pago.getMonto(), BigDecimal.ZERO);
            try {
                reversionRepository.saveAndFlush(reversion);
            } catch (DataIntegrityViolationException e) {
                return null;
            }
            LOG.info("Compensación solicitada", kv("module", MODULO), kv("pagoCodigo", pagoCodigo),
                kv("reversionCodigo", reversion.getCodigo()), kv("motivo", motivo));
            return new Devolucion(reversion.getId(), reversion.getCodigo(), pagoCodigo, reversion.getMonto(),
                cobroDe(pago), reversion.getCreatedAt());
        });
        if (devolucion == null) {
            return;
        }
        try {
            pedirDevolucion(devolucion);
        } catch (RuntimeException e) {
            LOG.warn("La pasarela no pudo crear la devolución de una compensación: queda pendiente",
                kv("module", MODULO), kv("pagoCodigo", pagoCodigo), kv("reversionCodigo", devolucion.reversionCodigo()));
        }
    }

    /**
     * Reintenta las compensaciones que quedaron pedidas sin devolución en la pasarela (la clave de idempotencia es
     * la misma, así que no devuelve dos veces). Las que llevan más de 24 horas sin lograrse se registran como
     * `ERROR` en cada vuelta para que alguien las revise; igual se siguen reintentando.
     *
     * @return cuántas se lograron
     */
    public int reintentarCompensacionesPendientes() {
        List<Long> ids = lectura.execute(estado ->
            reversionRepository.idsDeCompensacionesPendientes(PageRequest.of(0, LOTE_DE_REINTENTOS)));
        int logradas = 0;
        for (Long id : ids == null ? List.<Long>of() : ids) {
            Devolucion devolucion = lectura.execute(estado -> cargarPendiente(id));
            if (devolucion == null) {
                continue;
            }
            if (devolucion.creadaEn() != null
                    && devolucion.creadaEn().isBefore(reloj.instant().minus(PLAZO_DE_COMPENSACION))) {
                LOG.error("Una compensación lleva más de 24 horas sin lograrse: hay que revisarla",
                    kv("module", MODULO), kv("pagoCodigo", devolucion.pagoCodigo()),
                    kv("reversionCodigo", devolucion.reversionCodigo()));
            }
            try {
                pedirDevolucion(devolucion);
                logradas++;
            } catch (RuntimeException e) {
                LOG.warn("La compensación sigue pendiente (tipo {})", e.getClass().getSimpleName(),
                    kv("module", MODULO), kv("reversionCodigo", devolucion.reversionCodigo()));
            }
        }
        return logradas;
    }

    private Devolucion cargarPendiente(Long reversionId) {
        ReversionPago reversion = reversionRepository.findById(reversionId).orElse(null);
        if (reversion == null || reversion.getEstado() != EstadoReversion.SOLICITADA
                || reversion.getReferenciaExterna() != null) {
            return null;
        }
        String cobro = cobroDe(reversion.getPago());
        if (cobro == null) {
            LOG.error("Una compensación pendiente no tiene el cobro de la pasarela guardado", kv("module", MODULO),
                kv("reversionCodigo", reversion.getCodigo()));
            return null;
        }
        return new Devolucion(reversion.getId(), reversion.getCodigo(), reversion.getPago().getCodigo(),
            reversion.getMonto(), cobro, reversion.getCreatedAt());
    }

    // ------------------------------------------------------------------ pedir la devolución

    /**
     * Pide la devolución a la pasarela (SIN transacción abierta) y deja registrada su referencia. Si la pasarela
     * falla, lanza su excepción y no registra nada.
     */
    private void pedirDevolucion(Devolucion devolucion) {
        long centavos = devolucion.monto().movePointRight(2).longValueExact();
        DevolucionCreada creada = pasarela.reembolsar(new SolicitudDevolucion(devolucion.cobroReferencia(), centavos,
            devolucion.reversionCodigo().toString(), devolucion.pagoCodigo(), devolucion.reversionCodigo()));
        try {
            escritura.executeWithoutResult(estado -> registrarDevolucion(devolucion.reversionId(), creada));
        } catch (RuntimeException e) {
            // * La pasarela ya la creó: no se deshace nada. El aviso firmado la encuentra por el código de la reversión
            LOG.error("No se pudo guardar la referencia de una devolución ya creada en la pasarela",
                kv("module", MODULO), kv("reversionCodigo", devolucion.reversionCodigo()), e);
        }
    }

    private void registrarDevolucion(Long reversionId, DevolucionCreada creada) {
        ReversionPago reversion = reversionRepository.findById(reversionId).orElseThrow();
        if (reversion.getReferenciaExterna() == null) {
            reversion.setReferenciaExterna(creada.referenciaExterna());
            reversionRepository.save(reversion);
        }
        if (transaccionRepository.findByReferenciaExterna(creada.referenciaExterna()).isEmpty()) {
            TransaccionPasarela transaccion = new TransaccionPasarela(reversion.getPago(), PROVEEDOR_DEVOLUCION,
                reversion.getEstado() == EstadoReversion.EJECUTADA ? "COMPLETADA" : "SOLICITADA");
            transaccion.setReferenciaExterna(creada.referenciaExterna());
            transaccion.setRespuesta(JSON.writeValueAsString(Map.of("reversionCodigo",
                reversion.getCodigo().toString())));
            transaccionRepository.save(transaccion);
        }
    }

    // ------------------------------------------------------------------ aviso de la pasarela

    /**
     * Aplica el aviso FIRMADO de la pasarela sobre una devolución: ejecutada (el pago queda reembolsado y se emite
     * el comprobante) o fallida (las horas apartadas vuelven al estudiante).
     *
     * * Idempotente: el mismo aviso otra vez no cambia nada. La reversión y el pago se leen con bloqueo de escritura.
     * * Un aviso de una devolución que Pagos no conoce se ignora (`WARN`): la pasarela no debe reintentarlo.
     */
    public void aplicarAvisoReembolso(AvisoPasarela aviso) {
        escritura.executeWithoutResult(estado -> aplicar(aviso));
    }

    private void aplicar(AvisoPasarela aviso) {
        Optional<ReversionPago> encontrada = aviso.reversionCodigo() == null ? Optional.empty()
            : reversionRepository.buscarParaActualizarPorCodigo(aviso.reversionCodigo());
        if (encontrada.isEmpty() && aviso.referenciaExterna() != null) {
            encontrada = reversionRepository.buscarParaActualizarPorReferencia(aviso.referenciaExterna());
        }
        if (encontrada.isEmpty()) {
            LOG.warn("Aviso de una devolución que Pagos no conoce", kv("module", MODULO),
                kv("tipoAviso", aviso.tipo()), kv("eventoId", aviso.eventoId()));
            return;
        }
        ReversionPago reversion = encontrada.get();
        Pago pago = pagoRepository.buscarParaActualizar(reversion.getPago().getId()).orElseThrow();
        if (aviso.pagoCodigo() != null && !aviso.pagoCodigo().equals(pago.getCodigo())) {
            LOG.warn("El aviso de una devolución no corresponde al pago de esa reversión: se ignora",
                kv("module", MODULO), kv("reversionCodigo", reversion.getCodigo()), kv("eventoId", aviso.eventoId()));
            return;
        }
        if (aviso.tipo() == TipoAviso.REEMBOLSO_EJECUTADO) {
            ejecutar(reversion, pago, aviso);
        } else {
            fallar(reversion, pago, aviso);
        }
    }

    private void ejecutar(ReversionPago reversion, Pago pago, AvisoPasarela aviso) {
        switch (reversion.getEstado()) {
            case EJECUTADA -> LOG.info("Aviso repetido de una devolución ya ejecutada", kv("module", MODULO),
                kv("reversionCodigo", reversion.getCodigo()), kv("tipoAviso", aviso.tipo()));
            case FALLIDA -> LOG.error("La pasarela devolvió el dinero de una reversión que Pagos dio por fallida:"
                + " hay que revisarla", kv("module", MODULO), kv("reversionCodigo", reversion.getCodigo()));
            case SOLICITADA -> {
                reversion.setEstado(EstadoReversion.EJECUTADA);
                reversion.setEjecutadoEn(reloj.instant());
                if (reversion.getReferenciaExterna() == null) {
                    reversion.setReferenciaExterna(aviso.referenciaExterna());
                }
                reversionRepository.save(reversion);
                pago.setEstado(EstadoPago.REVERTIDO);
                pagoRepository.save(pago);
                cerrarTransaccion(reversion, "COMPLETADA");
                outboxRepository.save(new OutboxEvento("pago.revertido", JSON.writeValueAsString(Map.of(
                    "pagoCodigo", pago.getCodigo().toString(),
                    "escuelaCodigo", pago.getEscuelaCodigo().toString(),
                    "estudianteCodigo", pago.getEstudianteCodigo().toString(),
                    "reversionCodigo", reversion.getCodigo().toString(),
                    "origen", reversion.getOrigen().name()))));
                eventos.publishEvent(new ReembolsoEjecutado(pago.getCodigo(), reversion.getCodigo()));
                LOG.info("Devolución ejecutada", kv("module", MODULO), kv("reversionCodigo", reversion.getCodigo()),
                    kv("tipoAviso", aviso.tipo()));
            }
            default -> { }
        }
    }

    private void fallar(ReversionPago reversion, Pago pago, AvisoPasarela aviso) {
        if (reversion.getEstado() != EstadoReversion.SOLICITADA) {
            LOG.info("Aviso de devolución fallida para una reversión que ya no está solicitada", kv("module", MODULO),
                kv("reversionCodigo", reversion.getCodigo()), kv("estado", reversion.getEstado()));
            return;
        }
        reversion.setEstado(EstadoReversion.FALLIDA);
        reversionRepository.save(reversion);
        cerrarTransaccion(reversion, "FALLIDA");
        if (reversion.getOrigen() == OrigenReversion.ESTUDIANTE) {
            billeteraService.restituirApartadas(pago, pago.getHoras(), reversion.getCodigo());
            LOG.warn("La devolución de un reembolso falló: las horas volvieron al estudiante", kv("module", MODULO),
                kv("reversionCodigo", reversion.getCodigo()), kv("tipoAviso", aviso.tipo()));
        } else {
            LOG.error("La devolución de una compensación falló en la pasarela: hay que revisarla manualmente",
                kv("module", MODULO), kv("reversionCodigo", reversion.getCodigo()), kv("tipoAviso", aviso.tipo()));
        }
    }

    private void cerrarTransaccion(ReversionPago reversion, String estado) {
        if (reversion.getReferenciaExterna() == null) {
            return;
        }
        transaccionRepository.findByReferenciaExterna(reversion.getReferenciaExterna()).ifPresent(transaccion -> {
            transaccion.setEstado(estado);
            transaccionRepository.save(transaccion);
        });
    }

    // ------------------------------------------------------------------ el cobro del pago

    /** El id del cobro en la pasarela, guardado por CU27 al confirmar el pago; nulo si no está. */
    private String cobroDe(Pago pago) {
        return transaccionRepository.findFirstByPagoIdAndProveedorOrderByIdDesc(pago.getId(), PROVEEDOR_COBRO)
            .map(TransaccionPasarela::getRespuesta).map(ReversionService::cobroEn).orElse(null);
    }

    private static String cobroEn(String respuesta) {
        try {
            JsonNode nodo = JSON.readTree(respuesta);
            if (nodo.isTextual()) {
                nodo = JSON.readTree(nodo.asText());
            }
            JsonNode cobro = nodo.path("cobroReferencia");
            return cobro.isTextual() && !cobro.asText().isBlank() ? cobro.asText() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Guarda el id del cobro en la transacción del pago si todavía no está (un cobro tardío no lo había guardado). */
    private void guardarCobroSiFalta(Pago pago, String cobroReferencia) {
        if (cobroReferencia == null || cobroReferencia.isBlank() || cobroDe(pago) != null) {
            return;
        }
        TransaccionPasarela transaccion = transaccionRepository
            .findFirstByPagoIdAndProveedorOrderByIdDesc(pago.getId(), PROVEEDOR_COBRO).orElse(null);
        if (transaccion == null) {
            return;
        }
        transaccion.setRespuesta(JSON.writeValueAsString(Map.of("cobroReferencia", cobroReferencia)));
        transaccionRepository.save(transaccion);
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(PagosErrorCodes.PASARELA_NO_DISPONIBLE,
            "No pudimos conectar con el pago. Inténtalo de nuevo más tarde.");
    }
}
