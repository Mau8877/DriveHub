package com.drivehub.academico.common.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.drivehub.academico.features.reservas.model.Reserva;

class AsignarIdentificadoresCallbackTest {

    private SecuenciaService secuenciaService;
    private AsignarIdentificadoresCallback callback;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        secuenciaService = mock(SecuenciaService.class);
        ObjectProvider<SecuenciaService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(secuenciaService);
        callback = new AsignarIdentificadoresCallback(provider);
    }

    @Test
    void onBeforeConvert_documentoNuevo_asignaIdDeLaSecuenciaYCodigoV7() {
        when(secuenciaService.siguiente("reservas")).thenReturn(42L);
        Reserva reserva = new Reserva();

        callback.onBeforeConvert(reserva, "reservas");

        assertThat(reserva.getId()).isEqualTo(42L);
        assertThat(reserva.getCodigoReserva()).isNotNull();
        assertThat(reserva.getCodigoReserva().version()).isEqualTo(7);
    }

    @Test
    void onBeforeConvert_documentoExistente_noPideOtroIdNiCambiaElCodigo() {
        when(secuenciaService.siguiente("reservas")).thenReturn(7L);
        Reserva reserva = new Reserva();
        callback.onBeforeConvert(reserva, "reservas");
        var codigoOriginal = reserva.getCodigoReserva();

        callback.onBeforeConvert(reserva, "reservas");

        assertThat(reserva.getId()).isEqualTo(7L);
        assertThat(reserva.getCodigoReserva()).isEqualTo(codigoOriginal);
        verify(secuenciaService, never()).siguiente("otra");
    }
}
