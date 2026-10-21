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
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class RestablecerContrasenaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-20T12:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private TokenRecuperacionRepositoryPort tokenRepository;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;
    @Mock
    private PasswordHasherPort passwordHasher;

    private RestablecerContrasenaService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new RestablecerContrasenaService(usuarioRepository, tokenRepository, refreshTokenRepository,
            generadorToken, passwordHasher, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Usuario cuenta(EstadoUsuario estado) {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .passwordHash("hash-viejo").estado(estado).intentosFallidos(5).bloqueadoHasta(AHORA.plusSeconds(600))
            .correoVerificado(true).roles(Set.of()).build();
    }

    private TokenRecuperacion enlace(Usuario u, TipoTokenCorreo tipo, boolean usado, Instant expiraEn) {
        return TokenRecuperacion.builder().codigo(UUID.randomUUID()).usuarioCodigo(u.getCodigo()).tipo(tipo)
            .tokenHash("hash").expiraEn(expiraEn).usado(usado).build();
    }

    private void assertEnlaceInvalido() {
        assertThatThrownBy(() -> servicio.restablecer("abc", "contraseña-nueva-1"))
            .isInstanceOfSatisfying(ValidationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.ENLACE_INVALIDO));
        verify(tokenRepository, never()).marcarUsado(any());
        verify(usuarioRepository, never()).actualizar(any());
        verify(refreshTokenRepository, never()).revocarTodos(any());
        verify(passwordHasher, never()).hashear(any());
    }

    @Test
    void restablecer_conEnlaceValido_cambiaLaContrasenaDesbloqueaYCierraTodasLasSesiones() {
        Usuario usuario = cuenta(EstadoUsuario.BLOQUEADO);
        TokenRecuperacion token = enlace(usuario, TipoTokenCorreo.RECUPERACION, false, AHORA.plusSeconds(60));
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash")).thenReturn(Optional.of(token));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.hashear("contraseña-nueva-1")).thenReturn("hash-nuevo");

        servicio.restablecer("abc", "contraseña-nueva-1");

        verify(tokenRepository).marcarUsado(token);
        verify(tokenRepository).invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.RECUPERACION);
        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(actualizado.getValue().getIntentosFallidos()).isZero();
        assertThat(actualizado.getValue().getBloqueadoHasta()).isNull();
        assertThat(actualizado.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(refreshTokenRepository).revocarTodos(usuario.getCodigo());
    }

    @Test
    void restablecer_conCuentaActiva_laDejaActiva() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.RECUPERACION, false, AHORA.plusSeconds(60))));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.hashear(any())).thenReturn("hash-nuevo");

        servicio.restablecer("abc", "contraseña-nueva-1");

        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void restablecer_conTokenInexistente_lanzaErrIde11() {
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash")).thenReturn(Optional.empty());

        assertEnlaceInvalido();
    }

    @Test
    void restablecer_conEnlaceYaUsado_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.RECUPERACION, true, AHORA.plusSeconds(60))));

        assertEnlaceInvalido();
    }

    @Test
    void restablecer_conEnlaceVencido_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.RECUPERACION, false, AHORA)));

        assertEnlaceInvalido();
    }

    @Test
    void restablecer_conEnlaceDeVerificacionDeCorreo_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.VERIFICACION, false, AHORA.plusSeconds(60))));

        assertEnlaceInvalido();
    }

    @Test
    void restablecer_siLaCuentaQuedoInactiva_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.INACTIVO);
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash"))
            .thenReturn(Optional.of(enlace(usuario, TipoTokenCorreo.RECUPERACION, false, AHORA.plusSeconds(60))));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        assertEnlaceInvalido();
    }
}
