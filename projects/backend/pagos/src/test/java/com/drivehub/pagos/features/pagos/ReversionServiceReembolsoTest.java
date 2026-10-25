package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.features.pagos.dto.PagoDetalleResponse;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.enums.MotivoReembolso;
import com.drivehub.pagos.features.pagos.enums.OrigenReversion;
import com.drivehub.pagos.features.pagos.model.ReversionPago;
import com.drivehub.pagos.features.pagos.pasarela.DevolucionCreada;
import com.drivehub.pagos.features.pagos.pasarela.SolicitudDevolucion;

/**
 * El reembolso que pide el estudiante (CU31) con repositorios, billetera y pasarela mockeados.
 */
class ReversionServiceReembolsoTest extends ReversionServiceTestBase {

    private PagoDetalleResponse pedir(MotivoReembolso motivo, String detalle) {
        return servicio.solicitarReembolso(ESCUELA, ESTUDIANTE, pago.getCodigo(), motivo, detalle);
    }

    private void laPasarelaCrea() {
        when(pasarela.reembolsar(any(SolicitudDevolucion.class)))
            .thenReturn(new DevolucionCreada("re_test_1", "pending"));
    }

    @Test
    void reembolsoFeliz_devuelveElNoventaYCincoPorCiento_apartaLasHoras_yQuedaSolicitada() {
        laPasarelaCrea();
        PagoDetalleResponse detalle = mock();
        when(consulta.detalle(ESCUELA, ESTUDIANTE, pago.getCodigo())).thenReturn(detalle);

        PagoDetalleResponse respuesta = pedir(MotivoReembolso.MUDANZA, null);

        assertThat(respuesta).isSameAs(detalle);
        assertThat(guardadas).hasSize(1);
        ReversionPago reversion = guardadas.get(0);
        assertThat(reversion.getOrigen()).isEqualTo(OrigenReversion.ESTUDIANTE);
        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.SOLICITADA);
        assertThat(reversion.getMotivo()).isEqualTo("MUDANZA");
        assertThat(reversion.getMotivoDetalle()).isNull();
        assertThat(reversion.getMonto()).isEqualByComparingTo("36.32");
        assertThat(reversion.getMontoRetenido()).isEqualByComparingTo("1.91");
        assertThat(reversion.getReferenciaExterna()).isEqualTo("re_test_1");
        verify(billetera).apartarPorReembolso(pago, reversion.getCodigo());
        ArgumentCaptor<SolicitudDevolucion> pedido = ArgumentCaptor.forClass(SolicitudDevolucion.class);
        verify(pasarela).reembolsar(pedido.capture());
        assertThat(pedido.getValue().cobroReferencia()).isEqualTo(COBRO);
        assertThat(pedido.getValue().montoCentavosUsd()).isEqualTo(3632L);
        assertThat(pedido.getValue().idempotencyKey()).isEqualTo(reversion.getCodigo().toString());
        assertThat(pedido.getValue().pagoCodigo()).isEqualTo(pago.getCodigo());
        assertThat(pedido.getValue().reversionCodigo()).isEqualTo(reversion.getCodigo());
        assertThat(transaccionesGuardadas).singleElement().satisfies(t -> {
            assertThat(t.getReferenciaExterna()).isEqualTo("re_test_1");
            assertThat(t.getProveedor()).isEqualTo("STRIPE_REEMBOLSO");
            assertThat(t.getEstado()).isEqualTo("SOLICITADA");
        });
        assertThat(huboRegistro("INFO")).isTrue();
    }

    @Test
    void conOtro_guardaElDetalleRecortado() {
        laPasarelaCrea();

        pedir(MotivoReembolso.OTRO, "   Se cerró mi trabajo   ");

        assertThat(guardadas.get(0).getMotivo()).isEqualTo("OTRO");
        assertThat(guardadas.get(0).getMotivoDetalle()).isEqualTo("Se cerró mi trabajo");
    }

    @Test
    void conUnMotivoQueNoEsOtro_descartaElDetalle() {
        laPasarelaCrea();

        pedir(MotivoReembolso.SIN_TIEMPO, "esto no se guarda");

        assertThat(guardadas.get(0).getMotivoDetalle()).isNull();
    }

    @Test
    void sinMotivo_esErrSys01_sinTocarNada() {
        assertThatThrownBy(() -> pedir(null, null)).isInstanceOfSatisfying(ValidationException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        verifyNoInteractions(pagos, reversiones, billetera, pasarela);
    }

    @Test
    void otroSinDetalleOConDetalleDeLargoInvalido_esErrSys01_sinTocarNada() {
        for (String detalle : new String[] {null, "", "    ", "abcd", "x".repeat(201)}) {
            assertThatThrownBy(() -> pedir(MotivoReembolso.OTRO, detalle))
                .isInstanceOfSatisfying(ValidationException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        }
        verifyNoInteractions(pagos, reversiones, billetera, pasarela);
    }

    @Test
    void elDetalleEnLosLimites_seAcepta() {
        laPasarelaCrea();

        pedir(MotivoReembolso.OTRO, "x".repeat(5));
        guardadas.clear();
        pago.setEstado(EstadoPago.CONFIRMADO);
        pedir(MotivoReembolso.OTRO, "x".repeat(200));

        assertThat(guardadas).hasSize(1);
    }

    @Test
    void unPagoDeOtraPersonaOInexistente_esErrPag07() {
        UUID ajeno = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.solicitarReembolso(ESCUELA, ESTUDIANTE, ajeno, MotivoReembolso.MUDANZA,
            null)).isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_07"));
        verifyNoInteractions(pasarela);
        verify(billetera, never()).apartarPorReembolso(any(), any());
    }

    @Test
    void unPagoQueNoEstaConfirmado_esErrPag10() {
        for (EstadoPago estado : new EstadoPago[] {EstadoPago.PENDIENTE, EstadoPago.FALLIDO, EstadoPago.REVERTIDO}) {
            pago.setEstado(estado);
            assertThatThrownBy(() -> pedir(MotivoReembolso.MUDANZA, null))
                .isInstanceOfSatisfying(ConflictException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_10"));
        }
        verifyNoInteractions(pasarela);
        assertThat(guardadas).isEmpty();
    }

    @Test
    void conUnaReversionYaVigente_esErrPag10() {
        when(reversiones.existsByPagoIdAndEstadoNot(pago.getId(), EstadoReversion.FALLIDA)).thenReturn(true);

        assertThatThrownBy(() -> pedir(MotivoReembolso.MUDANZA, null)).isInstanceOfSatisfying(
            ConflictException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_10"));
        verifyNoInteractions(pasarela);
        verify(billetera, never()).apartarPorReembolso(any(), any());
    }

    @Test
    void siOtroPedidoGanoLaCarrera_laViolacionDelIndiceUnicoEsErrPag10() {
        when(reversiones.saveAndFlush(any(ReversionPago.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_reversion_pago_vigente"));

        assertThatThrownBy(() -> pedir(MotivoReembolso.MUDANZA, null)).isInstanceOfSatisfying(
            ConflictException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_10"));
        verifyNoInteractions(pasarela);
    }

    @Test
    void conUnaHoraUsada_esErrPag11() {
        pago.setHorasRestantes((short) 9);

        assertThatThrownBy(() -> pedir(MotivoReembolso.MUDANZA, null)).isInstanceOfSatisfying(
            ConflictException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_11"));
        verifyNoInteractions(pasarela);
        verify(billetera, never()).apartarPorReembolso(any(), any());
        assertThat(guardadas).isEmpty();
    }

    @Test
    void sinElCobroGuardado_noSeApartaNada_yEsErrPag06() {
        transaccionDelCobro.setRespuesta(null);

        assertThatThrownBy(() -> pedir(MotivoReembolso.MUDANZA, null)).isInstanceOfSatisfying(
            ServicioNoDisponibleException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_06"));
        verify(billetera, never()).apartarPorReembolso(any(), any());
        verifyNoInteractions(pasarela);
    }

    @Test
    void siLaPasarelaFalla_laReversionQuedaFallida_lasHorasVuelven_yEsErrPag06() {
        when(pasarela.reembolsar(any(SolicitudDevolucion.class))).thenThrow(
            new ServicioNoDisponibleException("ERR_PAG_06", "no hay pasarela"));

        assertThatThrownBy(() -> pedir(MotivoReembolso.MUDANZA, null)).isInstanceOfSatisfying(
            ServicioNoDisponibleException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_06"));

        ReversionPago reversion = guardadas.get(0);
        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.FALLIDA);
        verify(billetera).restituirApartadas(pago, 10, reversion.getCodigo());
        assertThat(transaccionesGuardadas).isEmpty();
        assertThat(huboRegistro("WARN")).isTrue();
    }

    @Test
    void siLaPasarelaFallaConOtroError_tambienSeRestituye_yEsErrPag06() {
        when(pasarela.reembolsar(any(SolicitudDevolucion.class))).thenThrow(new IllegalStateException("raro"));

        assertThatThrownBy(() -> pedir(MotivoReembolso.MUDANZA, null)).isInstanceOfSatisfying(
            ServicioNoDisponibleException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_06"));
        verify(billetera).restituirApartadas(eq(pago), anyInt(), any(UUID.class));
    }

    @Test
    void siNoSePuedeGuardarLaReferencia_elReembolsoSigueAdelante() {
        laPasarelaCrea();
        when(reversiones.findById(any(Long.class))).thenThrow(new IllegalStateException("base caída"));
        when(consulta.detalle(ESCUELA, ESTUDIANTE, pago.getCodigo())).thenReturn(mock(PagoDetalleResponse.class));

        pedir(MotivoReembolso.MUDANZA, null);

        verify(billetera, never()).restituirApartadas(any(), anyInt(), any());
        assertThat(guardadas.get(0).getEstado()).isEqualTo(EstadoReversion.SOLICITADA);
        assertThat(huboRegistro("ERROR")).isTrue();
    }
}
