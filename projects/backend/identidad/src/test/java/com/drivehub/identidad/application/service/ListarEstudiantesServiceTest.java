package com.drivehub.identidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.domain.model.EstadoFiltroEstudiante;
import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.ListarEstudiantesComando;
import com.drivehub.identidad.domain.port.in.PaginaEstudiantes;
import com.drivehub.identidad.domain.port.out.FiltroEstudiantes;
import com.drivehub.identidad.domain.port.out.PaginaUsuarios;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * La autorización por rol NO se prueba acá (un servicio sin Spring no ejecuta @PreAuthorize): se
 * ejerce en la integración de los endpoints.
 */
@ExtendWith(MockitoExtension.class)
class ListarEstudiantesServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final Clock RELOJ = Clock.fixed(
        LocalDate.of(2026, 10, 23).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    private ListarEstudiantesService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ListarEstudiantesService(usuarioRepository,
            new EscuelaDelAdministrador(usuarioRepository), new EstudianteMapeador(RELOJ));
    }

    private void elAdministradorEsDeLaEscuela() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).escuelaCodigo(ESCUELA).build()));
    }

    private void elRepositorioDevuelve(List<Usuario> elementos, long total) {
        when(usuarioRepository.listarEstudiantesDeEscuela(any(), any(), anyInt(), anyInt()))
            .thenReturn(new PaginaUsuarios(elementos, total));
    }

    private static Usuario estudiante(String nombres, String apellidos) {
        return Usuario.builder().codigo(UUID.randomUUID()).email(nombres + "@gmail.com")
            .estado(EstadoUsuario.ACTIVO).creadoEn(Instant.parse("2026-05-04T15:30:00Z"))
            .perfil(Perfil.builder().nombres(nombres).apellidos(apellidos).fechaNacimiento(LocalDate.of(2000, 1, 1))
                .ci("AB1").sexo(SexoPersona.MASCULINO).categoriaSigla("SIN").build())
            .build();
    }

    private static ListarEstudiantesComando comando(Integer pagina, Integer tamano, String busqueda,
            EstadoFiltroEstudiante estado) {
        return new ListarEstudiantesComando(ADMIN, pagina, tamano, busqueda, estado);
    }

    @Test
    void listar_tomaLaEscuelaDeLaCuentaDelAdministradorYSePasaAlPuerto() {
        elAdministradorEsDeLaEscuela();
        elRepositorioDevuelve(List.of(), 0);

        servicio.listar(comando(null, null, null, null));

        verify(usuarioRepository).listarEstudiantesDeEscuela(eq(ESCUELA), any(), eq(1), eq(20));
    }

    @Test
    void listar_sinParametros_usaPagina1VeintePorPaginaYSinFiltros() {
        elAdministradorEsDeLaEscuela();
        elRepositorioDevuelve(List.of(estudiante("Ana", "Alfa")), 1);

        PaginaEstudiantes resultado = servicio.listar(comando(null, null, null, null));

        ArgumentCaptor<FiltroEstudiantes> filtro = ArgumentCaptor.forClass(FiltroEstudiantes.class);
        verify(usuarioRepository).listarEstudiantesDeEscuela(eq(ESCUELA), filtro.capture(), eq(1), eq(20));
        assertThat(filtro.getValue()).isEqualTo(new FiltroEstudiantes(null, null));
        assertThat(resultado.pagina()).isEqualTo(1);
        assertThat(resultado.tamano()).isEqualTo(20);
        assertThat(resultado.totalElementos()).isEqualTo(1);
        assertThat(resultado.totalPaginas()).isEqualTo(1);
        assertThat(resultado.elementos()).singleElement()
            .satisfies(fila -> assertThat(fila.nombreCompleto()).isEqualTo("Ana Alfa"));
    }

    @Test
    void listar_conTamanoMayorAlMaximo_loLimitaACien() {
        elAdministradorEsDeLaEscuela();
        elRepositorioDevuelve(List.of(), 250);

        PaginaEstudiantes resultado = servicio.listar(comando(2, 5000, null, null));

        verify(usuarioRepository).listarEstudiantesDeEscuela(eq(ESCUELA), any(), eq(2), eq(100));
        assertThat(resultado.tamano()).isEqualTo(100);
        assertThat(resultado.totalPaginas()).isEqualTo(3);
    }

    @Test
    void listar_conPaginaOTamanoInvalidos_usaLosValoresPorDefecto() {
        elAdministradorEsDeLaEscuela();
        elRepositorioDevuelve(List.of(), 0);

        PaginaEstudiantes resultado = servicio.listar(comando(0, -4, null, null));

        verify(usuarioRepository).listarEstudiantesDeEscuela(eq(ESCUELA), any(), eq(1), eq(20));
        assertThat(resultado.pagina()).isEqualTo(1);
        assertThat(resultado.tamano()).isEqualTo(20);
    }

    @Test
    void listar_conBusquedaEnBlanco_noFiltraPorNombre() {
        elAdministradorEsDeLaEscuela();
        elRepositorioDevuelve(List.of(), 0);

        servicio.listar(comando(null, null, "   ", null));

        verify(usuarioRepository).listarEstudiantesDeEscuela(
            eq(ESCUELA), eq(new FiltroEstudiantes(null, null)), eq(1), eq(20));
    }

    @Test
    void listar_conBusquedaYEstado_laRecortaYLosPasaAlPuerto() {
        elAdministradorEsDeLaEscuela();
        elRepositorioDevuelve(List.of(), 0);

        servicio.listar(comando(null, null, "  juan perez ", EstadoFiltroEstudiante.INACTIVO));

        verify(usuarioRepository).listarEstudiantesDeEscuela(eq(ESCUELA),
            eq(new FiltroEstudiantes("juan perez", EstadoFiltroEstudiante.INACTIVO)), eq(1), eq(20));
    }

    @Test
    void listar_sinResultados_noEsErrorYTieneCeroPaginas() {
        elAdministradorEsDeLaEscuela();
        elRepositorioDevuelve(List.of(), 0);

        PaginaEstudiantes resultado = servicio.listar(comando(null, null, "nadie", null));

        assertThat(resultado.elementos()).isEmpty();
        assertThat(resultado.totalElementos()).isZero();
        assertThat(resultado.totalPaginas()).isZero();
    }

    @Test
    void listar_conUnAdministradorSinEscuela_lanzaForbiddenSinConsultarEstudiantes() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.of(
            Usuario.builder().codigo(ADMIN).build()));

        assertThatThrownBy(() -> servicio.listar(comando(null, null, null, null)))
            .isInstanceOf(ForbiddenException.class);
        verify(usuarioRepository, never()).listarEstudiantesDeEscuela(any(), any(), anyInt(), anyInt());
    }

    @Test
    void listar_conUnaCuentaInexistente_lanzaForbidden() {
        when(usuarioRepository.buscarPorCodigo(ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.listar(comando(null, null, null, null)))
            .isInstanceOf(ForbiddenException.class);
    }
}
