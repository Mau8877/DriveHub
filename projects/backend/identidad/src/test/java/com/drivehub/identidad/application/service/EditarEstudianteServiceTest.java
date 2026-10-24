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
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EditarEstudianteComando;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.out.CategoriaLicenciaConsultaPort;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class EditarEstudianteServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final BigDecimal LAT = new BigDecimal("-17.783300");
    private static final BigDecimal LON = new BigDecimal("-63.182100");

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private CategoriaLicenciaConsultaPort categoriaConsulta;
    @Mock
    private GeocodificadorInversoPort geocodificador;

    private EditarEstudianteService servicio;

    @BeforeEach
    void crearServicio() {
        Clock reloj = Clock.fixed(LocalDate.of(2026, 10, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servicio = new EditarEstudianteService(usuarioRepository, new EscuelaDelAdministrador(usuarioRepository),
            categoriaConsulta, geocodificador, new EstudianteMapeador(reloj, mock(PerfilArmador.class)));
    }

    private static Usuario estudianteActual() {
        return Usuario.builder().codigo(ESTUDIANTE).email("ana@gmail.com").passwordHash("$2a$hash")
            .escuelaCodigo(ESCUELA).estado(EstadoUsuario.ACTIVO).creadoEn(Instant.parse("2026-05-04T15:30:00Z"))
            .perfil(Perfil.builder().nombres("Ana").apellidos("Alfa").telefono("70000000")
                .fechaNacimiento(LocalDate.of(2000, 1, 1)).ci("AB12345").sexo(SexoPersona.FEMENINO)
                .direccion("Calle vieja").latitud(LAT).longitud(LON).categoriaSigla("SIN").build())
            .build();
    }

    private void elEstudianteEsDeLaEscuela() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
        when(usuarioRepository.buscarEstudianteDeEscuela(ESCUELA, ESTUDIANTE)).thenReturn(Optional.of(estudianteActual()));
    }

    private void actualizarDevuelveLoRecibido() {
        when(usuarioRepository.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static EditarEstudianteComando comando(String categoria, BigDecimal latitud, BigDecimal longitud) {
        return new EditarEstudianteComando(ADMIN, ESTUDIANTE, categoria, latitud, longitud);
    }

    private Usuario guardado() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).actualizar(captor.capture());
        return captor.getValue();
    }

    @Test
    void editar_cambiaLaCategoriaValidandolaConEscuela() {
        elEstudianteEsDeLaEscuela();
        actualizarDevuelveLoRecibido();
        when(categoriaConsulta.existe("B")).thenReturn(true);

        EstudianteDetalle detalle = servicio.editar(comando("b", null, null));

        assertThat(guardado().getPerfil().getCategoriaSigla()).isEqualTo("B");
        assertThat(detalle.categoriaSigla()).isEqualTo("B");
        verifyNoInteractions(geocodificador);
    }

    @Test
    void editar_conSinCategoria_noConsultaAEscuela() {
        elEstudianteEsDeLaEscuela();
        actualizarDevuelveLoRecibido();

        servicio.editar(comando("SIN", null, null));

        assertThat(guardado().getPerfil().getCategoriaSigla()).isEqualTo("SIN");
        verifyNoInteractions(categoriaConsulta);
    }

    @Test
    void editar_cambiaLaUbicacionYRecalculaLaDireccion() {
        elEstudianteEsDeLaEscuela();
        actualizarDevuelveLoRecibido();
        BigDecimal nuevaLat = new BigDecimal("-17.790000");
        BigDecimal nuevaLon = new BigDecimal("-63.190000");
        when(geocodificador.direccionDe(nuevaLat, nuevaLon)).thenReturn(Optional.of("Av. Banzer 500"));

        EstudianteDetalle detalle = servicio.editar(comando(null, nuevaLat, nuevaLon));

        Usuario actualizado = guardado();
        assertThat(actualizado.getPerfil().getLatitud()).isEqualByComparingTo(nuevaLat);
        assertThat(actualizado.getPerfil().getDireccion()).isEqualTo("Av. Banzer 500");
        assertThat(detalle.direccion()).isEqualTo("Av. Banzer 500");
        assertThat(actualizado.getPerfil().getCategoriaSigla()).isEqualTo("SIN");
    }

    @Test
    void editar_conElMismoPunto_noVuelveAConsultarElMapaYConservaLaDireccion() {
        elEstudianteEsDeLaEscuela();
        actualizarDevuelveLoRecibido();

        servicio.editar(comando(null, new BigDecimal("-17.7833"), new BigDecimal("-63.1821")));

        verifyNoInteractions(geocodificador);
        assertThat(guardado().getPerfil().getDireccion()).isEqualTo("Calle vieja");
    }

    @Test
    void editar_siNoSePuedeCalcularLaDireccion_laDejaEnBlancoSinError() {
        elEstudianteEsDeLaEscuela();
        actualizarDevuelveLoRecibido();
        when(geocodificador.direccionDe(any(), any())).thenReturn(Optional.empty());

        servicio.editar(comando(null, new BigDecimal("-17.79"), new BigDecimal("-63.19")));

        assertThat(guardado().getPerfil().getDireccion()).isNull();
    }

    @Test
    void editar_nuncaCambiaLosDatosPersonalesNiElAcceso() {
        elEstudianteEsDeLaEscuela();
        actualizarDevuelveLoRecibido();
        when(categoriaConsulta.existe("C")).thenReturn(true);

        servicio.editar(comando("C", new BigDecimal("-17.79"), new BigDecimal("-63.19")));

        Usuario actualizado = guardado();
        Usuario original = estudianteActual();
        assertThat(actualizado.getEmail()).isEqualTo(original.getEmail());
        assertThat(actualizado.getPasswordHash()).isEqualTo(original.getPasswordHash());
        assertThat(actualizado.getEstado()).isEqualTo(original.getEstado());
        assertThat(actualizado.getEscuelaCodigo()).isEqualTo(original.getEscuelaCodigo());
        assertThat(actualizado.getPerfil().getNombres()).isEqualTo("Ana");
        assertThat(actualizado.getPerfil().getApellidos()).isEqualTo("Alfa");
        assertThat(actualizado.getPerfil().getTelefono()).isEqualTo("70000000");
        assertThat(actualizado.getPerfil().getCi()).isEqualTo("AB12345");
        assertThat(actualizado.getPerfil().getSexo()).isEqualTo(SexoPersona.FEMENINO);
        assertThat(actualizado.getPerfil().getFechaNacimiento()).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    void editar_conUnEstudianteInexistenteODeOtraEscuela_lanzaErrIde20YNoGuardaNada() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
        when(usuarioRepository.buscarEstudianteDeEscuela(ESCUELA, ESTUDIANTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.editar(comando("B", null, null)))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO));
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void editar_conUnaCategoriaInexistente_lanzaErrIde21YNoGuardaNada() {
        elEstudianteEsDeLaEscuela();
        when(categoriaConsulta.existe("ZZZ")).thenReturn(false);

        assertThatThrownBy(() -> servicio.editar(comando("ZZZ", null, null)))
            .isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.CATEGORIA_NO_EXISTE));
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void editar_sinNingunCampo_lanzaValidacionYNoGuardaNada() {
        elEstudianteEsDeLaEscuela();

        assertThatThrownBy(() -> servicio.editar(comando(null, null, null)))
            .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        assertThatThrownBy(() -> servicio.editar(comando("  ", null, null))).isInstanceOf(ValidationException.class);
        verify(usuarioRepository, never()).actualizar(any());
    }

    @Test
    void editar_conUnaSolaCoordenada_lanzaValidacionYNoGuardaNada() {
        elEstudianteEsDeLaEscuela();

        assertThatThrownBy(() -> servicio.editar(comando(null, LAT, null))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> servicio.editar(comando("B", null, LON))).isInstanceOf(ValidationException.class);
        verify(usuarioRepository, never()).actualizar(any());
        verifyNoInteractions(categoriaConsulta);
    }
}
