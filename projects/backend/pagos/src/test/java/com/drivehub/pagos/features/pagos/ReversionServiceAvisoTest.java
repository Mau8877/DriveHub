package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.enums.OrigenReversion;
import com.drivehub.pagos.features.pagos.model.ReversionPago;
import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;
import com.drivehub.pagos.features.pagos.pasarela.AvisoPasarela;
import com.drivehub.pagos.features.pagos.pasarela.TipoAviso;
import com.drivehub.pagos.features.pagos.service.evento.ReembolsoEjecutado;

/**
 * Lo que pasa cuando la pasarela avisa que una devolución terminó o falló (CU31), con todo mockeado.
 */
class ReversionServiceAvisoTest extends ReversionServiceTestBase {

    private ReversionPago reversion;

    @BeforeEach
    void unaReversionSolicitada() {
        reversion = nueva(OrigenReversion.ESTUDIANTE);
    }

    private ReversionPago nueva(OrigenReversion origen) {
        ReversionPago r = new ReversionPago(pago, origen, origen == OrigenReversion.SAGA ? "MONTO_DISTINTO"
            : "MUDANZA", null, new java.math.BigDecimal("36.32"), new java.math.BigDecimal("1.91"));
        asignar(r);
        r.setReferenciaExterna("re_test_1");
        guardadas.add(r);
        TransaccionPasarela t = new TransaccionPasarela(pago, "STRIPE_REEMBOLSO", "SOLICITADA");
        t.setReferenciaExterna("re_test_1");
        transaccionesGuardadas.add(t);
        return r;
    }

    private AvisoPasarela aviso(TipoAviso tipo) {
        return new AvisoPasarela("evt_1", tipo, "re_test_1", pago.getCodigo(), 3632L, "usd", COBRO,
            reversion.getCodigo());
    }

    @Test
    void ejecutada_dejaLaReversionEjecutada_elPagoRevertido_unEventoYUnOutbox() {
        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_EJECUTADO));

        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.EJECUTADA);
        assertThat(reversion.getEjecutadoEn()).isEqualTo(AHORA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.REVERTIDO);
        assertThat(transaccionesGuardadas.get(0).getEstado()).isEqualTo("COMPLETADA");
        assertThat(eventosPublicados).containsExactly(new ReembolsoEjecutado(pago.getCodigo(), reversion.getCodigo()));
        assertThat(outboxGuardado).singleElement().satisfies(e -> {
            assertThat(e.getTipo()).isEqualTo("pago.revertido");
            assertThat(e.getPayload()).contains(pago.getCodigo().toString(), reversion.getCodigo().toString());
        });
        assertThat(huboRegistro("INFO")).isTrue();
    }

    @Test
    void elMismoAvisoOtraVez_noCambiaNada() {
        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_EJECUTADO));
        eventosPublicados.clear();
        outboxGuardado.clear();

        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_EJECUTADO));

        assertThat(eventosPublicados).isEmpty();
        assertThat(outboxGuardado).isEmpty();
        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.EJECUTADA);
    }

    @Test
    void sinElCodigoDeLaReversion_laEncuentraPorLaReferenciaDeLaDevolucion() {
        AvisoPasarela sinCodigo = new AvisoPasarela("evt_1", TipoAviso.REEMBOLSO_EJECUTADO, "re_test_1", null, 3632L,
            "usd", COBRO, null);

        servicio.aplicarAvisoReembolso(sinCodigo);

        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.EJECUTADA);
    }

    @Test
    void siElAvisoLlegoAntesDeGuardarLaReferencia_laReversionLaToma() {
        reversion.setReferenciaExterna(null);
        transaccionesGuardadas.clear();

        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_EJECUTADO));

        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.EJECUTADA);
        assertThat(reversion.getReferenciaExterna()).isEqualTo("re_test_1");
    }

    @Test
    void fallidaDelEstudiante_restituyeLasHoras_yElPagoSigueConfirmado() {
        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_FALLIDO));

        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.FALLIDA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
        verify(billetera).restituirApartadas(pago, 10, reversion.getCodigo());
        assertThat(transaccionesGuardadas.get(0).getEstado()).isEqualTo("FALLIDA");
        assertThat(eventosPublicados).isEmpty();
        assertThat(huboRegistro("WARN")).isTrue();
    }

    @Test
    void fallidaDeLaSaga_registraErrorParaRevision_sinRestituirHoras() {
        guardadas.clear();
        reversion = nueva(OrigenReversion.SAGA);
        pago.setEstado(EstadoPago.FALLIDO);

        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_FALLIDO));

        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.FALLIDA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        verify(billetera, never()).restituirApartadas(any(), anyInt(), any());
        assertThat(huboRegistro("ERROR")).isTrue();
    }

    @Test
    void unaFallidaRepetida_noRestituyeDosVeces() {
        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_FALLIDO));

        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_FALLIDO));

        verify(billetera).restituirApartadas(pago, 10, reversion.getCodigo());
    }

    @Test
    void unaDevolucionEjecutadaQuePagosDioPorFallida_seRegistraErrorYNoCambiaNada() {
        reversion.setEstado(EstadoReversion.FALLIDA);

        servicio.aplicarAvisoReembolso(aviso(TipoAviso.REEMBOLSO_EJECUTADO));

        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.FALLIDA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
        assertThat(eventosPublicados).isEmpty();
        assertThat(huboRegistro("ERROR")).isTrue();
    }

    @Test
    void unaReferenciaDesconocida_seRegistraSinError() {
        AvisoPasarela desconocido = new AvisoPasarela("evt_2", TipoAviso.REEMBOLSO_EJECUTADO, "re_otra",
            UUID.randomUUID(), 3632L, "usd", COBRO, UUID.randomUUID());

        servicio.aplicarAvisoReembolso(desconocido);

        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.SOLICITADA);
        assertThat(eventosPublicados).isEmpty();
        assertThat(huboRegistro("WARN")).isTrue();
    }

    @Test
    void unAvisoDeOtroPago_seIgnora() {
        AvisoPasarela deOtroPago = new AvisoPasarela("evt_3", TipoAviso.REEMBOLSO_EJECUTADO, "re_test_1",
            UUID.randomUUID(), 3632L, "usd", COBRO, reversion.getCodigo());

        servicio.aplicarAvisoReembolso(deOtroPago);

        assertThat(reversion.getEstado()).isEqualTo(EstadoReversion.SOLICITADA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
    }
}
