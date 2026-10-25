package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.pagos.common.exception.BusinessException;
import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.ValidationException;
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

@ExtendWith(MockitoExtension.class)
class BilleteraServiceConsumoTest {

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

    private BilleteraHoras conSaldo(int saldo) {
        billetera.setHorasDisponibles(saldo);
        when(billeteraRepository.buscarParaActualizar(ESCUELA, ESTUDIANTE)).thenReturn(Optional.of(billetera));
        return billetera;
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

    private static MovimientoHoras movimiento(BilleteraHoras billetera, Pago pago, TipoMovimiento tipo, int horas,
            String clave) {
        return new MovimientoHoras(billetera, pago, tipo, horas, INSTRUCTOR, CLASE, clave);
    }

    private ConsumoResultado consumir(int horas, String clave) {
        return servicio.consumir(ESCUELA, ESTUDIANTE, ESTUDIANTE, horas, INSTRUCTOR, CLASE, clave);
    }

    private static BusinessException error(Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException e) {
            return e;
        }
        throw new AssertionError("Se esperaba una BusinessException");
    }

    // ------------------------------------------------------------------ consumir

    @Test
    void consumir_deUnaSolaCompra_descuentaDeSusHorasRestantesYDelSaldo() {
        conSaldo(10);
        Pago compra = compra(1, 10);
        when(pagoRepository.confirmadosConHorasRestantes(ESCUELA, ESTUDIANTE)).thenReturn(List.of(compra));

        ConsumoResultado resultado = consumir(3, "clave-1");

        assertThat(resultado.horasDescontadas()).isEqualTo(3);
        assertThat(resultado.horasDisponibles()).isEqualTo(7);
        assertThat(compra.getHorasRestantes()).isEqualTo((short) 7);
        assertThat(billetera.getHorasDisponibles()).isEqualTo(7);
        ArgumentCaptor<MovimientoHoras> guardado = ArgumentCaptor.forClass(MovimientoHoras.class);
        verify(movimientoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getTipo()).isEqualTo(TipoMovimiento.CONSUMO);
        assertThat(guardado.getValue().getPago()).isSameAs(compra);
        assertThat(guardado.getValue().getHoras()).isEqualTo((short) 3);
        assertThat(guardado.getValue().getInstructorCodigo()).isEqualTo(INSTRUCTOR);
        assertThat(guardado.getValue().getClaseCodigo()).isEqualTo(CLASE);
        assertThat(guardado.getValue().getIdempotencyKey()).isEqualTo("clave-1");
    }

    @Test
    void consumir_cruzandoDosCompras_tomaDeLaMasAntiguaPrimero_yGeneraUnMovimientoPorCompra() {
        conSaldo(12);
        Pago antigua = compra(1, 2);
        Pago reciente = compra(2, 10);
        when(pagoRepository.confirmadosConHorasRestantes(ESCUELA, ESTUDIANTE)).thenReturn(List.of(antigua, reciente));

        ConsumoResultado resultado = consumir(5, "clave-1");

        assertThat(resultado.horasDescontadas()).isEqualTo(5);
        assertThat(antigua.getHorasRestantes()).isEqualTo((short) 0);
        assertThat(reciente.getHorasRestantes()).isEqualTo((short) 7);
        ArgumentCaptor<MovimientoHoras> guardados = ArgumentCaptor.forClass(MovimientoHoras.class);
        verify(movimientoRepository, times(2)).save(guardados.capture());
        assertThat(guardados.getAllValues()).extracting(m -> m.getPago().getId()).containsExactly(1L, 2L);
        assertThat(guardados.getAllValues()).extracting(MovimientoHoras::getHoras)
            .containsExactly((short) 2, (short) 3);
    }

    @Test
    void consumir_unaCompraAgotadaSeSalta() {
        conSaldo(4);
        Pago agotada = compra(1, 0);
        Pago conHoras = compra(2, 4);
        when(pagoRepository.confirmadosConHorasRestantes(ESCUELA, ESTUDIANTE)).thenReturn(List.of(agotada, conHoras));

        consumir(4, "clave-1");

        assertThat(conHoras.getHorasRestantes()).isEqualTo((short) 0);
        verify(movimientoRepository, times(1)).save(any());
    }

