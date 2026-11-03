package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.pagos.features.pagos.client.NotificacionesClient;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.service.AvisosPagoListener;
import com.drivehub.pagos.features.pagos.service.evento.CompensacionRequerida;
import com.drivehub.pagos.features.pagos.service.evento.MotivoCompensacion;
import com.drivehub.pagos.features.pagos.service.evento.PagoConfirmado;
import com.drivehub.pagos.features.pagos.service.evento.PagoVencido;
import com.drivehub.pagos.features.pagos.service.evento.ReembolsoEjecutado;
import com.drivehub.pagos.features.pagos.service.evento.ReembolsoFallido;

/**
 * Los avisos al estudiante por lo que le pasa a su pago (CU36).
 */
class AvisosPagoListenerTest {

    private static final UUID PAGO = UUID.fromString("0198f0a0-0000-7000-8000-0000000000a1");
    private static final UUID ESTUDIANTE = UUID.fromString("0198f0a0-0000-7000-8000-0000000000b2");

    private PagoRepository pagoRepository;
    private NotificacionesClient notificaciones;
    private AvisosPagoListener listener;

    @BeforeEach
    void preparar() {
        pagoRepository = mock(PagoRepository.class);
        notificaciones = mock(NotificacionesClient.class);
        listener = new AvisosPagoListener(pagoRepository, notificaciones);
        Pago pago = mock(Pago.class);
        when(pago.getEstudianteCodigo()).thenReturn(ESTUDIANTE);
        when(pago.getPlanNombre()).thenReturn("Plan 10 horas");
        when(pago.getHoras()).thenReturn((short) 10);
        when(pagoRepository.findByCodigo(PAGO)).thenReturn(Optional.of(pago));
    }

    @Test
    void alConfirmarseElPago_avisaAlEstudianteConLasHoras() {
        listener.alConfirmarseElPago(new PagoConfirmado(PAGO));

        verify(notificaciones).avisar(eq(NotificacionesClient.eventoCodigo("pago.confirmado", PAGO)), eq(ESTUDIANTE),
            eq("PAGO"), eq("Compra confirmada"), eq("Acreditamos 10 hora(s) de tu plan Plan 10 horas. "
                + "Ya puedes reservar tus clases."), any());
    }

    @Test
    void alRequerirseUnaCompensacion_avisaQueSeDevolveraElCobro() {
        listener.alRequerirseUnaCompensacion(
            new CompensacionRequerida(PAGO, MotivoCompensacion.ACREDITACION_FALLIDA, "cobro"));

        verify(notificaciones).avisar(eq(NotificacionesClient.eventoCodigo("pago.compensado", PAGO)), eq(ESTUDIANTE),
            eq("PAGO"), eq("No pudimos completar tu compra"), any(), any());
    }

    @Test
    void alVencerElPago_avisaAlEstudiante() {
        listener.alVencerElPago(new PagoVencido(PAGO));

        verify(notificaciones).avisar(eq(NotificacionesClient.eventoCodigo("pago.vencido", PAGO)), eq(ESTUDIANTE),
            eq("PAGO"), eq("Tu pago venció"), any(), any());
    }

    @Test
    void alEjecutarseElReembolso_avisaAlEstudiante() {
        listener.alEjecutarseElReembolso(new ReembolsoEjecutado(PAGO, UUID.randomUUID()));

        verify(notificaciones).avisar(eq(NotificacionesClient.eventoCodigo("pago.reembolsado", PAGO)),
            eq(ESTUDIANTE), eq("PAGO"), eq("Reembolso realizado"), any(), any());
    }

    @Test
    void alFallarElReembolso_avisaQueLasHorasVolvieron() {
        listener.alFallarElReembolso(new ReembolsoFallido(PAGO, UUID.randomUUID()));

        verify(notificaciones).avisar(eq(NotificacionesClient.eventoCodigo("pago.reembolso-fallido", PAGO)),
            eq(ESTUDIANTE), eq("PAGO"), eq("No pudimos reembolsarte"), any(), any());
    }

    @Test
    void conUnPagoQueNoExiste_noAvisaNada() {
        when(pagoRepository.findByCodigo(PAGO)).thenReturn(Optional.empty());

        listener.alConfirmarseElPago(new PagoConfirmado(PAGO));

        verify(notificaciones, never()).avisar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void siLaBaseFalla_noPropagaElError() {
        when(pagoRepository.findByCodigo(PAGO)).thenThrow(new IllegalStateException("base caída"));

        assertThatCode(() -> listener.alConfirmarseElPago(new PagoConfirmado(PAGO))).doesNotThrowAnyException();
    }
}
