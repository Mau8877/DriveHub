package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class SolicitarVerificacionCorreoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-20T12:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private TokenRecuperacionRepositoryPort tokenRepository;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;
    @Mock
    private EnviadorCorreoPort enviadorCorreo;

    private SolicitarVerificacionCorreoService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new SolicitarVerificacionCorreoService(usuarioRepository, tokenRepository, generadorToken,
            enviadorCorreo, Clock.fixed(AHORA, ZoneOffset.UTC), "http://localhost:4200/", 24);
    }

    private Usuario cuenta(boolean verificado) {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .estado(EstadoUsuario.ACTIVO).correoVerificado(verificado).roles(Set.of()).build();
    }

    @Test
    void solicitar_conCuentaSinVerificar_guardaElHashEnviaElEnlaceYInvalidaLosAnteriores() {
        Usuario usuario = cuenta(false);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(generadorToken.generar()).thenReturn("token-en-claro");
        when(generadorToken.hashear("token-en-claro")).thenReturn("hash-del-token");

        servicio.solicitar(usuario.getCodigo());

        InOrder orden = inOrder(tokenRepository, enviadorCorreo);
        orden.verify(tokenRepository).invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.VERIFICACION);
        ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
        orden.verify(tokenRepository).guardar(guardado.capture());
        orden.verify(enviadorCorreo).enviarVerificacionCorreo("ana@gmail.com", "Ana",
            "http://localhost:4200/account/verify-email?token=token-en-claro");
        assertThat(guardado.getValue().getTokenHash()).isEqualTo("hash-del-token");
        assertThat(guardado.getValue().getTipo()).isEqualTo(TipoTokenCorreo.VERIFICACION);
        assertThat(guardado.getValue().getUsuarioCodigo()).isEqualTo(usuario.getCodigo());
        assertThat(guardado.getValue().getExpiraEn()).isEqualTo(AHORA.plusSeconds(24 * 3600));
    }

    @Test
    void solicitar_conElCorreoYaVerificado_lanzaErrIde10SinEnviarNada() {
        Usuario usuario = cuenta(true);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.solicitar(usuario.getCodigo()))
            .isInstanceOfSatisfying(ConflictException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_YA_VERIFICADO));

        verify(tokenRepository, never()).guardar(any());
        verify(enviadorCorreo, never()).enviarVerificacionCorreo(any(), any(), any());
    }

    @Test
    void solicitar_conCuentaInexistente_lanzaErrIde09() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.solicitar(codigo))
            .isInstanceOfSatisfying(UnauthorizedException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.SESION_INVALIDA));
    }

    @Test
    void solicitar_siFallaElEnvio_laExcepcionSePropaga() {
        Usuario usuario = cuenta(false);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(generadorToken.generar()).thenReturn("t");
        when(generadorToken.hashear("t")).thenReturn("h");
        doThrow(new IllegalStateException("SMTP caido")).when(enviadorCorreo).enviarVerificacionCorreo(any(), any(), any());

        assertThatThrownBy(() -> servicio.solicitar(usuario.getCodigo())).isInstanceOf(IllegalStateException.class);
    }
}
