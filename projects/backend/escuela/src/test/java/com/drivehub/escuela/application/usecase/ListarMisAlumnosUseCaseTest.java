package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.repository.AsignacionAlumnoRepository;
import com.drivehub.escuela.domain.repository.Pagina;

/**
 * La autorización por rol NO se prueba acá (un caso de uso sin Spring no ejecuta @PreAuthorize): ver
 * `MisAlumnosAutorizacionTest`.
 */
@ExtendWith(MockitoExtension.class)
class ListarMisAlumnosUseCaseTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();

    @Mock
    private AsignacionAlumnoRepository asignaciones;

    @InjectMocks
    private ListarMisAlumnosUseCase listar;

    @InjectMocks
    private ConfirmarMiAlumnoUseCase confirmar;

    @Test
    void listar_sinParametros_pideLaPagina1DeVeinte() {
        UUID alumno = UUID.randomUUID();
        when(asignaciones.buscarEstudiantesDeInstructor(ESCUELA, INSTRUCTOR, 1, 20))
            .thenReturn(new Pagina<>(List.of(alumno), 1));

        PaginaOutput<UUID> pagina = listar.ejecutar(ESCUELA, INSTRUCTOR, null, null);

        assertThat(pagina.elementos()).containsExactly(alumno);
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamano()).isEqualTo(20);
        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
    }

    @Test
    void listar_conTamanoMayorAlTope_usaCien_yConPaginaInvalida_usaLaPrimera() {
        when(asignaciones.buscarEstudiantesDeInstructor(ESCUELA, INSTRUCTOR, 1, 100))
            .thenReturn(new Pagina<>(List.of(), 250));

        PaginaOutput<UUID> pagina = listar.ejecutar(ESCUELA, INSTRUCTOR, 0, 500);

        assertThat(pagina.tamano()).isEqualTo(100);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    void listar_sinAlumnos_devuelveUnaPaginaVaciaSinPaginas() {
        when(asignaciones.buscarEstudiantesDeInstructor(ESCUELA, INSTRUCTOR, 1, 20))
            .thenReturn(new Pagina<>(List.of(), 0));

        PaginaOutput<UUID> pagina = listar.ejecutar(ESCUELA, INSTRUCTOR, 1, 20);

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
    }

    @Test
    void confirmar_deUnAlumnoVigente_devuelveSuCodigo() {
        UUID alumno = UUID.randomUUID();
        when(asignaciones.existeAsignacionActiva(ESCUELA, INSTRUCTOR, alumno)).thenReturn(true);

        assertThat(confirmar.ejecutar(ESCUELA, INSTRUCTOR, alumno)).isEqualTo(alumno);
    }

    @Test
    void confirmar_deUnAlumnoAjenoOInexistente_lanzaErrEsc15() {
        UUID alumno = UUID.randomUUID();
        when(asignaciones.existeAsignacionActiva(ESCUELA, INSTRUCTOR, alumno)).thenReturn(false);

        assertThatThrownBy(() -> confirmar.ejecutar(ESCUELA, INSTRUCTOR, alumno))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ESC_15"));
        verify(asignaciones).existeAsignacionActiva(ESCUELA, INSTRUCTOR, alumno);
    }
}
