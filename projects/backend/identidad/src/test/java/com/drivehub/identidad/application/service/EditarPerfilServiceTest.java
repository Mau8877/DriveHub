package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EditarPerfilComando;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;
import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class EditarPerfilServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T12:00:00Z");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private AlmacenFotosPort almacenFotos;
    @Mock
    private EscuelaConsultaPort escuelaConsulta;

    private EditarPerfilService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new EditarPerfilService(usuarioRepository, new PerfilArmador(almacenFotos, escuelaConsulta),
            Clock.fixed(AHORA, ZoneOffset.UTC), 18);
    }

    private Usuario cuenta() {
        return Usuario.builder().codigo(UUID.randomUUID()).email("ana@gmail.com").passwordHash("$2a$hash")
            .nombres("Ana").apellidos("Pérez").telefono("0991234567").fechaNacimiento(LocalDate.of(2000, 1, 1))
            .estado(EstadoUsuario.ACTIVO).roles(Set.of()).build();
    }

    private EditarPerfilComando comando(Usuario u, Optional<String> nombres, Optional<String> apellidos,
            Optional<String> telefono, Optional<LocalDate> fecha) {
        return new EditarPerfilComando(u.getCodigo(), nombres, apellidos, telefono, fecha);
    }

    private Usuario guardado(Usuario u, Optional<String> nombres, Optional<String> apellidos,
            Optional<String> telefono, Optional<LocalDate> fecha) {
        when(usuarioRepository.buscarPorCodigo(u.getCodigo())).thenReturn(Optional.of(u));
        when(usuarioRepository.actualizar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        servicio.editar(comando(u, nombres, apellidos, telefono, fecha));
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(captor.capture());
        return captor.getValue();
    }

    @Test
    void editar_conTodosLosCampos_losGuardaYDevuelveElPerfil() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.actualizar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        PerfilUsuario perfil = servicio.editar(comando(usuario, Optional.of("  María José "), Optional.of("Gómez"),
            Optional.of("0987654321"), Optional.of(LocalDate.of(1995, 5, 5))));

        assertThat(perfil.nombres()).isEqualTo("María José");
        assertThat(perfil.apellidos()).isEqualTo("Gómez");
        assertThat(perfil.telefono()).isEqualTo("0987654321");
        assertThat(perfil.fechaNacimiento()).isEqualTo(LocalDate.of(1995, 5, 5));
        assertThat(perfil.email()).isEqualTo("ana@gmail.com");
    }

    @Test
    void editar_soloCambiaLosCamposPresentes() {
        Usuario nuevo = guardado(cuenta(), Optional.of("Luisa"), Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(nuevo.getNombres()).isEqualTo("Luisa");
        assertThat(nuevo.getApellidos()).isEqualTo("Pérez");
        assertThat(nuevo.getTelefono()).isEqualTo("0991234567");
        assertThat(nuevo.getFechaNacimiento()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(nuevo.getEmail()).isEqualTo("ana@gmail.com");
        assertThat(nuevo.getPasswordHash()).isEqualTo("$2a$hash");
    }

    @Test
    void editar_conTelefonoVacio_loBorra() {
        assertThat(guardado(cuenta(), Optional.empty(), Optional.empty(), Optional.of(""), Optional.empty())
            .getTelefono()).isNull();
    }

    @Test
    void editar_sinNingunCampo_noGuarda() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        PerfilUsuario perfil = servicio.editar(
            comando(usuario, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        assertThat(perfil.nombres()).isEqualTo("Ana");
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void editar_conMenorDe18_lanzaErrIde02SinGuardar() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));
        LocalDate diecisieteAnios = LocalDate.of(2009, 10, 22);

        assertThatThrownBy(() -> servicio.editar(
                comando(usuario, Optional.of("Luisa"), Optional.empty(), Optional.empty(), Optional.of(diecisieteAnios))))
            .isInstanceOfSatisfying(ValidationException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.MENOR_DE_EDAD));
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void editar_conExactamente18Anios_loAcepta() {
        Usuario nuevo = guardado(cuenta(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(LocalDate.of(2008, 10, 21)));

        assertThat(nuevo.getFechaNacimiento()).isEqualTo(LocalDate.of(2008, 10, 21));
    }

    @Test
    void editar_conNombresEnBlanco_lanzaErrSys01SinGuardar() {
        Usuario usuario = cuenta();
        when(usuarioRepository.buscarPorCodigo(usuario.getCodigo())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.editar(
                comando(usuario, Optional.of("   "), Optional.empty(), Optional.empty(), Optional.empty())))
            .isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo("ERR_SYS_01"));
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void editar_conCuentaInexistente_lanzaErrIde09() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.editar(new EditarPerfilComando(codigo, Optional.of("Luisa"), Optional.empty(),
                Optional.empty(), Optional.empty())))
            .isInstanceOfSatisfying(UnauthorizedException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(IdentidadErrorCodes.SESION_INVALIDA));
    }
}
