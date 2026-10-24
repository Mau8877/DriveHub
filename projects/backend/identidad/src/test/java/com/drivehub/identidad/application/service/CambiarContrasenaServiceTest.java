package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CambiarContrasenaComando;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class CambiarContrasenaServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private PasswordHasherPort passwordHasher;

    private CambiarContrasenaService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new CambiarContrasenaService(usuarioRepository, refreshTokenRepository, passwordHasher);
    }

    private Usuario cuenta() {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .passwordHash("hash-actual").estado(EstadoUsuario.ACTIVO).intentosFallidos(2).roles(Set.of()).build();
    }

    private CambiarContrasenaComando comando(Usuario u, Dispositivo dispositivo) {
        return new CambiarContrasenaComando(u.getCodigo(), "actual-123", "nueva-secreta-456", dispositivo);
    }

    @Test
    void cambiar_conLaActualCorrecta_guardaElHashNuevoSinTocarLosIntentos() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.coincide("actual-123", "hash-actual")).thenReturn(true);
        when(passwordHasher.coincide("nueva-secreta-456", "hash-actual")).thenReturn(false);
        when(passwordHasher.hashear("nueva-secreta-456")).thenReturn("hash-nuevo");

        servicio.cambiar(comando(usuario, Dispositivo.WEB));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(guardado.capture());
        assertThat(guardado.getValue().getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(guardado.getValue().getIntentosFallidos()).isEqualTo(2);
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void cambiar_desdeLaWeb_revocaSoloLaSesionDelMovil() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.coincide("actual-123", "hash-actual")).thenReturn(true);

        servicio.cambiar(comando(usuario, Dispositivo.WEB));

        verify(refreshTokenRepository).revocarActivos(usuario.getCodigo(), Dispositivo.MOVIL);
        verify(refreshTokenRepository, never()).revocarActivos(usuario.getCodigo(), Dispositivo.WEB);
        verify(refreshTokenRepository, never()).revocarTodos(any());
    }

    @Test
    void cambiar_desdeElMovil_revocaSoloLaSesionDeLaWeb() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.coincide("actual-123", "hash-actual")).thenReturn(true);

        servicio.cambiar(comando(usuario, Dispositivo.MOVIL));

        verify(refreshTokenRepository).revocarActivos(usuario.getCodigo(), Dispositivo.WEB);
        verify(refreshTokenRepository, never()).revocarActivos(usuario.getCodigo(), Dispositivo.MOVIL);
    }

    @Test
    void cambiar_conLaActualIncorrecta_lanzaErrIde14SinCambiarNadaNiTocarLosIntentos() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.coincide("actual-123", "hash-actual")).thenReturn(false);

        assertThatThrownBy(() -> servicio.cambiar(comando(usuario, Dispositivo.WEB)))
            .isInstanceOfSatisfying(ValidationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.PASSWORD_ACTUAL_INCORRECTA));
        verify(usuarioRepository, never()).actualizar(any());
        verify(refreshTokenRepository, never()).revocarActivos(any(), any());
    }

    @Test
    void cambiar_conLaNuevaIgualALaActual_lanzaErrIde15() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.coincide("actual-123", "hash-actual")).thenReturn(true);
        when(passwordHasher.coincide("nueva-secreta-456", "hash-actual")).thenReturn(true);

        assertThatThrownBy(() -> servicio.cambiar(comando(usuario, Dispositivo.WEB)))
            .isInstanceOfSatisfying(ValidationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.PASSWORD_NUEVA_IGUAL_A_ACTUAL));
        verify(usuarioRepository, never()).actualizar(any());
        verify(refreshTokenRepository, never()).revocarActivos(any(), any());
    }

    @Test
    void cambiar_conCuentaInexistente_lanzaErrIde09() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiar(
                new CambiarContrasenaComando(codigo, "actual-123", "nueva-secreta-456", Dispositivo.WEB)))
            .isInstanceOfSatisfying(UnauthorizedException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.SESION_INVALIDA));
    }

    @Test
    void cambiar_ningunLogContieneLasContrasenas() {
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(CambiarContrasenaService.class);
        ListAppender<ILoggingEvent> registros = new ListAppender<>();
        registros.start();
        logger.addAppender(registros);
        try {
            Usuario usuario = cuenta();
            when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
            when(passwordHasher.coincide("actual-123", "hash-actual")).thenReturn(true, false);

            servicio.cambiar(comando(usuario, Dispositivo.WEB));
            assertThatThrownBy(() -> servicio.cambiar(comando(usuario, Dispositivo.WEB))).isInstanceOf(ValidationException.class);

            assertThat(registros.list).isNotEmpty();
            assertThat(registros.list).allSatisfy(evento -> {
                String todo = evento.getFormattedMessage() + " " + java.util.Arrays.toString(evento.getArgumentArray());
                assertThat(todo).doesNotContain("actual-123").doesNotContain("nueva-secreta-456").doesNotContain("hash-");
            });
        } finally {
            logger.detachAppender(registros);
        }
    }
}
