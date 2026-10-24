package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EstudianteDetalle;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class ObtenerEstudianteServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final Clock RELOJ = Clock.fixed(
        LocalDate.of(2026, 10, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    private ObtenerEstudianteService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ObtenerEstudianteService(usuarioRepository,
            new EscuelaDelAdministrador(usuarioRepository), new EstudianteMapeador(RELOJ));
    }

    private void elAdministradorEsDeLaEscuela() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
    }

    @Test
    void obtener_conUnEstudianteDeLaEscuela_loDevuelveBuscandoEnLaEscuelaDelAdministrador() {
        elAdministradorEsDeLaEscuela();
        when(usuarioRepository.buscarEstudianteDeEscuela(ESCUELA, ESTUDIANTE)).thenReturn(Optional.of(
            Usuario.builder().codigo(ESTUDIANTE).email("ana@gmail.com").estado(EstadoUsuario.ACTIVO)
                .creadoEn(Instant.parse("2026-05-04T15:30:00Z"))
                .perfil(Perfil.builder().nombres("Ana").apellidos("Alfa").fechaNacimiento(LocalDate.of(2000, 1, 1))
                    .ci("AB1").sexo(SexoPersona.FEMENINO).categoriaSigla("SIN").build())
                .build()));

        EstudianteDetalle detalle = servicio.obtener(ADMIN, ESTUDIANTE);

        verify(usuarioRepository).buscarEstudianteDeEscuela(ESCUELA, ESTUDIANTE);
        assertThat(detalle.codigo()).isEqualTo(ESTUDIANTE);
        assertThat(detalle.nombreCompleto()).isEqualTo("Ana Alfa");
        assertThat(detalle.edad()).isEqualTo(26);
        assertThat(detalle.email()).isEqualTo("ana@gmail.com");
    }

    @Test
    void obtener_conUnEstudianteInexistenteOEstudianteDeOtraEscuela_lanzaErrIde20() {
        elAdministradorEsDeLaEscuela();
        when(usuarioRepository.buscarEstudianteDeEscuela(ESCUELA, ESTUDIANTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(ADMIN, ESTUDIANTE))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO));
    }

    @Test
    void obtener_conUnAdministradorSinEscuela_lanzaForbidden() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).build()));

        assertThatThrownBy(() -> servicio.obtener(ADMIN, ESTUDIANTE)).isInstanceOf(ForbiddenException.class);
    }
}
