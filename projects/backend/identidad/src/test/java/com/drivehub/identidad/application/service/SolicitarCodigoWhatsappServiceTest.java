package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import com.drivehub.identidad.common.exception.LimiteExcedidoException;
import com.drivehub.identidad.domain.model.DesafioWhatsapp;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.SolicitarCodigoWhatsappComando;
import com.drivehub.identidad.domain.port.out.DesafioWhatsappRepositoryPort;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class SolicitarCodigoWhatsappServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private DesafioWhatsappRepositoryPort desafioRepository;
    @Mock
    private HasheadorWhatsappPort hasheador;
    @Mock
    private EnviadorCorreoPort enviadorCorreo;

    private SolicitarCodigoWhatsappService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new SolicitarCodigoWhatsappService(usuarioRepository, desafioRepository, hasheador, enviadorCorreo,
            new CuentaDeWhatsapp(ESCUELA), Clock.fixed(AHORA, ZoneOffset.UTC), 10, 5);
        when(hasheador.hashearTelefono(anyString())).thenReturn("tel-hash");
        when(hasheador.hashearCorreo(anyString())).thenReturn("correo-hash");
    }

    private Usuario cuenta(EstadoUsuario estado, String rol, UUID escuela) {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana").apellidos("Pérez")
            .estado(estado).escuelaCodigo(escuela).roles(Set.of(Rol.builder().nombre(rol).build())).build();
    }

    private SolicitarCodigoWhatsappComando pedido() {
        return new SolicitarCodigoWhatsappComando("59171234567", " Ana@Gmail.com ");
    }

    @Test
    void solicitar_conEstudianteValido_guardaElHashEnviaElCodigoEInvalidaLosAnteriores() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO, RolNombres.ESTUDIANTE, ESCUELA);
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(hasheador.hashearCodigo(anyString())).thenReturn("codigo-hash");

        servicio.solicitar(pedido());

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(enviadorCorreo).enviarCodigoWhatsapp(eq("ana@gmail.com"), eq("Ana"), codigo.capture(), eq(10));
        assertThat(codigo.getValue()).matches("\\d{6}");
        verify(hasheador).hashearCodigo(codigo.getValue());
        verify(desafioRepository).invalidarPendientes(usuario.getCodigo());
        ArgumentCaptor<DesafioWhatsapp> guardado = ArgumentCaptor.forClass(DesafioWhatsapp.class);
        verify(desafioRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getUsuarioCodigo()).isEqualTo(usuario.getCodigo());
        assertThat(guardado.getValue().getCodigoHash()).isEqualTo("codigo-hash");
        assertThat(guardado.getValue().getExpiraEn()).isEqualTo(AHORA.plusSeconds(600));
        assertThat(guardado.getValue().isUsado()).isFalse();
    }

    @Test
    void solicitar_conCorreoSinCuenta_noEnviaNadaPeroRegistraElPedidoDescartado() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.empty());
        when(hasheador.hashearCodigo(anyString())).thenReturn("x");

        servicio.solicitar(pedido());

        verify(enviadorCorreo, never()).enviarCodigoWhatsapp(anyString(), anyString(), anyString(), anyInt());
        ArgumentCaptor<DesafioWhatsapp> guardado = ArgumentCaptor.forClass(DesafioWhatsapp.class);
        verify(desafioRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().isUsado()).isTrue();
        assertThat(guardado.getValue().getUsuarioCodigo()).isNull();
    }

    @Test
    void solicitar_conCuentaDeOtroRol_noEnviaNada() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com"))
            .thenReturn(Optional.of(cuenta(EstadoUsuario.ACTIVO, RolNombres.INSTRUCTOR, ESCUELA)));
        when(hasheador.hashearCodigo(anyString())).thenReturn("x");

        servicio.solicitar(pedido());

        verify(enviadorCorreo, never()).enviarCodigoWhatsapp(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void solicitar_conCuentaDeOtraEscuela_noEnviaNada() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com"))
            .thenReturn(Optional.of(cuenta(EstadoUsuario.ACTIVO, RolNombres.ESTUDIANTE, UUID.randomUUID())));
        when(hasheador.hashearCodigo(anyString())).thenReturn("x");

        servicio.solicitar(pedido());

        verify(enviadorCorreo, never()).enviarCodigoWhatsapp(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void solicitar_conCuentaBloqueada_noEnviaNada() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com"))
            .thenReturn(Optional.of(cuenta(EstadoUsuario.BLOQUEADO, RolNombres.ESTUDIANTE, ESCUELA)));
        when(hasheador.hashearCodigo(anyString())).thenReturn("x");

        servicio.solicitar(pedido());

        verify(enviadorCorreo, never()).enviarCodigoWhatsapp(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void solicitar_alSuperarElLimiteDelNumero_lanzaLimiteExcedidoSinGuardarNada() {
        when(desafioRepository.contarPorTelefonoDesde(eq("tel-hash"), any())).thenReturn(5L);

        assertThatThrownBy(() -> servicio.solicitar(pedido())).isInstanceOf(LimiteExcedidoException.class);

        verify(desafioRepository, never()).guardar(any());
        verify(enviadorCorreo, never()).enviarCodigoWhatsapp(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void solicitar_alSuperarElLimiteDelCorreo_lanzaLimiteExcedido() {
        when(desafioRepository.contarPorTelefonoDesde(eq("tel-hash"), any())).thenReturn(0L);
        when(desafioRepository.contarPorCorreoDesde(eq("correo-hash"), any())).thenReturn(5L);

        assertThatThrownBy(() -> servicio.solicitar(pedido())).isInstanceOf(LimiteExcedidoException.class);
    }

    @Test
    void solicitar_cuentaElLimiteDeLaUltimaHora() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.empty());
        when(hasheador.hashearCodigo(anyString())).thenReturn("x");

        servicio.solicitar(pedido());

        verify(desafioRepository).contarPorTelefonoDesde("tel-hash", AHORA.minusSeconds(3600));
        verify(desafioRepository).contarPorCorreoDesde("correo-hash", AHORA.minusSeconds(3600));
    }

    @Test
    void solicitar_siElCorreoFalla_propagaLaExcepcion() {
        Usuario usuario = cuenta(EstadoUsuario.ACTIVO, RolNombres.ESTUDIANTE, ESCUELA);
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(hasheador.hashearCodigo(anyString())).thenReturn("codigo-hash");
        doThrow(new IllegalStateException("SMTP caído")).when(enviadorCorreo)
            .enviarCodigoWhatsapp(anyString(), anyString(), anyString(), anyInt());

        assertThatThrownBy(() -> servicio.solicitar(pedido())).isInstanceOf(IllegalStateException.class);
    }
}
