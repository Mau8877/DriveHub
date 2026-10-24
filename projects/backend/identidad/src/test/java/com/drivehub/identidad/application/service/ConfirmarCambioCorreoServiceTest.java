package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.mail.MailSendException;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ConfirmarCambioCorreoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T12:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private TokenRecuperacionRepositoryPort tokenRepository;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;
    @Mock
    private EnviadorCorreoPort enviadorCorreo;

    private ConfirmarCambioCorreoService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ConfirmarCambioCorreoService(usuarioRepository, tokenRepository, generadorToken, enviadorCorreo,
            Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Usuario cuenta(EstadoUsuario estado) {
        return Usuario.builder().codigo(UUID.randomUUID()).email("viejo@gmail.com").nombres("Ana").apellidos("Pérez")
            .estado(estado).correoVerificado(false).roles(Set.of()).build();
    }

    private TokenRecuperacion enlace(Usuario u, TipoTokenCorreo tipo, boolean usado, Instant expiraEn, String correoNuevo) {
        return TokenRecuperacion.builder().codigo(UUID.randomUUID()).usuarioCodigo(u.getCodigo()).tipo(tipo)
            .tokenHash("hash").expiraEn(expiraEn).usado(usado).correoNuevo(correoNuevo).build();
    }

    private void enlaceEncontrado(TokenRecuperacion token) {
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash")).thenReturn(Optional.of(token));
    }

    private void assertEnlaceInvalido() {
        assertThatThrownBy(() -> servicio.confirmar("abc"))
            .isInstanceOfSatisfying(ValidationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.ENLACE_INVALIDO));
        verify(tokenRepository, never()).marcarUsado(any());
        verify(usuarioRepository, never()).actualizar(any());
        verify(enviadorCorreo, never()).enviarAvisoCambioCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void confirmar_conEnlaceValido_cambiaElCorreoLoVerificaConsumeElEnlaceYAvisaAlAnterior() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        TokenRecuperacion token = enlace(usuario, TipoTokenCorreo.CAMBIO_CORREO, false, AHORA.plusSeconds(60), "nuevo@gmail.com");
        enlaceEncontrado(token);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existePorEmail("nuevo@gmail.com")).thenReturn(false);

        servicio.confirmar("abc");

        verify(tokenRepository).marcarUsado(token);
        verify(tokenRepository).invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.CAMBIO_CORREO);
        ArgumentCaptor<Usuario> actualizado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(actualizado.capture());
        assertThat(actualizado.getValue().getEmail()).isEqualTo("nuevo@gmail.com");
        assertThat(actualizado.getValue().isCorreoVerificado()).isTrue();
        assertThat(actualizado.getValue().getCodigo()).isEqualTo(usuario.getCodigo());
        verify(enviadorCorreo).enviarAvisoCambioCorreo("viejo@gmail.com", "Ana", "nuevo@gmail.com");
    }

    @Test
    void confirmar_conEnlaceDeVerificacionODeRecuperacion_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        enlaceEncontrado(enlace(usuario, TipoTokenCorreo.VERIFICACION, false, AHORA.plusSeconds(60), null));

        assertEnlaceInvalido();
    }

    @Test
    void confirmar_conEnlaceDeRecuperacion_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        enlaceEncontrado(enlace(usuario, TipoTokenCorreo.RECUPERACION, false, AHORA.plusSeconds(60), null));

        assertEnlaceInvalido();
    }

    @Test
    void confirmar_conTokenInexistente_lanzaErrIde11() {
        when(generadorToken.hashear("abc")).thenReturn("hash");
        when(tokenRepository.buscarPorHash("hash")).thenReturn(Optional.empty());

        assertEnlaceInvalido();
    }

    @Test
    void confirmar_conEnlaceUsado_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        enlaceEncontrado(enlace(usuario, TipoTokenCorreo.CAMBIO_CORREO, true, AHORA.plusSeconds(60), "nuevo@gmail.com"));

        assertEnlaceInvalido();
    }

    @Test
    void confirmar_conEnlaceVencido_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        enlaceEncontrado(enlace(usuario, TipoTokenCorreo.CAMBIO_CORREO, false, AHORA, "nuevo@gmail.com"));

        assertEnlaceInvalido();
    }

    @Test
    void confirmar_siElCorreoYaEsDeOtraCuenta_lanzaErrIde11SinCambiarNada() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        enlaceEncontrado(enlace(usuario, TipoTokenCorreo.CAMBIO_CORREO, false, AHORA.plusSeconds(60), "nuevo@gmail.com"));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existePorEmail("nuevo@gmail.com")).thenReturn(true);

        assertEnlaceInvalido();
    }

    @Test
    void confirmar_conCuentaInactiva_lanzaErrIde11() {
        Usuario usuario = cuenta(EstadoUsuario.INACTIVO);
        enlaceEncontrado(enlace(usuario, TipoTokenCorreo.CAMBIO_CORREO, false, AHORA.plusSeconds(60), "nuevo@gmail.com"));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        assertEnlaceInvalido();
    }

    @Test
    void confirmar_siFallaElAvisoAlCorreoAnterior_elCambioQuedaHecho() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO);
        enlaceEncontrado(enlace(usuario, TipoTokenCorreo.CAMBIO_CORREO, false, AHORA.plusSeconds(60), "nuevo@gmail.com"));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        doThrow(new MailSendException("SMTP caído")).when(enviadorCorreo)
            .enviarAvisoCambioCorreo(anyString(), anyString(), anyString());

        servicio.confirmar("abc");

        verify(usuarioRepository).actualizar(any(Usuario.class));
    }
}
