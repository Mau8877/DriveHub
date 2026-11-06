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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.BusinessException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.DesafioWhatsapp;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.ConfirmarCodigoWhatsappComando;
import com.drivehub.identidad.domain.port.out.DesafioWhatsappRepositoryPort;
import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.domain.port.out.VinculoWhatsappRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ConfirmarCodigoWhatsappServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private DesafioWhatsappRepositoryPort desafioRepository;
    @Mock
    private VinculoWhatsappRepositoryPort vinculoRepository;
    @Mock
    private HasheadorWhatsappPort hasheador;

    private ConfirmarCodigoWhatsappService servicio;
    private Usuario usuario;

    @BeforeEach
    void crearServicio() {
        servicio = new ConfirmarCodigoWhatsappService(usuarioRepository, desafioRepository, vinculoRepository,
            hasheador, new CuentaDeWhatsapp(ESCUELA), Clock.fixed(AHORA, ZoneOffset.UTC), 5);
        usuario = Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").nombres("Ana")
            .estado(EstadoUsuario.ACTIVO).escuelaCodigo(ESCUELA)
            .roles(Set.of(Rol.builder().nombre(RolNombres.ESTUDIANTE).build())).build();
        when(hasheador.hashearTelefono("59171234567")).thenReturn("tel-hash");
    }

    private ConfirmarCodigoWhatsappComando comando(String codigo) {
        return new ConfirmarCodigoWhatsappComando("59171234567", "Ana@gmail.com", codigo);
    }

    private DesafioWhatsapp desafio(int intentos) {
        return DesafioWhatsapp.builder().codigo(UUID.randomUUID()).usuarioCodigo(usuario.getCodigo())
            .telefonoHash("tel-hash").codigoHash("hash-ok").intentos(intentos).expiraEn(AHORA.plusSeconds(60)).build();
    }

    private void cuentaYDesafio(DesafioWhatsapp desafio) {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(desafioRepository.buscarVigente(usuario.getCodigo(), "tel-hash", AHORA)).thenReturn(Optional.of(desafio));
    }

    @Test
    void confirmar_conCodigoCorrecto_marcaUsadoYVinculaElNumero() {
        DesafioWhatsapp d = desafio(0);
        cuentaYDesafio(d);
        when(hasheador.hashearCodigo("123456")).thenReturn("hash-ok");

        servicio.confirmar(comando("123456"));

        verify(desafioRepository).marcarUsado(d.getCodigo());
        verify(vinculoRepository).reemplazar(usuario.getCodigo(), "tel-hash");
    }

    @Test
    void confirmar_conCodigoIncorrecto_sumaUnIntentoYLanzaErr25() {
        DesafioWhatsapp d = desafio(0);
        cuentaYDesafio(d);
        when(hasheador.hashearCodigo("000000")).thenReturn("hash-otro");

        assertThatThrownBy(() -> servicio.confirmar(comando("000000")))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CODIGO_WHATSAPP_INVALIDO));

        verify(desafioRepository).registrarIntentoFallido(d.getCodigo());
        verify(vinculoRepository, never()).reemplazar(any(), any());
    }

    @Test
    void confirmar_conElMaximoDeIntentosYaGastado_rechazaAunqueElCodigoSeaCorrecto() {
        cuentaYDesafio(desafio(5));

        assertThatThrownBy(() -> servicio.confirmar(comando("123456"))).isInstanceOf(BusinessException.class);

        verify(vinculoRepository, never()).reemplazar(any(), any());
        verify(desafioRepository, never()).marcarUsado(any());
    }

    @Test
    void confirmar_sinCodigoVigente_lanzaErr25() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(desafioRepository.buscarVigente(usuario.getCodigo(), "tel-hash", AHORA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.confirmar(comando("123456"))).isInstanceOf(BusinessException.class);

        verify(vinculoRepository, never()).reemplazar(any(), any());
    }

    @Test
    void confirmar_conCorreoSinCuenta_lanzaElMismoErr25() {
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.confirmar(comando("123456")))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CODIGO_WHATSAPP_INVALIDO));
    }

    @Test
    void confirmar_conCuentaDeOtroRol_lanzaErr25() {
        Usuario instructor = usuario.toBuilder()
            .roles(Set.of(Rol.builder().nombre(RolNombres.INSTRUCTOR).build())).build();
        when(usuarioRepository.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(instructor));

        assertThatThrownBy(() -> servicio.confirmar(comando("123456"))).isInstanceOf(BusinessException.class);

        verify(vinculoRepository, never()).reemplazar(any(), any());
    }
}
