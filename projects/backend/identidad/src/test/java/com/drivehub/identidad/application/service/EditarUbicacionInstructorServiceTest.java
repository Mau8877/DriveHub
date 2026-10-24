package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.PersonaInstructor;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class EditarUbicacionInstructorServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final BigDecimal LAT = new BigDecimal("-17.783300");
    private static final BigDecimal LON = new BigDecimal("-63.182100");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private GeocodificadorInversoPort geocodificador;

    private EditarUbicacionInstructorService servicio;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(LocalDate.of(2026, 10, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servicio = new EditarUbicacionInstructorService(usuarioRepository, new EscuelaDelAdministrador(usuarioRepository),
            geocodificador, new PersonaInstructorMapeador(reloj, mock(PerfilArmador.class)));
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
    }

    private static Usuario instructor() {
        return Usuario.builder().codigo(INSTRUCTOR).email("ivan@gmail.com").escuelaCodigo(ESCUELA)
            .creadoEn(Instant.parse("2026-05-04T15:30:00Z"))
            .perfil(Perfil.builder().nombres("Ivan").apellidos("Pérez").fechaNacimiento(LocalDate.of(1990, 1, 1))
                .ci("AB12345").sexo(SexoPersona.MASCULINO).direccion("Dirección vieja").latitud(LAT).longitud(LON)
                .build())
            .build();
    }

    @Test
    void editar_conOtroPunto_recalculaLaDireccionYSoloCambiaLaUbicacion() {
        when(usuarioRepository.buscarInstructorDeEscuela(ESCUELA, INSTRUCTOR)).thenReturn(Optional.of(instructor()));
        when(geocodificador.direccionDe(new BigDecimal("-17.800000"), new BigDecimal("-63.200000")))
            .thenReturn(Optional.of("Av. Nueva 10"));
        when(usuarioRepository.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonaInstructor persona = servicio.editar(ADMIN, INSTRUCTOR,
            new BigDecimal("-17.800000"), new BigDecimal("-63.200000"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(guardado.capture());
        Perfil perfil = guardado.getValue().getPerfil();
        assertThat(perfil.getDireccion()).isEqualTo("Av. Nueva 10");
        assertThat(perfil.getLatitud()).isEqualByComparingTo("-17.8");
        assertThat(perfil.getNombres()).isEqualTo("Ivan");
        assertThat(perfil.getCi()).isEqualTo("AB12345");
        assertThat(guardado.getValue().getEmail()).isEqualTo("ivan@gmail.com");
        assertThat(persona.direccion()).isEqualTo("Av. Nueva 10");
    }

    @Test
    void editar_conElMismoPunto_noConsultaElMapa() {
        when(usuarioRepository.buscarInstructorDeEscuela(ESCUELA, INSTRUCTOR)).thenReturn(Optional.of(instructor()));
        when(usuarioRepository.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonaInstructor persona = servicio.editar(ADMIN, INSTRUCTOR, new BigDecimal("-17.7833"), new BigDecimal("-63.1821"));

        verifyNoInteractions(geocodificador);
        assertThat(persona.direccion()).isEqualTo("Dirección vieja");
    }

    @Test
    void editar_siElGeocodificadorNoResponde_dejaLaDireccionEnBlanco() {
        when(usuarioRepository.buscarInstructorDeEscuela(ESCUELA, INSTRUCTOR)).thenReturn(Optional.of(instructor()));
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.empty());
        when(usuarioRepository.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonaInstructor persona = servicio.editar(ADMIN, INSTRUCTOR, new BigDecimal("-17.9"), new BigDecimal("-63.3"));

        assertThat(persona.direccion()).isNull();
    }

    @Test
    void editar_conUnInstructorQueNoEsDeLaEscuela_lanzaErrIde22SinGuardar() {
        when(usuarioRepository.buscarInstructorDeEscuela(ESCUELA, INSTRUCTOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.editar(ADMIN, INSTRUCTOR, LAT, LON))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.INSTRUCTOR_NO_ENCONTRADO));
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void editar_conUnaSolaCoordenada_lanzaErrSys01SinGuardar() {
        when(usuarioRepository.buscarInstructorDeEscuela(ESCUELA, INSTRUCTOR)).thenReturn(Optional.of(instructor()));

        assertThatThrownBy(() -> servicio.editar(ADMIN, INSTRUCTOR, LAT, null))
            .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        verify(usuarioRepository, never()).actualizar(any());
    }
}
