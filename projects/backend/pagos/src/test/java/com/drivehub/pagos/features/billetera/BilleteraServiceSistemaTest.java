package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.pagos.common.exception.BusinessException;
import com.drivehub.pagos.features.billetera.dto.ConsumoResultado;
import com.drivehub.pagos.features.billetera.dto.DevolucionResultado;
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
 * Descontar y devolver horas SIN sesión (CU44): el orquestador de Académico no tiene rol, pero las reglas de saldo y de
 * idempotencia son las mismas que las de `consumir` y `devolver`.
 */
@ExtendWith(MockitoExtension.class)
class BilleteraServiceSistemaTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CLASE = UUID.randomUUID();

    @Mock
    private BilleteraRepository billeteraRepository;
    @Mock
    private MovimientoHorasRepository movimientoRepository;
    @Mock
    private PagoRepository pagoRepository;

    private BilleteraService servicio;
    private BilleteraHoras billetera;

    @BeforeEach
    void preparar() {
        servicio = new BilleteraService(billeteraRepository, movimientoRepository, pagoRepository);
        billetera = new BilleteraHoras(ESCUELA, ESTUDIANTE);
        ReflectionTestUtils.setField(billetera, "id", 1L);
    }

    private void conSaldo(int saldo) {
        billetera.setHorasDisponibles(saldo);
        when(billeteraRepository.buscarParaActualizar(ESCUELA, ESTUDIANTE)).thenReturn(Optional.of(billetera));
    }

    private static Pago compra(long id, int restantes) {
        PlanPago plan = new PlanPago(ESCUELA, "Plan", (short) 10, new BigDecimal("450.00"));
        Pago pago = new Pago(ESCUELA, ESTUDIANTE, plan, new BigDecimal("38.23"), new BigDecimal("11.77"), "Ana",
            "Escuela", "clave-" + id, Instant.now());
        ReflectionTestUtils.setField(pago, "id", id);
        ReflectionTestUtils.setField(pago, "codigo", UUID.randomUUID());
        pago.setHorasRestantes((short) restantes);
        return pago;
    }

    // ------------------------------------------------------------------ consumirDeSistema

    @Test
    void consumirDeSistema_descuentaDeLaCompraYDelSaldo_igualQueConsumir() {
        conSaldo(10);
        Pago compra = compra(1, 10);
        when(pagoRepository.confirmadosConHorasRestantes(ESCUELA, ESTUDIANTE)).thenReturn(List.of(compra));

        ConsumoResultado resultado = servicio.consumirDeSistema(ESCUELA, ESTUDIANTE, 1, INSTRUCTOR, CLASE,
            "reserva-1");

        assertThat(resultado.horasDescontadas()).isEqualTo(1);
        assertThat(resultado.horasDisponibles()).isEqualTo(9);
        assertThat(compra.getHorasRestantes()).isEqualTo((short) 9);
        verify(movimientoRepository).save(any(MovimientoHoras.class));
    }

    @Test
    void consumirDeSistema_conLaMismaClave_devuelveLoAnteriorSinDescontarDeNuevo() {
        conSaldo(9);
        Pago compra = compra(1, 9);
        MovimientoHoras previo = new MovimientoHoras(billetera, compra, TipoMovimiento.CONSUMO, 1, INSTRUCTOR, CLASE,
            "reserva-1");
        when(movimientoRepository.findByBilleteraHorasIdAndIdempotencyKeyAndTipo(1L, "reserva-1",
            TipoMovimiento.CONSUMO)).thenReturn(List.of(previo));

        ConsumoResultado resultado = servicio.consumirDeSistema(ESCUELA, ESTUDIANTE, 1, INSTRUCTOR, CLASE,
            "reserva-1");

        assertThat(resultado.horasDescontadas()).isEqualTo(1);
        assertThat(resultado.horasDisponibles()).isEqualTo(9);
        assertThat(billetera.getHorasDisponibles()).isEqualTo(9);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void consumirDeSistema_sinHorasSuficientes_esErrPag09_yNoCambiaNada() {
        conSaldo(0);

        assertThatThrownBy(() -> servicio.consumirDeSistema(ESCUELA, ESTUDIANTE, 1, INSTRUCTOR, CLASE, "reserva-1"))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_09"));
        assertThat(billetera.getHorasDisponibles()).isZero();
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void consumirDeSistema_conUnaClaveInvalida_esErrSys01() {
        assertThatThrownBy(() -> servicio.consumirDeSistema(ESCUELA, ESTUDIANTE, 1, INSTRUCTOR, CLASE, " "))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
    }

    // ------------------------------------------------------------------ devolverDeSistema

    @Test
    void devolverDeSistema_devuelveLoQueEsaClaseGasto_alasMismasCompras() {
        conSaldo(9);
        Pago compra = compra(1, 9);
        MovimientoHoras consumo = new MovimientoHoras(billetera, compra, TipoMovimiento.CONSUMO, 1, INSTRUCTOR,
            CLASE, "reserva-1");
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.CONSUMO))
            .thenReturn(List.of(consumo));
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.DEVOLUCION))
            .thenReturn(List.of());
        when(pagoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(compra));

        DevolucionResultado resultado = servicio.devolverDeSistema(ESCUELA, ESTUDIANTE, CLASE, "devolucion-1");

        assertThat(resultado.horasDevueltas()).isEqualTo(1);
        assertThat(resultado.horasDisponibles()).isEqualTo(10);
        assertThat(compra.getHorasRestantes()).isEqualTo((short) 10);
    }

    @Test
    void devolverDeSistema_conLaMismaClave_noDevuelveDosVeces() {
        conSaldo(10);
        Pago compra = compra(1, 10);
        MovimientoHoras previa = new MovimientoHoras(billetera, compra, TipoMovimiento.DEVOLUCION, 1, INSTRUCTOR,
            CLASE, "devolucion-1");
        when(movimientoRepository.findByBilleteraHorasIdAndIdempotencyKeyAndTipo(1L, "devolucion-1",
            TipoMovimiento.DEVOLUCION)).thenReturn(List.of(previa));

        DevolucionResultado resultado = servicio.devolverDeSistema(ESCUELA, ESTUDIANTE, CLASE, "devolucion-1");

        assertThat(resultado.horasDevueltas()).isEqualTo(1);
        assertThat(billetera.getHorasDisponibles()).isEqualTo(10);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void devolverDeSistema_deUnaClaseQueNoGasto_esErrPag16() {
        conSaldo(10);
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.CONSUMO))
            .thenReturn(List.of());
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.DEVOLUCION))
            .thenReturn(List.of());

        assertThatThrownBy(() -> servicio.devolverDeSistema(ESCUELA, ESTUDIANTE, CLASE, "devolucion-1"))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_16"));
    }

    // ------------------------------------------------------------------ la autorización no cambió

    private static Method metodo(String nombre) {
        for (Method m : BilleteraService.class.getDeclaredMethods()) {
            if (m.getName().equals(nombre) && java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                return m;
            }
        }
        throw new AssertionError("No existe el método público " + nombre);
    }

    @Test
    void consumirYDevolver_conservanSuPreAuthorize_yLasVersionesDeSistemaNoLoTienen() {
        assertThat(metodo("consumir").getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(metodo("devolver").getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(metodo("consumirDeSistema").getAnnotation(PreAuthorize.class)).isNull();
        assertThat(metodo("devolverDeSistema").getAnnotation(PreAuthorize.class)).isNull();
    }
}
