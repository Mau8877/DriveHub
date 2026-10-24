package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La regla compartida por el registro (CU01) y el alta del Administrador (CU08). El registro tiene
 * además su propio test, que sigue ejerciendo todo el flujo a través de esta clase.
 */
@ExtendWith(MockitoExtension.class)
class AltaEstudianteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private RolRepositoryPort rolRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private GeocodificadorInversoPort geocodificador;

    private AltaEstudiante alta;

    @BeforeEach
    void crearAlta() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        alta = new AltaEstudiante(usuarioRepository, rolRepository, passwordHasher, geocodificador, reloj, 18);
    }

    private static AltaEstudiante.Datos datos(LocalDate nacimiento) {
        return new AltaEstudiante.Datos(" Ana ", "Pérez", nacimiento, " Ana@Gmail.com ", "123456789", "  ",
            " ab 12345 ", SexoPersona.FEMENINO, new BigDecimal("-17.783300"), new BigDecimal("-63.182100"));
    }

    private void rolYGuardadoDisponibles() {
        Rol estudiante = Rol.builder().codigo(UUID.randomUUID()).nombre(RolNombres.ESTUDIANTE).permisos(Set.of()).build();
        when(rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE)).thenReturn(Optional.of(estudiante));
        when(passwordHasher.hashear("123456789")).thenReturn("$2a$hash");
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.of("Av. Cañoto 100"));
        when(usuarioRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void crear_conDatosValidos_creaLaCuentaActivaConLaEscuelaYLaCategoriaRecibidas() {
        rolYGuardadoDisponibles();

        Usuario creado = alta.crear(datos(LocalDate.of(2000, 1, 1)), () -> ESCUELA, "B");

        assertThat(creado.getEmail()).isEqualTo("ana@gmail.com");
        assertThat(creado.getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(creado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(creado.isCorreoVerificado()).isFalse();
        assertThat(creado.getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(creado.getRoles()).extracting(Rol::getNombre).containsExactly(RolNombres.ESTUDIANTE);
        assertThat(creado.getPerfil().getNombres()).isEqualTo("Ana");
        assertThat(creado.getPerfil().getCi()).isEqualTo("AB12345");
        assertThat(creado.getPerfil().getTelefono()).isNull();
        assertThat(creado.getPerfil().getDireccion()).isEqualTo("Av. Cañoto 100");
        assertThat(creado.getPerfil().getCategoriaSigla()).isEqualTo("B");
    }

    @Test
    void crear_siElGeocodificadorNoResponde_guardaLaDireccionEnBlanco() {
        rolYGuardadoDisponibles();
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.empty());

        Usuario creado = alta.crear(datos(LocalDate.of(2000, 1, 1)), () -> ESCUELA, "SIN");

        assertThat(creado.getPerfil().getDireccion()).isNull();
        assertThat(creado.getPerfil().getLatitud()).isEqualByComparingTo("-17.7833");
    }

    @Test
    void crear_conUnMenorDeEdad_lanzaErrIde02SinPreguntarLaEscuelaNiGuardarNada() {
        AtomicBoolean preguntoLaEscuela = new AtomicBoolean(false);

        assertThatThrownBy(() -> alta.crear(datos(HOY.minusYears(18).plusDays(1)), () -> {
            preguntoLaEscuela.set(true);
            return ESCUELA;
        }, "SIN")).isInstanceOfSatisfying(ValidationException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.MENOR_DE_EDAD));

        assertThat(preguntoLaEscuela).isFalse();
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conCorreoRepetido_lanzaErrIde01YNoGuardaNada() {
        when(usuarioRepository.existePorEmail("ana@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> alta.crear(datos(LocalDate.of(2000, 1, 1)), () -> ESCUELA, "SIN"))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_EN_USO));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conCiRepetido_lanzaErrIde19YNoGuardaNada() {
        when(usuarioRepository.existePorCi("AB12345")).thenReturn(true);

        assertThatThrownBy(() -> alta.crear(datos(LocalDate.of(2000, 1, 1)), () -> ESCUELA, "SIN"))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CI_EN_USO));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_hasheaLaContrasenaYNuncaGuardaLaOriginal() {
        rolYGuardadoDisponibles();
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);

        alta.crear(datos(LocalDate.of(2000, 1, 1)), () -> ESCUELA, "SIN");

        verify(usuarioRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getPasswordHash()).doesNotContain("123456789");
    }
}