    @Test
    void consumir_todoElSaldo_loDejaEnCero() {
        conSaldo(4);
        when(pagoRepository.confirmadosConHorasRestantes(ESCUELA, ESTUDIANTE)).thenReturn(List.of(compra(1, 4)));

        assertThat(consumir(4, "clave-1").horasDisponibles()).isZero();
    }

    @Test
    void consumir_conMenosSaldoDelPedido_esErrPag09_yNoCambiaNada() {
        conSaldo(2);

        BusinessException ex = error(() -> consumir(3, "clave-1"));

        assertThat(ex).isInstanceOf(ConflictException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_09");
        assertThat(billetera.getHorasDisponibles()).isEqualTo(2);
        verify(movimientoRepository, never()).save(any());
        verify(billeteraRepository, never()).save(any());
    }

    @Test
    void consumir_sinBilletera_esErrPag09() {
        when(billeteraRepository.buscarParaActualizar(ESCUELA, ESTUDIANTE)).thenReturn(Optional.empty());

        assertThat(error(() -> consumir(1, "clave-1")).getErrorCode()).isEqualTo("ERR_PAG_09");
    }

    @Test
    void consumir_siElSaldoNoCuadraConLasCompras_noDescuentaNada() {
        conSaldo(5);
        when(pagoRepository.confirmadosConHorasRestantes(ESCUELA, ESTUDIANTE)).thenReturn(List.of(compra(1, 3)));

        BusinessException ex = error(() -> consumir(5, "clave-1"));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_09");
        verify(billeteraRepository, never()).save(any());
    }

    @Test
    void consumir_conLaMismaClave_devuelveLoQueYaDescontoSinDescontarDeNuevo() {
        conSaldo(7);
        Pago a = compra(1, 0);
        Pago b = compra(2, 7);
        when(movimientoRepository.findByBilleteraHorasIdAndIdempotencyKeyAndTipo(1L, "clave-1",
            TipoMovimiento.CONSUMO)).thenReturn(List.of(
            movimiento(billetera, a, TipoMovimiento.CONSUMO, 2, "clave-1"),
            movimiento(billetera, b, TipoMovimiento.CONSUMO, 1, "clave-1")));

        ConsumoResultado resultado = consumir(3, "clave-1");

        assertThat(resultado.horasDescontadas()).isEqualTo(3);
        assertThat(resultado.horasDisponibles()).isEqualTo(7);
        verify(movimientoRepository, never()).save(any());
        verify(pagoRepository, never()).confirmadosConHorasRestantes(any(), any());
    }

    @Test
    void consumir_validaLosDatos_antesDeTocarLaBase() {
        for (int horas : new int[] {0, -1, 501}) {
            BusinessException ex = error(() -> consumir(horas, "clave-1"));
            assertThat(ex).isInstanceOf(ValidationException.class);
            assertThat(ex.getErrorCode()).isEqualTo("ERR_SYS_01");
        }
        for (String clave : new String[] {null, "", "  ", "x".repeat(65)}) {
            assertThat(error(() -> consumir(1, clave)).getErrorCode()).isEqualTo("ERR_SYS_01");
        }
        assertThat(error(() -> servicio.consumir(ESCUELA, ESTUDIANTE, ESTUDIANTE, 1, null, CLASE, "c"))
            .getErrorCode()).isEqualTo("ERR_SYS_01");
        assertThat(error(() -> servicio.consumir(ESCUELA, ESTUDIANTE, ESTUDIANTE, 1, INSTRUCTOR, null, "c"))
            .getErrorCode()).isEqualTo("ERR_SYS_01");
        verify(billeteraRepository, never()).buscarParaActualizar(any(), any());
    }

    // ------------------------------------------------------------------ devolver

    private DevolucionResultado devolver(String clave) {
        return servicio.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, CLASE, clave);
    }

