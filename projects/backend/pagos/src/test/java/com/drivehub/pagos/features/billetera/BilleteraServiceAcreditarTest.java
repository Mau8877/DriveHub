package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.pagos.features.billetera.enums.TipoMovimiento;
import com.drivehub.pagos.features.billetera.model.BilleteraHoras;
import com.drivehub.pagos.features.billetera.model.MovimientoHoras;
import com.drivehub.pagos.features.billetera.repository.BilleteraRepository;
import com.drivehub.pagos.features.billetera.repository.MovimientoHorasRepository;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class BilleteraServiceAcreditarTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    @Mock
    private BilleteraRepository billeteraRepository;
    @Mock
    private MovimientoHorasRepository movimientoRepository;
    @Mock
    private PagoRepository pagoRepository;

    private BilleteraService servicio;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void preparar() {
        servicio = new BilleteraService(billeteraRepository, movimientoRepository, pagoRepository);
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(BilleteraService.class)).addAppender(logs);
    }

    @AfterEach
    void limpiarLogs() {
        ((Logger) LoggerFactory.getLogger(BilleteraService.class)).detachAppender(logs);
    }

    private static Pago pago(int horas, long id) {
        PlanPago plan = new PlanPago(ESCUELA, "Plan", (short) horas, new BigDecimal("450.00"));
        Pago pago = new Pago(ESCUELA, ESTUDIANTE, plan, new BigDecimal("38.23"), new BigDecimal("11.77"), "Ana",
            "Escuela", "clave-" + id, Instant.now());
        ReflectionTestUtils.setField(pago, "id", id);
        ReflectionTestUtils.setField(pago, "codigo", UUID.randomUUID());
        return pago;
    }

    private BilleteraHoras billeteraConSaldo(int saldo) {
        BilleteraHoras billetera = new BilleteraHoras(ESCUELA, ESTUDIANTE);
        billetera.setHorasDisponibles(saldo);
        when(billeteraRepository.buscarParaActualizar(ESCUELA, ESTUDIANTE)).thenReturn(Optional.of(billetera));
        return billetera;
    }

    @Test
    void acreditar_laPrimeraCompra_creaLaBilleteraConElSaldoDelPlan() {
        Pago pago = pago(10, 1);
        BilleteraHoras billetera = billeteraConSaldo(0);

        boolean acreditado = servicio.acreditar(pago);

        assertThat(acreditado).isTrue();
        verify(billeteraRepository).crearSiNoExiste(any(UUID.class), eq(ESCUELA), eq(ESTUDIANTE));
        assertThat(billetera.getHorasDisponibles()).isEqualTo(10);
        verify(billeteraRepository).save(billetera);
    }

    @Test
    void acreditar_unaSegundaCompra_sumaAlSaldoQueYaHabia() {
        BilleteraHoras billetera = billeteraConSaldo(3);

        servicio.acreditar(pago(5, 2));

        assertThat(billetera.getHorasDisponibles()).isEqualTo(8);
    }

    @Test
    void acreditar_guardaElMovimientoConElPagoElTipoYLaClave() {
        Pago pago = pago(10, 1);
        BilleteraHoras billetera = billeteraConSaldo(0);

        servicio.acreditar(pago);

        ArgumentCaptor<MovimientoHoras> movimiento = ArgumentCaptor.forClass(MovimientoHoras.class);
        verify(movimientoRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getBilleteraHoras()).isSameAs(billetera);
        assertThat(movimiento.getValue().getPago()).isSameAs(pago);
        assertThat(movimiento.getValue().getTipo()).isEqualTo(TipoMovimiento.ACREDITACION);
        assertThat(movimiento.getValue().getHoras()).isEqualTo((short) 10);
        assertThat(movimiento.getValue().getIdempotencyKey()).isEqualTo("acreditacion:" + pago.getCodigo());
        assertThat(movimiento.getValue().getInstructorCodigo()).isNull();
        assertThat(movimiento.getValue().getClaseCodigo()).isNull();
    }

    @Test
    void acreditar_dejaEnElPagoLasHorasSinGastarIgualesALasCompradas() {
        Pago pago = pago(10, 1);
        billeteraConSaldo(0);

        servicio.acreditar(pago);

        assertThat(pago.getHorasRestantes()).isEqualTo((short) 10);
    }

    @Test
    void acreditar_elMismoPagoDosVeces_noSumaOtraVez() {
        Pago pago = pago(10, 1);
        BilleteraHoras billetera = billeteraConSaldo(10);
        when(movimientoRepository.existsByIdempotencyKeyAndPagoId("acreditacion:" + pago.getCodigo(), 1L))
            .thenReturn(true);

        boolean acreditado = servicio.acreditar(pago);

        assertThat(acreditado).isFalse();
        assertThat(billetera.getHorasDisponibles()).isEqualTo(10);
        verify(billeteraRepository, never()).save(any());
        verify(movimientoRepository, never()).save(any());
        assertThat(pago.getHorasRestantes()).isNull();
    }

    @Test
    void saldo_devuelveLasHorasDisponibles_oCeroSiNoTieneBilletera() {
        BilleteraHoras billetera = new BilleteraHoras(ESCUELA, ESTUDIANTE);
        billetera.setHorasDisponibles(7);
        when(billeteraRepository.findByEscuelaCodigoAndEstudianteCodigo(ESCUELA, ESTUDIANTE))
            .thenReturn(Optional.of(billetera));
        UUID otro = UUID.randomUUID();
        when(billeteraRepository.findByEscuelaCodigoAndEstudianteCodigo(ESCUELA, otro)).thenReturn(Optional.empty());

        assertThat(servicio.saldo(ESCUELA, ESTUDIANTE)).isEqualTo(7);
        assertThat(servicio.saldo(ESCUELA, otro)).isZero();
    }

    @Test
    void acreditar_registraSoloCodigosYHoras_nuncaDatosDelEstudiante() {
        billeteraConSaldo(0);

        servicio.acreditar(pago(10, 1));

        assertThat(logs.list).hasSize(1);
        String todo = logs.list.get(0).getFormattedMessage() + java.util.Arrays.toString(
            logs.list.get(0).getArgumentArray());
        assertThat(todo).doesNotContain("Ana").doesNotContain(ESTUDIANTE.toString());
        assertThat(todo).contains("horas");
    }
}
