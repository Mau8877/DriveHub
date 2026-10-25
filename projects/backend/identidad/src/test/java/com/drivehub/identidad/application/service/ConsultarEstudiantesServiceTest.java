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

import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.PersonaEstudiante;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): ver
 * `ConsultarEstudiantesAutorizacionTest`.
 */
@ExtendWith(MockitoExtension.class)
class ConsultarEstudiantesServiceTest {

    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    private ConsultarEstudiantesService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ConsultarEstudiantesService(usuarioRepository, new EscuelaDelAdministrador(usuarioRepository));
    }

    private void elSolicitanteEsDeLaEscuela() {
        when(usuarioRepository.buscarPorCodigo(INSTRUCTOR)).thenReturn(Optional.of(
            Usuario.builder().codigo(INSTRUCTOR).escuelaCodigo(ESCUELA).build()));
    }

    private static Usuario estudiante(UUID codigo, String nombres, String apellidos) {
        return Usuario.builder().codigo(codigo).email("secreto@gmail.com")
            .perfil(Perfil.builder().nombres(nombres).apellidos(apellidos).ci("AB12345")
                .fechaNacimiento(LocalDate.of(2000, 1, 1)).build())
            .build();
    }

    @Test
    void consultar_pideSoloEnLaEscuelaDelSolicitante_quitaRepetidos_yDevuelveSoloElNombre() {
        elSolicitanteEsDeLaEscuela();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(usuarioRepository.buscarEstudiantesPorCodigos(eq(ESCUELA), any()))
            .thenReturn(List.of(estudiante(a, "Ana", "Pérez")));

        List<PersonaEstudiante> personas = servicio.consultar(INSTRUCTOR, List.of(a, b, a));

        verify(usuarioRepository).buscarEstudiantesPorCodigos(ESCUELA, List.of(a, b));
        assertThat(personas).containsExactly(new PersonaEstudiante(a, "Ana", "Pérez"));
    }

    @Test
    void consultar_unCodigoDeOtraEscuelaOInexistente_simplementeNoAparece() {
        elSolicitanteEsDeLaEscuela();
        when(usuarioRepository.buscarEstudiantesPorCodigos(eq(ESCUELA), any())).thenReturn(List.of());

        assertThat(servicio.consultar(INSTRUCTOR, List.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void consultar_sinCodigosOConMasDeCien_lanzaErrSys01() {
        elSolicitanteEsDeLaEscuela();
        List<UUID> ciento1 = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> servicio.consultar(INSTRUCTOR, List.of()))
            .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        assertThatThrownBy(() -> servicio.consultar(INSTRUCTOR, ciento1))
            .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        verify(usuarioRepository, never()).buscarEstudiantesPorCodigos(any(), any());
    }

    @Test
    void consultar_conExactamenteCienCodigos_loAcepta() {
        elSolicitanteEsDeLaEscuela();
        List<UUID> cien = IntStream.range(0, 100).mapToObj(i -> UUID.randomUUID()).toList();
        when(usuarioRepository.buscarEstudiantesPorCodigos(eq(ESCUELA), any())).thenReturn(List.of());

        assertThat(servicio.consultar(INSTRUCTOR, cien)).isEmpty();
    }
}
