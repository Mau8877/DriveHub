package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ConfirmarCorreoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-20T12:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private TokenRecuperacionRepositoryPort tokenRepository;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;

    private ConfirmarCorreoService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ConfirmarCorreoService(usuarioRepository, tokenRepository, generadorToken,
            Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Usuario cuenta() {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .estado(EstadoUsuario.ACTIVO).roles(Set.of()).build();
    }

    private TokenRecuperacion enlace(Usuario u, TipoTokenCorreo tipo, boolean usado, Instant expiraEn) {
        return TokenRecuperacion.builder().codigo(UUID.randomUUID()).usuarioCodigo(u.getCodigo()).tipo(tipo)
            .tokenHash("hash").expiraEn(expiraEn).usado(usado).build();
    }

    private void assertEnlaceInvalido(String token) {
        assertThatThrownBy(() -> servicio.confirmar(token))
            .isInstanceOfSatisfying(ValidationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.ENLACE_INVALIDO));
        verify(tokenRepository, never()).marcarUsado(any());
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void confirmar_conEnlaceValido_verificaElCorreoYConsumeElEnlace() {
        Usuario usuario = cuenta();
        TokenRecuperacion token = enlace(usuario, TipoTokenCorreo.VERIFICACION, false, AHORA.plusSeconds(60));
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash")).thenReturn(Optional.of(token));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        servicio.confirmar("abc");

        verify(tokenRepository).marcarUsado(token);
        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().isCorreoVerificado()).isTrue();
        assertThat(actualizado.getValue().getCodigo()).isEqualTo(usuario.getCodigo());
    }

    @Test
    void confirmar_conTokenInexistente_lanzaErrIde11() {
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash")).thenReturn(Optional.empty());

        assertEnlaceInvalido("abc");
    }

    @Test
    void confirmar_conEnlaceYaUsado_lanzaErrIde11() {
        Usuario usuario = cuenta();
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.VERIFICACION, true, AHORA.plusSeconds(60))));

        assertEnlaceInvalido("abc");
    }

    @Test
    void confirmar_conEnlaceVencido_lanzaErrIde11() {
        Usuario usuario = cuenta();
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.VERIFICACION, false, AHORA)));

        assertEnlaceInvalido("abc");
    }

    @Test
    void confirmar_conEnlaceDeRecuperacion_lanzaErrIde11() {
        Usuario usuario = cuenta();
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.RECUPERACION, false, AHORA.plusSeconds(60))));

        assertEnlaceInvalido("abc");
    }

    @Test
    void confirmar_siLaCuentaYaNoExiste_lanzaErrIde11() {
        Usuario usuario = cuenta();
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.VERIFICACION, false, AHORA.plusSeconds(60))));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.empty());

        assertEnlaceInvalido("abc");
    }
}
