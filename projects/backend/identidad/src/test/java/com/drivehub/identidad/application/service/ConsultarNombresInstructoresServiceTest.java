package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.NombreInstructor;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): ver
 * `ConsultarNombresInstructoresAutorizacionTest`.
 */
@ExtendWith(MockitoExtension.class)
class ConsultarNombresInstructoresServiceTest {

    private static final UUID SOLICITANTE = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    private ConsultarNombresInstructoresService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ConsultarNombresInstructoresService(usuarioRepository,
            new EscuelaDelAdministrador(usuarioRepository));
    }

    private void elSolicitanteEsDeLaEscuela() {
        when(usuarioRepository.buscarPorCodigo(SOLICITANTE)).thenReturn(Optional.of(
            Usuario.builder().codigo(SOLICITANTE).escuelaCodigo(ESCUELA).build()));
    }

    private static Usuario instructor(UUID codigo, String nombres, String apellidos) {
        return Usuario.builder().codigo(codigo).email("secreto@gmail.com")
            .perfil(Perfil.builder().nombres(nombres).apellidos(apellidos).ci("AB12345").telefono("70000000")
                .fechaNacimiento(LocalDate.of(1990, 1, 1)).build())
            .build();
    }

    @Test
    void consultar_pideSoloEnLaEscuelaDelSolicitante_quitaRepetidos_yDevuelveSoloElNombre() {
        elSolicitanteEsDeLaEscuela();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(usuarioRepository.buscarInstructoresPorCodigos(eq(ESCUELA), any()))
            .thenReturn(List.of(instructor(a, "Ivan", "Alfa")));

        List<NombreInstructor> nombres = servicio.consultar(SOLICITANTE, List.of(a, b, a));

        verify(usuarioRepository).buscarInstructoresPorCodigos(ESCUELA, List.of(a, b));
        assertThat(nombres).containsExactly(new NombreInstructor(a, "Ivan Alfa"));
    }

    @Test
    void consultar_unCodigoDeOtraEscuelaOEstudianteOInexistente_simplementeNoAparece() {
        elSolicitanteEsDeLaEscuela();
        when(usuarioRepository.buscarInstructoresPorCodigos(eq(ESCUELA), any())).thenReturn(List.of());

        assertThat(servicio.consultar(SOLICITANTE, List.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void consultar_sinCodigosOConMasDeCien_lanzaErrSys01() {
        elSolicitanteEsDeLaEscuela();
        List<UUID> ciento1 = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> servicio.consultar(SOLICITANTE, List.of()))
            .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        assertThatThrownBy(() -> servicio.consultar(SOLICITANTE, ciento1))
            .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        verify(usuarioRepository, never()).buscarInstructoresPorCodigos(any(), any());
    }

    @Test
    void consultar_conExactamenteCienCodigos_loAcepta() {
        elSolicitanteEsDeLaEscuela();
        List<UUID> cien = IntStream.range(0, 100).mapToObj(i -> UUID.randomUUID()).toList();
        when(usuarioRepository.buscarInstructoresPorCodigos(eq(ESCUELA), any())).thenReturn(List.of());

        assertThat(servicio.consultar(SOLICITANTE, cien)).isEmpty();
    }

    @Test
    void consultar_conUnaCuentaSinEscuela_lanzaForbidden() {
        when(usuarioRepository.buscarPorCodigo(SOLICITANTE)).thenReturn(Optional.of(
            Usuario.builder().codigo(SOLICITANTE).build()));

        assertThatThrownBy(() -> servicio.consultar(SOLICITANTE, List.of(UUID.randomUUID())))
            .isInstanceOf(ForbiddenException.class);
    }
}
