package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.features.billetera.enums.TipoMovimiento;
import com.drivehub.pagos.features.billetera.model.BilleteraHoras;
import com.drivehub.pagos.features.billetera.model.MovimientoHoras;
import com.drivehub.pagos.features.billetera.repository.BilleteraRepository;
import com.drivehub.pagos.features.billetera.repository.MovimientoHorasRepository;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;

/**
 * Apartar las horas de un reembolso y devolverlas si la devolución falla (CU31).
 */
@ExtendWith(MockitoExtension.class)
class BilleteraServiceReembolsoTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID REVERSION = UUID.randomUUID();

    @Mock
    private BilleteraRepository billeteraRepository;
    @Mock
    private MovimientoHorasRepository movimientoRepository;
    @Mock
    private PagoRepository pagoRepository;

    private BilleteraService servicio;
    private Pago pago;
    private BilleteraHoras billetera;

    @BeforeEach
    void preparar() {
        servicio = new BilleteraService(billeteraRepository, movimientoRepository, pagoRepository);
        PlanPago plan = new PlanPago(ESCUELA, "Plan", (short) 10, new BigDecimal("450.00"));
        pago = new Pago(ESCUELA, ESTUDIANTE, plan, new BigDecimal("38.23"), new BigDecimal("11.77"), "Ana",
            "Escuela", "clave", Instant.now());
        pago.setHorasRestantes((short) 10);
        ReflectionTestUtils.setField(pago, "id", 1L);
        ReflectionTestUtils.setField(pago, "codigo", UUID.randomUUID());
        billetera = new BilleteraHoras(ESCUELA, ESTUDIANTE);
        billetera.setHorasDisponibles(14);
        when(billeteraRepository.buscarParaActualizar(ESCUELA, ESTUDIANTE)).thenReturn(Optional.of(billetera));
    }

    @Test
    void apartar_bajaElSaldo_vaciaLaCompra_yGuardaUnMovimientoReembolso() {
        servicio.apartarPorReembolso(pago, REVERSION);

        assertThat(billetera.getHorasDisponibles()).isEqualTo(4);
        assertThat(pago.getHorasRestantes()).isZero();
        ArgumentCaptor<MovimientoHoras> movimiento = ArgumentCaptor.forClass(MovimientoHoras.class);
        verify(movimientoRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getTipo()).isEqualTo(TipoMovimiento.REEMBOLSO);
        assertThat(movimiento.getValue().getHoras()).isEqualTo((short) 10);
        assertThat(movimiento.getValue().getIdempotencyKey()).isEqualTo("reembolso:" + REVERSION);
        assertThat(movimiento.getValue().getInstructorCodigo()).isNull();
        assertThat(movimiento.getValue().getClaseCodigo()).isNull();
        verify(billeteraRepository).save(billetera);
    }

    @Test
    void apartar_conLaMismaReversion_noApartaDosVeces() {
        when(movimientoRepository.existsByIdempotencyKeyAndPagoId("reembolso:" + REVERSION, 1L)).thenReturn(true);

        servicio.apartarPorReembolso(pago, REVERSION);

        assertThat(billetera.getHorasDisponibles()).isEqualTo(14);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void apartar_conSaldoMenorQueLaCompra_esErrPag09_yNoCambiaNada() {
        billetera.setHorasDisponibles(3);

        assertThatThrownBy(() -> servicio.apartarPorReembolso(pago, REVERSION))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_09"));
        assertThat(billetera.getHorasDisponibles()).isEqualTo(3);
        assertThat(pago.getHorasRestantes()).isEqualTo((short) 10);
    }

    @Test
    void apartar_sinBilletera_esErrPag09() {
        when(billeteraRepository.buscarParaActualizar(ESCUELA, ESTUDIANTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.apartarPorReembolso(pago, REVERSION))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void restituir_devuelveLasHorasAlSaldoYALaCompra_conUnMovimientoDevolucionSinInstructor() {
        pago.setHorasRestantes((short) 0);
        billetera.setHorasDisponibles(4);

        servicio.restituirApartadas(pago, 10, REVERSION);

        assertThat(billetera.getHorasDisponibles()).isEqualTo(14);
        assertThat(pago.getHorasRestantes()).isEqualTo((short) 10);
        ArgumentCaptor<MovimientoHoras> movimiento = ArgumentCaptor.forClass(MovimientoHoras.class);
        verify(movimientoRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getTipo()).isEqualTo(TipoMovimiento.DEVOLUCION);
        assertThat(movimiento.getValue().getIdempotencyKey()).isEqualTo("restitucion:" + REVERSION);
        assertThat(movimiento.getValue().getInstructorCodigo()).isNull();
    }

    @Test
    void restituir_conLaMismaReversion_noRestituyeDosVeces() {
        when(movimientoRepository.existsByIdempotencyKeyAndPagoId("restitucion:" + REVERSION, 1L)).thenReturn(true);

        servicio.restituirApartadas(pago, 10, REVERSION);

        assertThat(billetera.getHorasDisponibles()).isEqualTo(14);
        verify(movimientoRepository, never()).save(any());
    }
}
