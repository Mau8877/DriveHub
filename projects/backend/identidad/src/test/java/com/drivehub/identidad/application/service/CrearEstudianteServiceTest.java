package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.drivehub.identidad.domain.model.EstadoEstudiante;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.CrearEstudianteComando;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.out.CategoriaLicenciaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaNoDisponibleException;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class CrearEstudianteServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private RolRepositoryPort rolRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private GeocodificadorInversoPort geocodificador;
    @Mock
    private CategoriaLicenciaConsultaPort categoriaConsulta;

    private CrearEstudianteService servicio;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servicio = new CrearEstudianteService(new EscuelaDelAdministrador(usuarioRepository), categoriaConsulta,
            new AltaEstudiante(usuarioRepository, rolRepository, passwordHasher, geocodificador, reloj, 18),
            new EstudianteMapeador(reloj));
    }

    private void elAdministradorEsDeLaEscuela() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
    }

    private void rolYGuardadoDisponibles() {
        Rol estudiante = Rol.builder().codigo(UUID.randomUUID()).nombre(RolNombres.ESTUDIANTE).permisos(Set.of()).build();
        when(rolRepository.buscarPorNombre(RolNombres.ESTUDIANTE)).thenReturn(Optional.of(estudiante));
        when(passwordHasher.hashear(any())).thenReturn("$2a$hash");
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.of("Av. Cañoto 100"));
        when(usuarioRepository.guardar(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            return u.toBuilder().codigo(UUID.randomUUID()).creadoEn(Instant.parse("2026-10-23T12:00:00Z")).build();
        });
    }

    private static CrearEstudianteComando comando(LocalDate nacimiento, String categoria) {
        return new CrearEstudianteComando(ADMIN, "Ana", "Pérez", nacimiento, SexoPersona.FEMENINO, "ab12345", null,
            "ana@gmail.com", "123456789", new BigDecimal("-17.7833"), new BigDecimal("-63.1821"), categoria);
    }

    @Test
    void crear_conDatosValidosYSinCategoria_creaElEstudianteEnLaEscuelaDelAdministradorConSin() {
        elAdministradorEsDeLaEscuela();
        rolYGuardadoDisponibles();

        EstudianteDetalle detalle = servicio.crear(comando(LocalDate.of(2000, 1, 1), null));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(guardado.getValue().isCorreoVerificado()).isFalse();
        assertThat(guardado.getValue().getPerfil().getCategoriaSigla()).isEqualTo("SIN");
        assertThat(detalle.categoriaSigla()).isEqualTo("SIN");
        assertThat(detalle.estado()).isEqualTo(EstadoEstudiante.ACTIVO);
        assertThat(detalle.nombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(detalle.edad()).isEqualTo(26);
        assertThat(detalle.ci()).isEqualTo("AB12345");
        verifyNoInteractions(categoriaConsulta);
    }

    @Test
    void crear_conCategoriaEnBlanco_cuentaComoSinCategoriaYNoConsultaAEscuela() {
        elAdministradorEsDeLaEscuela();
        rolYGuardadoDisponibles();

        EstudianteDetalle detalle = servicio.crear(comando(LocalDate.of(2000, 1, 1), "  "));

        assertThat(detalle.categoriaSigla()).isEqualTo("SIN");
        verifyNoInteractions(categoriaConsulta);
    }

    @Test
    void crear_conUnaCategoriaValida_laValidaConEscuelaYLaGuardaEnMayusculas() {
        elAdministradorEsDeLaEscuela();
        rolYGuardadoDisponibles();
        when(categoriaConsulta.existe("B")).thenReturn(true);

        EstudianteDetalle detalle = servicio.crear(comando(LocalDate.of(2000, 1, 1), " b "));

        assertThat(detalle.categoriaSigla()).isEqualTo("B");
    }

    @Test
    void crear_conUnaCategoriaInexistente_lanzaErrIde21YNoGuardaNada() {
        elAdministradorEsDeLaEscuela();
        when(categoriaConsulta.existe("ZZZ")).thenReturn(false);

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(2000, 1, 1), "zzz")))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CATEGORIA_NO_EXISTE));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conCorreoRepetidoMenorDeEdadOCiRepetido_noGuardaNada() {
        elAdministradorEsDeLaEscuela();
        when(usuarioRepository.existePorEmail("ana@gmail.com")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(2000, 1, 1), null)))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CORREO_EN_USO));

        assertThatThrownBy(() -> servicio.crear(comando(HOY.minusYears(18).plusDays(1), null)))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.MENOR_DE_EDAD));

        when(usuarioRepository.existePorEmail("ana@gmail.com")).thenReturn(false);
        when(usuarioRepository.existePorCi("AB12345")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(2000, 1, 1), null)))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CI_EN_USO));

        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_cuandoEscuelaNoResponde_dejaSubirLaExcepcionYNoGuardaNada() {
        elAdministradorEsDeLaEscuela();
        when(categoriaConsulta.existe("B")).thenThrow(new EscuelaNoDisponibleException("caída"));

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(2000, 1, 1), "B")))
            .isInstanceOf(EscuelaNoDisponibleException.class);
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void crear_conUnAdministradorSinEscuela_lanzaForbiddenYNoGuardaNada() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(Usuario.builder().codigo(ADMIN).build()));

        assertThatThrownBy(() -> servicio.crear(comando(LocalDate.of(2000, 1, 1), null)))
            .isInstanceOf(ForbiddenException.class);
        verify(usuarioRepository, never()).guardar(any());
    }
}
