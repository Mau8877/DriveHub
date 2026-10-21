package com.drivehub.identidad.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.RefreshToken;
import com.drivehub.identidad.domain.port.in.CerrarSesionComando;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;

@ExtendWith(MockitoExtension.class)
class CerrarSesionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T12:00:00Z");

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;

    private CerrarSesionService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new CerrarSesionService(refreshTokenRepository, generadorToken, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private RefreshToken token(Dispositivo dispositivo, boolean revocado, Instant expiraEn) {
        return RefreshToken.builder().codigo(UUID.randomUUID()).usuarioCodigo(UUID.randomUUID()).tokenHash("hash")
            .dispositivo(dispositivo).revocado(revocado).expiraEn(expiraEn).build();
    }

    private void tokenGuardado(RefreshToken token) {
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(refreshTokenRepository.buscarPorHash("hash")).thenReturn(Optional.of(token));
    }

    @Test
    void cerrar_conTokenVigenteDelDispositivo_loRevoca() {
        RefreshToken token = token(Dispositivo.WEB, false, AHORA.plusSeconds(60));
        tokenGuardado(token);

        servicio.cerrar(new CerrarSesionComando("abc", Dispositivo.WEB));

        verify(refreshTokenRepository).revocar(token);
    }

    @Test
    void cerrar_conTokenNuloOVacio_noHaceNada() {
        assertDoesNotThrow(() -> servicio.cerrar(new CerrarSesionComando(null, Dispositivo.WEB)));
        assertDoesNotThrow(() -> servicio.cerrar(new CerrarSesionComando("  ", Dispositivo.WEB)));

        verify(refreshTokenRepository, never()).revocar(any());
    }

    @Test
    void cerrar_conTokenInexistente_noHaceNada() {
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(refreshTokenRepository.buscarPorHash("hash")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> servicio.cerrar(new CerrarSesionComando("abc", Dispositivo.WEB)));

        verify(refreshTokenRepository, never()).revocar(any());
    }

    @Test
    void cerrar_conTokenYaRevocado_noHaceNada() {
        tokenGuardado(token(Dispositivo.WEB, true, AHORA.plusSeconds(60)));

        servicio.cerrar(new CerrarSesionComando("abc", Dispositivo.WEB));

        verify(refreshTokenRepository, never()).revocar(any());
    }

    @Test
    void cerrar_conTokenVencido_noHaceNada() {
        tokenGuardado(token(Dispositivo.WEB, false, AHORA));

        servicio.cerrar(new CerrarSesionComando("abc", Dispositivo.WEB));

        verify(refreshTokenRepository, never()).revocar(any());
    }

    @Test
    void cerrar_conTokenDeOtroDispositivo_noHaceNada() {
        tokenGuardado(token(Dispositivo.MOVIL, false, AHORA.plusSeconds(60)));

        servicio.cerrar(new CerrarSesionComando("abc", Dispositivo.WEB));

        verify(refreshTokenRepository, never()).revocar(any());
    }
}
