package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.config.PagosProperties;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.TransaccionPasarelaRepository;
import com.drivehub.pagos.features.pagos.service.PagoService;
import com.drivehub.pagos.features.pagos.service.PagosVencidosJob;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;
import com.drivehub.pagos.features.planes.service.ConversionMoneda;

/**
 * El vencimiento de los pagos pendientes (CU26): con la hora fijada por un reloj de prueba.
 */
@ExtendWith(MockitoExtension.class)
class PagoVencimientoTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private TransaccionPasarelaRepository transaccionRepository;
    @Mock
    private PlanPagoRepository planRepository;
    @Mock
    private PasarelaPago pasarela;
    @Mock
    private IdentidadClient identidad;
    @Mock
    private BearerActual bearer;

    private PagoService servicio;

    @BeforeEach
    void preparar() {
        PagosProperties propiedades = PagosPropertiesPrueba.con(PagosPropertiesPrueba.TIPO_CAMBIO);
        servicio = new PagoService(pagoRepository, transaccionRepository, planRepository,
            new ConversionMoneda(propiedades), propiedades, pasarela, identidad, bearer,
            Clock.fixed(AHORA, ZoneOffset.UTC), mock(PlatformTransactionManager.class));
    }

    private static Pago pago(long id, EstadoPago estado) {
        PlanPago plan = new PlanPago(UUID.randomUUID(), "Plan", (short) 10, new BigDecimal("450.00"));
        Pago pago = new Pago(plan.getEscuelaCodigo(), UUID.randomUUID(), plan, new BigDecimal("38.23"),
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO), "Ana", "Escuela", "clave-" + id, AHORA.minusSeconds(3600));
        pago.setEstado(estado);
        ReflectionTestUtils.setField(pago, "id", id);
        return pago;
    }

    @Test
    void vencerPendientes_pasaAFallidoLosVencidos_yDevuelveCuantos() {
        Pago a = pago(1, EstadoPago.PENDIENTE);
        Pago b = pago(2, EstadoPago.PENDIENTE);
        when(pagoRepository.idsVencidos(eq(EstadoPago.PENDIENTE), any(), any(Pageable.class)))
            .thenReturn(List.of(1L, 2L)).thenReturn(List.of());
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(a));
        when(pagoRepository.findById(2L)).thenReturn(Optional.of(b));

        int cantidad = servicio.vencerPendientes(AHORA);

        assertThat(cantidad).isEqualTo(2);
        assertThat(a.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        assertThat(b.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        verify(pagoRepository).saveAndFlush(a);
        verify(pagoRepository).saveAndFlush(b);
    }

    @Test
    void vencerPendientes_esperaElMargenDespuesDelVencimiento() {
        when(pagoRepository.idsVencidos(any(), any(), any(Pageable.class))).thenReturn(List.of());

        servicio.vencerPendientes(AHORA);

        ArgumentCaptor<Instant> limite = ArgumentCaptor.forClass(Instant.class);
        verify(pagoRepository).idsVencidos(eq(EstadoPago.PENDIENTE), limite.capture(), any(Pageable.class));
        // * El margen por defecto es de 10 minutos: lo que venció hace menos que eso todavía espera
        assertThat(limite.getValue()).isEqualTo(AHORA.minusSeconds(600));
    }

    @Test
    void vencerPendientes_unPagoQueYaNoEstaPendienteNoSeToca() {
        Pago confirmado = pago(1, EstadoPago.CONFIRMADO);
        when(pagoRepository.idsVencidos(any(), any(), any(Pageable.class))).thenReturn(List.of(1L), List.of(1L));
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(confirmado));

        int cantidad = servicio.vencerPendientes(AHORA);

        assertThat(cantidad).isZero();
        assertThat(confirmado.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @Test
    void vencerPendientes_siUnPagoFalla_sigueConLosDemas_yNoLoReintentaEnLaMismaVuelta() {
        Pago malo = pago(1, EstadoPago.PENDIENTE);
        Pago bueno = pago(2, EstadoPago.PENDIENTE);
        when(pagoRepository.idsVencidos(any(), any(), any(Pageable.class)))
            .thenReturn(List.of(1L, 2L), List.of(1L, 2L));
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(malo));
        when(pagoRepository.findById(2L)).thenReturn(Optional.of(bueno));
        when(pagoRepository.saveAndFlush(malo)).thenThrow(new IllegalStateException("fallo de escritura"));

        int cantidad = servicio.vencerPendientes(AHORA);

        assertThat(cantidad).isEqualTo(1);
        assertThat(bueno.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        verify(pagoRepository).saveAndFlush(malo);
    }

    @Test
    void vencerPendientes_sinVencidos_devuelveCero() {
        when(pagoRepository.idsVencidos(any(), any(), any(Pageable.class))).thenReturn(List.of());

        assertThat(servicio.vencerPendientes(AHORA)).isZero();
    }

    // ------------------------------------------------------------------ el proceso

    @Test
    void elProceso_llamaAlServiceConLaHoraDelReloj() {
        PagoService servicioFalso = mock(PagoService.class);
        PagosVencidosJob job = new PagosVencidosJob(servicioFalso, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(servicioFalso.vencerPendientes(AHORA)).thenReturn(3);

        job.ejecutar();

        verify(servicioFalso).vencerPendientes(AHORA);
    }

    @Test
    void elProceso_unErrorNoLoDetiene() {
        PagoService servicioFalso = mock(PagoService.class);
        PagosVencidosJob job = new PagosVencidosJob(servicioFalso, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(servicioFalso.vencerPendientes(any())).thenThrow(new IllegalStateException("base caída"))
            .thenReturn(1);

        job.ejecutar();
        job.ejecutar();

        verify(servicioFalso, times(2)).vencerPendientes(AHORA);
    }
}
