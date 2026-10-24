package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.mail.MailSendException;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.TipoTokenCorreo;
import com.drivehub.identidad.domain.model.TokenRecuperacion;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.SolicitarCambioCorreoComando;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.TokenRecuperacionRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class SolicitarCambioCorreoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T12:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private TokenRecuperacionRepositoryPort tokenRepository;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private EnviadorCorreoPort enviadorCorreo;

    private SolicitarCambioCorreoService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new SolicitarCambioCorreoService(usuarioRepository, tokenRepository, generadorToken, passwordHasher,
            enviadorCorreo, Clock.fixed(AHORA, ZoneOffset.UTC), "http://localhost:4200/", 24);
    }

    private Usuario cuenta() {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .passwordHash("hash").estado(EstadoUsuario.ACTIVO).roles(Set.of()).build();
    }

    private void cuentaConPasswordCorrecta(Usuario usuario) {
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.coincide("actual-123", "hash")).thenReturn(true);
    }

    private SolicitarCambioCorreoComando comando(Usuario u, String correoNuevo) {
        return new SolicitarCambioCorreoComando(u.getCodigo(), "actual-123", correoNuevo);
    }

    @Test
    void solicitar_conDatosValidos_enviaElEnlaceAlCorreoNuevoYNoModificaLaCuenta() {
        Usuario usuario = cuenta();
        cuentaConPasswordCorrecta(usuario);
        when(usuarioRepository.existePorEmail("nuevo@gmail.com")).thenReturn(false);
        when(generadorToken.generar()).thenReturn("token-en-claro");
        when(generadorToken.hashear("token-en-claro")).thenReturn("hash-del-token");

        servicio.solicitar(comando(usuario, "  Nuevo@Gmail.com "));

        verify(enviadorCorreo).enviarCambioCorreo("nuevo@gmail.com", "Ana",
            "http://localhost:4200/account/change-email?token=token-en-claro");
        verify(usuarioRepository, never()).actualizar(any());
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void solicitar_invalidaLosPendientesAntesDeGuardarYGuardaSoloElHashConTipoYCorreoNuevo() {
        Usuario usuario = cuenta();
        cuentaConPasswordCorrecta(usuario);
        when(generadorToken.generar()).thenReturn("token-en-claro");
        when(generadorToken.hashear("token-en-claro")).thenReturn("hash-del-token");

        servicio.solicitar(comando(usuario, "nuevo@gmail.com"));

        InOrder orden = inOrder(tokenRepository, enviadorCorreo);
        orden.verify(tokenRepository).invalidarPendientes(usuario.getCodigo(), TipoTokenCorreo.CAMBIO_CORREO);
        ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
        orden.verify(tokenRepository).guardar(guardado.capture());
        orden.verify(enviadorCorreo).enviarCambioCorreo(anyString(), anyString(), anyString());
        TokenRecuperacion token = guardado.getValue();
        assertThat(token.getTokenHash()).isEqualTo("hash-del-token");
        assertThat(token.getTipo()).isEqualTo(TipoTokenCorreo.CAMBIO_CORREO);
        assertThat(token.getCorreoNuevo()).isEqualTo("nuevo@gmail.com");
        assertThat(token.getUsuarioCodigo()).isEqualTo(usuario.getCodigo());
        assertThat(token.getExpiraEn()).isEqualTo(AHORA.plusSeconds(24 * 3600));
    }

    @Test
    void solicitar_conLaActualIncorrecta_lanzaErrIde14SinEnviar() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(passwordHasher.coincide("actual-123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> servicio.solicitar(comando(usuario, "nuevo@gmail.com")))
            .isInstanceOfSatisfying(ValidationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.PASSWORD_ACTUAL_INCORRECTA));
        verify(tokenRepository, never()).guardar(any());
        verify(enviadorCorreo, never()).enviarCambioCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void solicitar_conElMismoCorreoAunConMayusculas_lanzaErrIde18() {
        Usuario usuario = cuenta();
        cuentaConPasswordCorrecta(usuario);

        assertThatThrownBy(() -> servicio.solicitar(comando(usuario, " ANA@Gmail.com ")))
            .isInstanceOfSatisfying(ConflictException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_IGUAL_AL_ACTUAL));
        verify(tokenRepository, never()).guardar(any());
        verify(enviadorCorreo, never()).enviarCambioCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void solicitar_conUnCorreoYaUsado_lanzaErrIde01() {
        Usuario usuario = cuenta();
        cuentaConPasswordCorrecta(usuario);
        when(usuarioRepository.existePorEmail("otro@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.solicitar(comando(usuario, "otro@gmail.com")))
            .isInstanceOfSatisfying(ConflictException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_EN_USO));
        verify(tokenRepository, never()).guardar(any());
        verify(enviadorCorreo, never()).enviarCambioCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void solicitar_siFallaElEnvio_laExcepcionSePropagaParaQueSeRevierta() {
        Usuario usuario = cuenta();
        cuentaConPasswordCorrecta(usuario);
        when(generadorToken.generar()).thenReturn("token-en-claro");
        when(generadorToken.hashear("token-en-claro")).thenReturn("hash-del-token");
        doThrow(new MailSendException("SMTP caído")).when(enviadorCorreo).enviarCambioCorreo(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> servicio.solicitar(comando(usuario, "nuevo@gmail.com")))
            .isInstanceOf(MailSendException.class);
    }

    @Test
    void solicitar_conCuentaInexistente_lanzaErrIde09() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.solicitar(new SolicitarCambioCorreoComando(codigo, "x", "nuevo@gmail.com")))
            .isInstanceOf(UnauthorizedException.class);
    }
}
