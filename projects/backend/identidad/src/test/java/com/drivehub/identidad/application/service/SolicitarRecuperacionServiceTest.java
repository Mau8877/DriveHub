package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class SolicitarRecuperacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-20T12:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private TokenRecuperacionRepositoryPort tokenRepository;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;
    @Mock
    private EnviadorCorreoPort enviadorCorreo;

    private SolicitarRecuperacionService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new SolicitarRecuperacionService(usuarioRepository, tokenRepository, generadorToken,
            enviadorCorreo, Clock.fixed(AHORA, ZoneOffset.UTC), "http://localhost:4200", 30);
    }

    private Usuario cuenta(EstadoUsuario estado, boolean verificado) {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .estado(estado).correoVerificado(verificado).roles(Set.of()).build();
    }

    private void assertNoHaceNada() {
        verify(tokenRepository, never()).guardar(any());
        verify(tokenRepository, never()).invalidarPendientes(any(), any());
        verify(enviadorCorreo, never()).enviarRecuperacionContrasena(any(), any(), any());
    }

    @Test
    void solicitar_conCuentaVerificadaYActiva_guardaElHashEnviaElEnlaceEInvalidaLosAnteriores() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO, true);
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(generadorToken.generar()).thenReturn("token-en-claro");
        when(generadorToken.hashear("token-en-claro")).thenReturn("hash-del-token");

        servicio.solicitar("ana@gmail.com");

        InOrder orden = inOrder(tokenRepository, enviadorCorreo);
        orden.verify(tokenRepository).invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.RECUPERACION);
        ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
        orden.verify(tokenRepository).guardar(guardado.capture());
        orden.verify(enviadorCorreo).enviarRecuperacionContrasena("ana@gmail.com", "Ana",
            "http://localhost:4200/account/reset-password?token=token-en-claro");
        assertThat(guardado.getValue().getTokenHash()).isEqualTo("hash-del-token");
        assertThat(guardado.getValue().getTipo()).isEqualTo(TipoTokenCorreo.RECUPERACION);
        assertThat(guardado.getValue().getExpiraEn()).isEqualTo(AHORA.plusSeconds(30 * 60));
    }

    @Test
    void solicitar_conCuentaBloqueadaPorIntentos_tambienEnviaElEnlace() {
        Usuario usuario = cuenta(EstadoUsuario.BLOQUEADO, true);
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(generadorToken.generar()).thenReturn("t");
        when(generadorToken.hashear("t")).thenReturn("h");

        servicio.solicitar("ana@gmail.com");

        verify(enviadorCorreo).enviarRecuperacionContrasena(any(), any(), any());
    }

    @Test
    void solicitar_conCorreoInexistente_noHaceNadaYNoLanza() {
        when(usuarioRepository.buscarPorEmail("nadie@gmail.com")).thenReturn(Optional.empty());

        assertThatCode(() -> servicio.solicitar("nadie@gmail.com")).doesNotThrowAnyException();

        assertNoHaceNada();
    }

    @Test
    void solicitar_conCuentaInactiva_noHaceNadaYNoLanza() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com"))
            .thenReturn(Optional.of(cuenta(EstadoUsuario.INACTIVO, true)));

        assertThatCode(() -> servicio.solicitar("ana@gmail.com")).doesNotThrowAnyException();

        assertNoHaceNada();
    }

    @Test
    void solicitar_conElCorreoSinVerificar_noHaceNadaYNoLanza() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com"))
            .thenReturn(Optional.of(cuenta(EstadoUsuario.ACTIVO, false)));

        assertThatCode(() -> servicio.solicitar("ana@gmail.com")).doesNotThrowAnyException();

        assertNoHaceNada();
    }

    @Test
    void solicitar_siFallaElEnvio_noLanza() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO, true);
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(generadorToken.generar()).thenReturn("t");
        when(generadorToken.hashear("t")).thenReturn("h");
        doThrow(new IllegalStateException("SMTP caido")).when(enviadorCorreo)
            .enviarRecuperacionContrasena(any(), any(), any());

        assertThatCode(() -> servicio.solicitar("ana@gmail.com")).doesNotThrowAnyException();
    }

    @Test
    void solicitar_normalizaElCorreoAntesDeBuscar() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.empty());

        servicio.solicitar("  Ana@Gmail.COM ");

        verify(usuarioRepository).buscarPorEmail("ana@gmail.com");
    }
}
