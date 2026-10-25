package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.enums.OrigenReversion;
import com.drivehub.pagos.features.pagos.model.ReversionPago;
import com.drivehub.pagos.features.pagos.pasarela.DevolucionCreada;
import com.drivehub.pagos.features.pagos.pasarela.SolicitudDevolucion;
import com.drivehub.pagos.features.pagos.service.evento.MotivoCompensacion;

/**
 * La compensación automática del cobro completo (CU31) y su reintento, con todo mockeado.
 */
class ReversionServiceCompensacionTest extends ReversionServiceTestBase {

    private void laPasarelaCrea() {
        when(pasarela.reembolsar(any(SolicitudDevolucion.class)))
            .thenReturn(new DevolucionCreada("re_test_1", "pending"));
    }

    private void laPasarelaFalla() {
        when(pasarela.reembolsar(any(SolicitudDevolucion.class)))
            .thenThrow(new ServicioNoDisponibleException("ERR_PAG_06", "sin pasarela"));
    }

    @Test
    void cadaMotivo_creaUnaReversionDelCienPorCiento_conRetencionCero() {
        laPasarelaCrea();
        pago.setEstado(EstadoPago.FALLIDO);

        for (MotivoCompensacion motivo : MotivoCompensacion.values()) {
            guardadas.clear();
            servicio.compensar(pago.getCodigo(), motivo, COBRO);

            assertThat(guardadas).singleElement().satisfies(r -> {
                assertThat(r.getOrigen()).isEqualTo(OrigenReversion.SAGA);
                assertThat(r.getMotivo()).isEqualTo(motivo.name());
                assertThat(r.getMotivoDetalle()).isNull();
                assertThat(r.getMonto()).isEqualByComparingTo("38.23");
                assertThat(r.getMontoRetenido()).isEqualByComparingTo("0");
                assertThat(r.getEstado()).isEqualTo(EstadoReversion.SOLICITADA);
                assertThat(r.getReferenciaExterna()).isEqualTo("re_test_1");
            });
        }
        ArgumentCaptor<SolicitudDevolucion> pedido = ArgumentCaptor.forClass(SolicitudDevolucion.class);
        verify(pasarela, times(MotivoCompensacion.values().length)).reembolsar(pedido.capture());
        assertThat(pedido.getValue().montoCentavosUsd()).isEqualTo(3823L);
        assertThat(pedido.getValue().cobroReferencia()).isEqualTo(COBRO);
        verify(billetera, never()).apartarPorReembolso(any(), any());
    }

    @Test
    void elEventoRepetido_conUnaReversionVigente_noCreaOtra() {
        when(reversiones.existsByPagoIdAndEstadoNot(pago.getId(), EstadoReversion.FALLIDA)).thenReturn(true);

        servicio.compensar(pago.getCodigo(), MotivoCompensacion.MONTO_DISTINTO, COBRO);

        assertThat(guardadas).isEmpty();
        verifyNoInteractions(pasarela);
    }

    @Test
    void unPagoQueNoExiste_seRegistraYNoRompe() {
        servicio.compensar(java.util.UUID.randomUUID(), MotivoCompensacion.COBRO_TARDIO, COBRO);

        assertThat(guardadas).isEmpty();
        assertThat(huboRegistro("WARN")).isTrue();
    }

    @Test
    void siLaViolacionDelIndiceUnicoGanaOtroHilo_noHaceNada() {
        when(reversiones.saveAndFlush(any(ReversionPago.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_reversion_pago_vigente"));

        servicio.compensar(pago.getCodigo(), MotivoCompensacion.MONTO_DISTINTO, COBRO);

        verifyNoInteractions(pasarela);
    }

    @Test
    void siLaPasarelaFalla_laReversionQuedaPendienteSinExcepcion_yRegistraWarn() {
        laPasarelaFalla();

        servicio.compensar(pago.getCodigo(), MotivoCompensacion.ACREDITACION_FALLIDA, COBRO);

        assertThat(guardadas).singleElement().satisfies(r -> {
            assertThat(r.getEstado()).isEqualTo(EstadoReversion.SOLICITADA);
            assertThat(r.getReferenciaExterna()).isNull();
        });
        assertThat(huboRegistro("WARN")).isTrue();
    }

    @Test
    void unCobroTardio_guardaElCobroEnLaTransaccionSiNoLoTenia() {
        transaccionDelCobro.setRespuesta(null);
        laPasarelaCrea();
        pago.setEstado(EstadoPago.FALLIDO);

        servicio.compensar(pago.getCodigo(), MotivoCompensacion.COBRO_TARDIO, "pi_tardio");

        assertThat(transaccionDelCobro.getRespuesta()).contains("pi_tardio");
        ArgumentCaptor<SolicitudDevolucion> pedido = ArgumentCaptor.forClass(SolicitudDevolucion.class);
        verify(pasarela).reembolsar(pedido.capture());
        assertThat(pedido.getValue().cobroReferencia()).isEqualTo("pi_tardio");
    }

    // ------------------------------------------------------------------ reintento

    private ReversionPago pendiente() {
        laPasarelaFalla();
        servicio.compensar(pago.getCodigo(), MotivoCompensacion.ACREDITACION_FALLIDA, COBRO);
        ReversionPago reversion = guardadas.get(0);
        when(reversiones.idsDeCompensacionesPendientes(any(Pageable.class))).thenReturn(List.of(reversion.getId()));
        return reversion;
    }

    @Test
    void elReintento_laCompleta_conLaMismaClave_yNoDuplica() {
        ReversionPago reversion = pendiente();
        laPasarelaCrea();

        int logradas = servicio.reintentarCompensacionesPendientes();

        assertThat(logradas).isEqualTo(1);
        assertThat(reversion.getReferenciaExterna()).isEqualTo("re_test_1");
        ArgumentCaptor<SolicitudDevolucion> pedido = ArgumentCaptor.forClass(SolicitudDevolucion.class);
        verify(pasarela, times(2)).reembolsar(pedido.capture());
        assertThat(pedido.getAllValues()).extracting(SolicitudDevolucion::idempotencyKey)
            .containsOnly(reversion.getCodigo().toString());
        assertThat(guardadas).hasSize(1);

        // * Con la devolución ya creada, la próxima vuelta no la encuentra pendiente
        when(reversiones.idsDeCompensacionesPendientes(any(Pageable.class))).thenReturn(List.of(reversion.getId()));
        assertThat(servicio.reintentarCompensacionesPendientes()).isZero();
        verify(pasarela, times(2)).reembolsar(any());
    }

    @Test
    void siLaPasarelaSigueCaida_elReintentoNoLograNadaYNoRompe() {
        pendiente();

        assertThat(servicio.reintentarCompensacionesPendientes()).isZero();
        assertThat(huboRegistro("WARN")).isTrue();
    }

    @Test
    void conMasDe24HorasSinLograrse_seRegistraError() {
        pendiente();
        reloj = reloj.plus(Duration.ofHours(25));

        servicio.reintentarCompensacionesPendientes();

        assertThat(huboRegistro("ERROR")).isTrue();
    }

    @Test
    void conMenosDe24Horas_noSeRegistraError() {
        pendiente();
        reloj = reloj.plus(Duration.ofHours(2));

        servicio.reintentarCompensacionesPendientes();

        assertThat(huboRegistro("ERROR")).isFalse();
    }

    @Test
    void sinPendientes_noHaceNada() {
        when(reversiones.idsDeCompensacionesPendientes(any(Pageable.class))).thenReturn(List.of());

        assertThat(servicio.reintentarCompensacionesPendientes()).isZero();
        verifyNoInteractions(pasarela);
    }
}