    @Test
    void devolver_restituyeALasMismasComprasConElMismoInstructorYClase() {
        conSaldo(5);
        Pago a = compra(1, 0);
        Pago b = compra(2, 5);
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.CONSUMO))
            .thenReturn(List.of(movimiento(billetera, a, TipoMovimiento.CONSUMO, 2, "c"),
                movimiento(billetera, b, TipoMovimiento.CONSUMO, 3, "c")));
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.DEVOLUCION))
            .thenReturn(List.of());
        when(pagoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(a));
        when(pagoRepository.buscarParaActualizar(2L)).thenReturn(Optional.of(b));

        DevolucionResultado resultado = devolver("clave-d");

        assertThat(resultado.horasDevueltas()).isEqualTo(5);
        assertThat(resultado.horasDisponibles()).isEqualTo(10);
        assertThat(a.getHorasRestantes()).isEqualTo((short) 2);
        assertThat(b.getHorasRestantes()).isEqualTo((short) 8);
        ArgumentCaptor<MovimientoHoras> guardados = ArgumentCaptor.forClass(MovimientoHoras.class);
        verify(movimientoRepository, times(2)).save(guardados.capture());
        assertThat(guardados.getAllValues()).allSatisfy(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimiento.DEVOLUCION);
            assertThat(m.getInstructorCodigo()).isEqualTo(INSTRUCTOR);
            assertThat(m.getClaseCodigo()).isEqualTo(CLASE);
            assertThat(m.getIdempotencyKey()).isEqualTo("clave-d");
        });
    }

    @Test
    void devolver_resta_loQueYaSeDevolvio() {
        conSaldo(5);
        Pago a = compra(1, 4);
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.CONSUMO))
            .thenReturn(List.of(movimiento(billetera, a, TipoMovimiento.CONSUMO, 4, "c")));
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.DEVOLUCION))
            .thenReturn(List.of(movimiento(billetera, a, TipoMovimiento.DEVOLUCION, 1, "antes")));
        when(pagoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(a));

        assertThat(devolver("clave-d").horasDevueltas()).isEqualTo(3);
    }

    @Test
    void devolver_sinNadaParaDevolver_esErrPag16() {
        conSaldo(5);
        Pago a = compra(1, 4);
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.CONSUMO))
            .thenReturn(List.of(movimiento(billetera, a, TipoMovimiento.CONSUMO, 4, "c")));
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.DEVOLUCION))
            .thenReturn(List.of(movimiento(billetera, a, TipoMovimiento.DEVOLUCION, 4, "antes")));

        BusinessException ex = error(() -> devolver("clave-d"));

        assertThat(ex).isInstanceOf(NotFoundException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_16");
    }

    @Test
    void devolver_unaClaseQueNuncaGasto_oSinBilletera_esErrPag16() {
        conSaldo(5);
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.CONSUMO))
            .thenReturn(List.of());
        when(movimientoRepository.findByBilleteraHorasIdAndClaseCodigoAndTipo(1L, CLASE, TipoMovimiento.DEVOLUCION))
            .thenReturn(List.of());
        assertThat(error(() -> devolver("clave-d")).getErrorCode()).isEqualTo("ERR_PAG_16");

        when(billeteraRepository.buscarParaActualizar(ESCUELA, ESTUDIANTE)).thenReturn(Optional.empty());
        assertThat(error(() -> devolver("clave-d")).getErrorCode()).isEqualTo("ERR_PAG_16");
    }

    @Test
    void devolver_conLaMismaClave_devuelveLoQueYaDevolvioSinDevolverDeNuevo() {
        conSaldo(9);
        Pago a = compra(1, 9);
        when(movimientoRepository.findByBilleteraHorasIdAndIdempotencyKeyAndTipo(1L, "clave-d",
            TipoMovimiento.DEVOLUCION)).thenReturn(List.of(
            movimiento(billetera, a, TipoMovimiento.DEVOLUCION, 4, "clave-d")));

        DevolucionResultado resultado = devolver("clave-d");

        assertThat(resultado.horasDevueltas()).isEqualTo(4);
        assertThat(resultado.horasDisponibles()).isEqualTo(9);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void devolver_validaLaClaveYLaClase() {
        assertThat(error(() -> devolver(null)).getErrorCode()).isEqualTo("ERR_SYS_01");
        assertThat(error(() -> devolver("x".repeat(65))).getErrorCode()).isEqualTo("ERR_SYS_01");
        assertThat(error(() -> servicio.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, null, "c")).getErrorCode())
            .isEqualTo("ERR_SYS_01");
        verify(billeteraRepository, never()).buscarParaActualizar(any(), any());
    }
}
