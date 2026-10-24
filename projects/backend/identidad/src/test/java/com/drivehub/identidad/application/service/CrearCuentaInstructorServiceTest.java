package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CrearCuentaInstructorComando;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class CrearCuentaInstructorServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private RolRepositoryPort rolRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private GeocodificadorInversoPort geocodificador;

    private CrearCuentaInstructorService servicio;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servicio = new CrearCuentaInstructorService(new EscuelaDelAdministrador(usuarioRepository),
            new AltaCuenta(usuarioRepository, rolRepository, passwordHasher, geocodificador, reloj, 18),
            new PersonaInstructorMapeador(reloj, mock(PerfilArmador.class)));
    }

    private void elAdministradorEsDeLaEscuela() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
    }

    private void rolYGuardadoDisponibles() {
        Rol instructor = Rol.builder().codigo(UUID.randomUUID()).nombre(RolNombres.INSTRUCTOR).permisos(Set.of()).build();
        when(rolRepository.buscarPorNombre(RolNombres.INSTRUCTOR)).thenReturn(Optional.of(instructor));
        when(passwordHasher.hashear(any())).thenReturn("$2a$hash");
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.of("Av. Cañoto 100"));
        when(usuarioRepository.guardar(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            return u.toBuilder().creadoEn(Instant.parse("2026-10-23T12:00:00Z")).build();
        });
    }

    private static CrearCuentaInstructorComando comando(LocalDate nacimiento) {
        return new CrearCuentaInstructorComando(ADMIN, CODIGO, "Ivan", "Pérez", nacimiento, SexoPersona.MASCULINO,
            "ab12345", null, "ivan@gmail.com", "123456789", new BigDecimal("-17.7833"), new BigDecimal("-63.1821"));
    }

    @Test
    void crear_conDatosValidos_creaLaCuentaDeInstructorEnLaEscuelaDelAdministradorConElCodigoPedido() {
        elAdministradorEsDeLaEscuela();
        rolYGuardadoDisponibles();
        when(usuarioRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.empty());

        PersonaInstructor persona = servicio.crear(comando(LocalDate.of(1990, 1, 1)));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(guardado.capture());
        Usuario cuenta = guardado.getValue();
        assertThat(cuenta.getCodigo()).isEqualTo(CODIGO);
        assertThat(cuenta.getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(cuenta.isCorreoVerificado()).isFalse();
        assertThat(cuenta.getRoles()).extracting(Rol::getNombre).containsExactly(RolNombres.INSTRUCTOR);
        assertThat(cuenta.getPerfil().getCategoriaSigla()).isNull();
        assertThat(persona.codigo()).isEqualTo(CODIGO);
        assertThat(persona.nombreCompleto()).isEqualTo("Ivan Pérez");
        assertThat(persona.email()).isEqualTo("ivan@gmail.com");
    }

    @Test
    void crear_conUnCodigoQueYaExiste_lanzaErrSys01SinGuardar() {
        elAdministradorEsDeLaEscuela();
        when(usuarioRepository.buscarPorCodigo(CODIGO)).thenReturn(Optional.of(Usuario.builder().codigo(CODIGO).build()));

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(1990, 1, 1))))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conCorreoRepetido_lanzaErrIde01SinGuardar() {
        elAdministradorEsDeLaEscuela();
        when(usuarioRepository.existePorEmail("ivan@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(1990, 1, 1))))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_EN_USO));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conCiRepetido_lanzaErrIde19SinGuardar() {
        elAdministradorEsDeLaEscuela();
        when(usuarioRepository.existePorCi("AB12345")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(1990, 1, 1))))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CI_EN_USO));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conUnMenorDeEdad_lanzaErrIde02SinGuardar() {
        elAdministradorEsDeLaEscuela();

        assertThatThrownBy(() -> servicio.crear(comando(HOY.minusYears(18).plusDays(1))))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.MENOR_DE_EDAD));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conUnAdministradorSinEscuela_lanzaForbiddenSinGuardar() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(1990, 1, 1))))
            .isInstanceOf(ForbiddenException.class);
        verify(usuarioRepository, never()).guardar(any());
    }
}
