package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.model.VinculoWhatsapp;
import com.drivehub.identidad.domain.port.in.SesionWhatsapp;
import com.drivehub.identidad.domain.port.out.EmisorTokenAccesoPort;
import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.domain.port.out.VinculoWhatsappRepositoryPort;

@ExtendWith(MockitoExtension.class)
class EmitirSesionWhatsappServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private VinculoWhatsappRepositoryPort vinculoRepository;
    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private HasheadorWhatsappPort hasheador;
    @Mock
    private SesionEmisora sesionEmisora;
    @Mock
    private EmisorTokenAccesoPort emisorToken;

    private EmitirSesionWhatsappService servicio;
    private Usuario usuario;

    @BeforeEach
    void crearServicio() {
        servicio = new EmitirSesionWhatsappService(vinculoRepository, usuarioRepository, hasheador,
            new CuentaDeWhatsapp(ESCUELA), sesionEmisora, emisorToken, Clock.fixed(AHORA, ZoneOffset.UTC), 300);
        usuario = Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").estado(EstadoUsuario.ACTIVO)
            .escuelaCodigo(ESCUELA).roles(Set.of(Rol.builder().nombre(RolNombres.ESTUDIANTE).build())).build();
        when(hasheador.hashearTelefono("59171234567")).thenReturn("tel-hash");
    }

    private void vinculado() {
        when(vinculoRepository.buscarPorTelefonoHash("tel-hash")).thenReturn(Optional.of(
            VinculoWhatsapp.builder().usuarioCodigo(usuario.getCodigo()).telefonoHash("tel-hash").build()));
    }

    @Test
    void emitir_conVinculoYCuentaValida_devuelveUnTokenDeCincoMinutos() {
        vinculado();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(emisorToken.emitir(usuario, AHORA, Duration.ofSeconds(300))).thenReturn("jwt");

        SesionWhatsapp sesion = servicio.emitir("59171234567");

        assertThat(sesion.accessToken()).isEqualTo("jwt");
        assertThat(sesion.expiraEnSegundos()).isEqualTo(300);
        verify(sesionEmisora).verificarEscuelaActiva(usuario);
    }

    @Test
    void emitir_sinVinculo_lanzaErr26() {
        when(vinculoRepository.buscarPorTelefonoHash("tel-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.emitir("59171234567"))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.WHATSAPP_NO_VINCULADO));

        verify(emisorToken, never()).emitir(any(), any(), any());
    }

    @Test
    void emitir_conCuentaBloqueada_lanzaErr27() {
        vinculado();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo()))
            .thenReturn(Optional.of(usuario.toBuilder().estado(EstadoUsuario.BLOQUEADO).build()));

        assertThatThrownBy(() -> servicio.emitir("59171234567"))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.WHATSAPP_CUENTA_NO_HABILITADA));

        verify(emisorToken, never()).emitir(any(), any(), any());
    }

    @Test
    void emitir_conCuentaQueYaNoEsEstudiante_lanzaErr27() {
        vinculado();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario.toBuilder()
            .roles(Set.of(Rol.builder().nombre(RolNombres.ADMIN_ESCUELA).build())).build()));

        assertThatThrownBy(() -> servicio.emitir("59171234567")).isInstanceOf(BusinessException.class);
    }
}
