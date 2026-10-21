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

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.RefreshToken;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.RenovarSesionComando;
import com.drivehub.identidad.domain.port.in.SesionEmitida;
import com.drivehub.identidad.domain.port.out.EmisorTokenAccesoPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;
import com.drivehub.identidad.domain.port.out.RefreshTokenRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class RenovarSesionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T12:00:00Z");
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private EscuelaConsultaPort escuelaConsulta;
    @Mock
    private EmisorTokenAccesoPort emisorToken;
    @Mock
    private GeneradorTokenOpacoPort generadorToken;

    private RenovarSesionService servicio;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        SesionEmisora emisora = new SesionEmisora(escuelaConsulta, refreshTokenRepository, emisorToken, generadorToken, 15, 7);
        servicio = new RenovarSesionService(usuarioRepository, refreshTokenRepository, generadorToken, emisora, reloj);
    }

    private Usuario cuenta(String rol, EstadoUsuario estado, Instant bloqueadoHasta) {
        return Usuario.builder()
            .codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .escuelaCodigo(ESCUELA).estado(estado).bloqueadoHasta(bloqueadoHasta)
            .roles(Set.of(Rol.builder().nombre(rol).build())).build();
    }

    private RefreshToken token(UUID usuarioCodigo, Dispositivo dispositivo, boolean revocado, Instant expiraEn) {
        return RefreshToken.builder().codigo(UUID.randomUUID()).usuarioCodigo(usuarioCodigo).tokenHash("hash-viejo")
            .dispositivo(dispositivo).revocado(revocado).expiraEn(expiraEn).build();
    }

    private void tokenGuardado(RefreshToken token) {
        when(generadorToken.hashear("viejo")).thenReturn("hash-viejo");
        when(refreshTokenRepository.buscarPorHash("hash-viejo")).thenReturn(Optional.of(token));
    }

    private void emisionDisponible() {
        when(generadorToken.generar()).thenReturn("nuevo");
        when(generadorToken.hashear("nuevo")).thenReturn("hash-nuevo");
        when(emisorToken.emitir(any(Usuario.class), any(Instant.class))).thenReturn("jwt-nuevo");
    }

    private void escuela(boolean activa) {
        when(escuelaConsulta.buscarPorCodigo(ESCUELA)).thenReturn(Optional.of(new EscuelaRef(ESCUELA, "e", "E", activa)));
    }

    private RenovarSesionComando renovar(Dispositivo dispositivo) {
        return new RenovarSesionComando("viejo", dispositivo);
    }

    private void esperaCodigo(RenovarSesionComando comando, String codigo) {
        assertThatThrownBy(() -> servicio.renovar(comando))
            .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void renovar_conSesionValida_revocaElTokenUsadoYEntregaUnoNuevo() {
        Usuario usuario = cuenta(RolNombres.ESTUDIANTE, EstadoUsuario.ACTIVO, null);
        RefreshToken viejo = token(usuario.getCodigo(), Dispositivo.MOVIL, false, AHORA.plusSeconds(3600));
        tokenGuardado(viejo);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        escuela(true);
        emisionDisponible();

        SesionEmitida sesion = servicio.renovar(renovar(Dispositivo.MOVIL));

        assertThat(sesion.accessToken()).isEqualTo("jwt-nuevo");
        assertThat(sesion.refreshToken()).isEqualTo("nuevo");
        verify(refreshTokenRepository).revocar(viejo);
        ArgumentCaptor<RefreshToken> nuevo = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).guardar(nuevo.capture());
        assertThat(nuevo.getValue().getTokenHash()).isEqualTo("hash-nuevo");
        assertThat(nuevo.getValue().getExpiraEn()).isEqualTo(AHORA.plusSeconds(7 * 24 * 3600));
    }

    @Test
    void renovar_conTokenInexistente_lanzaErrIde09() {
        when(generadorToken.hashear("viejo")).thenReturn("hash-viejo");
        when(refreshTokenRepository.buscarPorHash("hash-viejo")).thenReturn(Optional.empty());

        esperaCodigo(renovar(Dispositivo.WEB), IdentidadErrorCodes.SESION_INVALIDA);
    }

    @Test
    void renovar_sinToken_lanzaErrIde09() {
        esperaCodigo(new RenovarSesionComando(null, Dispositivo.WEB), IdentidadErrorCodes.SESION_INVALIDA);
        esperaCodigo(new RenovarSesionComando("  ", Dispositivo.MOVIL), IdentidadErrorCodes.SESION_INVALIDA);
    }

    @Test
    void renovar_conTokenYaRotado_lanzaErrIde09YNoEmiteNada() {
        tokenGuardado(token(UUID.randomUUID(), Dispositivo.WEB, true, AHORA.plusSeconds(3600)));

        esperaCodigo(renovar(Dispositivo.WEB), IdentidadErrorCodes.SESION_INVALIDA);
        verify(refreshTokenRepository, never()).guardar(any());
    }

    @Test
    void renovar_conTokenVencido_lanzaErrIde09() {
        tokenGuardado(token(UUID.randomUUID(), Dispositivo.WEB, false, AHORA.minusSeconds(1)));

        esperaCodigo(renovar(Dispositivo.WEB), IdentidadErrorCodes.SESION_INVALIDA);
    }

    @Test
    void renovar_conTokenDeOtroDispositivo_lanzaErrIde09() {
        tokenGuardado(token(UUID.randomUUID(), Dispositivo.MOVIL, false, AHORA.plusSeconds(3600)));

        esperaCodigo(renovar(Dispositivo.WEB), IdentidadErrorCodes.SESION_INVALIDA);
    }

    @Test
    void renovar_conCuentaInactiva_revocaElTokenYLanzaErrIde06() {
        Usuario usuario = cuenta(RolNombres.ESTUDIANTE, EstadoUsuario.INACTIVO, null);
        RefreshToken viejo = token(usuario.getCodigo(), Dispositivo.MOVIL, false, AHORA.plusSeconds(3600));
        tokenGuardado(viejo);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        esperaCodigo(renovar(Dispositivo.MOVIL), IdentidadErrorCodes.CUENTA_INACTIVA);
        verify(refreshTokenRepository).revocar(viejo);
        verify(refreshTokenRepository, never()).guardar(any());
    }

    @Test
    void renovar_conCuentaBloqueada_revocaElTokenYLanzaErrIde05() {
        Usuario usuario = cuenta(RolNombres.ESTUDIANTE, EstadoUsuario.BLOQUEADO, AHORA.plusSeconds(600));
        RefreshToken viejo = token(usuario.getCodigo(), Dispositivo.MOVIL, false, AHORA.plusSeconds(3600));
        tokenGuardado(viejo);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        esperaCodigo(renovar(Dispositivo.MOVIL), IdentidadErrorCodes.CUENTA_BLOQUEADA);
        verify(refreshTokenRepository).revocar(viejo);
    }

    @Test
    void renovar_conEscuelaInactiva_elMensajeDiceQueLaEscuelaEstaInactiva() {
        Usuario usuario = cuenta(RolNombres.ESTUDIANTE, EstadoUsuario.ACTIVO, null);
        tokenGuardado(token(usuario.getCodigo(), Dispositivo.MOVIL, false, AHORA.plusSeconds(3600)));
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        escuela(false);

        assertThatThrownBy(() -> servicio.renovar(renovar(Dispositivo.MOVIL)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Tu escuela está inactiva. Contacta al administrador de la plataforma");
    }

    @Test
    void renovar_conEscuelaSuspendida_revocaElTokenYLanzaErrIde07() {
        Usuario usuario = cuenta(RolNombres.ESTUDIANTE, EstadoUsuario.ACTIVO, null);
        RefreshToken viejo = token(usuario.getCodigo(), Dispositivo.MOVIL, false, AHORA.plusSeconds(3600));
        tokenGuardado(viejo);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        escuela(false);

        esperaCodigo(renovar(Dispositivo.MOVIL), IdentidadErrorCodes.ESCUELA_SUSPENDIDA);
        verify(refreshTokenRepository).revocar(viejo);
    }

    @Test
    void renovar_cuandoElRolYaNoPuedeUsarLaPlataforma_revocaElTokenYLanzaErrIde08() {
        Usuario usuario = cuenta(RolNombres.ESTUDIANTE, EstadoUsuario.ACTIVO, null);
        RefreshToken viejo = token(usuario.getCodigo(), Dispositivo.WEB, false, AHORA.plusSeconds(3600));
        tokenGuardado(viejo);
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        escuela(true);

        esperaCodigo(renovar(Dispositivo.WEB), IdentidadErrorCodes.PLATAFORMA_NO_PERMITIDA);
        verify(refreshTokenRepository).revocar(viejo);
    }
}
