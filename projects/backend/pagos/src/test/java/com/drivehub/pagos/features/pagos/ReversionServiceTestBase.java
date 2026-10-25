package com.drivehub.pagos.features.pagos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.outbox.OutboxEvento;
import com.drivehub.pagos.common.outbox.OutboxEventoRepository;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.ReversionPago;
import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.pagos.repository.TransaccionPasarelaRepository;
import com.drivehub.pagos.features.pagos.service.ConsultaPagosService;
import com.drivehub.pagos.features.pagos.service.ReembolsoCalculadora;
import com.drivehub.pagos.features.pagos.service.ReversionService;
import com.drivehub.pagos.features.planes.model.PlanPago;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Lo común de los tests de `ReversionService` (CU31): repositorios, billetera, pasarela y eventos mockeados, un
 * pago confirmado e intacto de 10 horas y un reloj fijo. Las transacciones se ejecutan sin base de datos.
 */
abstract class ReversionServiceTestBase {

    protected static final UUID ESCUELA = UUID.randomUUID();
    protected static final UUID ESTUDIANTE = UUID.randomUUID();
    protected static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");
    protected static final String COBRO = "pi_test_456";

    protected PagoRepository pagos;
    protected ReversionPagoRepository reversiones;
    protected TransaccionPasarelaRepository transacciones;
    protected BilleteraService billetera;
    protected PasarelaPago pasarela;
    protected ConsultaPagosService consulta;
    protected OutboxEventoRepository outbox;
    protected ApplicationEventPublisher eventos;
    protected ReversionService servicio;
    protected Pago pago;
    protected TransaccionPasarela transaccionDelCobro;
    protected final List<ReversionPago> guardadas = new ArrayList<>();
    protected final List<TransaccionPasarela> transaccionesGuardadas = new ArrayList<>();
    protected final List<Object> eventosPublicados = new ArrayList<>();
    protected final List<OutboxEvento> outboxGuardado = new ArrayList<>();
    protected Instant reloj = AHORA;
    protected ListAppender<ILoggingEvent> registros;
    private final AtomicLong ids = new AtomicLong(500);
    private Logger logger;

    @BeforeEach
    void prepararBase() {
        pago = unPago(EstadoPago.CONFIRMADO, 10, 10);
        pagos = mock(PagoRepository.class);
        when(pagos.buscarDeEstudianteParaActualizar(pago.getCodigo(), ESTUDIANTE, ESCUELA))
            .thenReturn(Optional.of(pago));
        when(pagos.buscarPorCodigoParaActualizar(pago.getCodigo())).thenReturn(Optional.of(pago));
        when(pagos.buscarParaActualizar(pago.getId())).thenReturn(Optional.of(pago));

        reversiones = mock(ReversionPagoRepository.class);
        when(reversiones.saveAndFlush(any(ReversionPago.class))).thenAnswer(i -> {
            ReversionPago r = i.getArgument(0);
            asignar(r);
            guardadas.add(r);
            return r;
        });
        when(reversiones.save(any(ReversionPago.class))).thenAnswer(i -> i.getArgument(0));
        when(reversiones.findById(any(Long.class))).thenAnswer(i -> guardadas.stream()
            .filter(r -> r.getId().equals(i.getArgument(0))).findFirst());
        when(reversiones.buscarParaActualizarPorCodigo(any(UUID.class))).thenAnswer(i -> guardadas.stream()
            .filter(r -> r.getCodigo().equals(i.getArgument(0))).findFirst());
        when(reversiones.buscarParaActualizarPorReferencia(any(String.class))).thenAnswer(i -> guardadas.stream()
            .filter(r -> i.getArgument(0).equals(r.getReferenciaExterna())).findFirst());

        transaccionDelCobro = new TransaccionPasarela(pago, "STRIPE", "COMPLETADA");
        transaccionDelCobro.setRespuesta("{\"cobroReferencia\":\"" + COBRO + "\"}");
        transacciones = mock(TransaccionPasarelaRepository.class);
        when(transacciones.findFirstByPagoIdAndProveedorOrderByIdDesc(pago.getId(), "STRIPE"))
            .thenReturn(Optional.of(transaccionDelCobro));
        when(transacciones.findByReferenciaExterna(any(String.class))).thenAnswer(i -> transaccionesGuardadas.stream()
            .filter(t -> i.getArgument(0).equals(t.getReferenciaExterna())).findFirst());
        when(transacciones.save(any(TransaccionPasarela.class))).thenAnswer(i -> {
            TransaccionPasarela t = i.getArgument(0);
            if (!transaccionesGuardadas.contains(t) && t != transaccionDelCobro) {
                transaccionesGuardadas.add(t);
            }
            return t;
        });

        billetera = mock(BilleteraService.class);
        pasarela = mock(PasarelaPago.class);
        consulta = mock(ConsultaPagosService.class);
        outbox = mock(OutboxEventoRepository.class);
        when(outbox.save(any(OutboxEvento.class))).thenAnswer(i -> {
            outboxGuardado.add(i.getArgument(0));
            return i.getArgument(0);
        });
        eventos = eventosPublicados::add;

        servicio = new ReversionService(pagos, reversiones, transacciones, billetera, pasarela,
            new ReembolsoCalculadora(PagosPropertiesPrueba.conRetencion(5)), consulta, outbox, eventos, new Clock() {
                @Override
                public java.time.ZoneId getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return reloj;
                }
            }, mock(PlatformTransactionManager.class));

        logger = (Logger) LoggerFactory.getLogger(ReversionService.class);
        registros = new ListAppender<>();
        registros.start();
        logger.addAppender(registros);
    }

    @AfterEach
    void limpiarBase() {
        logger.detachAppender(registros);
    }

    protected Pago unPago(EstadoPago estado, int horas, int horasRestantes) {
        PlanPago plan = new PlanPago(ESCUELA, "Plan " + horas + " horas", (short) horas, new BigDecimal("450.00"));
        Pago p = new Pago(ESCUELA, ESTUDIANTE, plan, new BigDecimal("38.23"),
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO), "María Peña", "Autoescuela Cóndor", "clave-1", AHORA);
        p.setEstado(estado);
        p.setHorasRestantes((short) horasRestantes);
        ReflectionTestUtils.setField(p, "id", 1L);
        ReflectionTestUtils.setField(p, "codigo", UUID.randomUUID());
        ReflectionTestUtils.setField(p, "createdAt", AHORA);
        return p;
    }

    protected void asignar(ReversionPago r) {
        ReflectionTestUtils.setField(r, "id", ids.incrementAndGet());
        ReflectionTestUtils.setField(r, "codigo", UUID.randomUUID());
        ReflectionTestUtils.setField(r, "createdAt", reloj);
    }

    protected boolean huboRegistro(String nivel) {
        return registros.list.stream().anyMatch(e -> e.getLevel().toString().equals(nivel));
    }
}
