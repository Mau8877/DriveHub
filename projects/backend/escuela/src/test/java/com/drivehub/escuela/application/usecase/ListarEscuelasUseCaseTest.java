package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.EscuelaOutput;
import com.drivehub.escuela.application.dto.ListarEscuelasInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.domain.entity.Escuela;
import com.drivehub.escuela.domain.entity.EstadoEscuela;
import com.drivehub.escuela.domain.repository.EscuelaRepository;
import com.drivehub.escuela.domain.repository.FiltroEscuelas;
import com.drivehub.escuela.domain.repository.Pagina;

@ExtendWith(MockitoExtension.class)
class ListarEscuelasUseCaseTest {

    @Mock
    private EscuelaRepository escuelaRepository;

    @InjectMocks
    private ListarEscuelasUseCase useCase;

    private static Escuela escuela(String slug, boolean activa) {
        return Escuela.builder().codigo(UUID.randomUUID()).slug(slug).nombre("Escuela " + slug)
            .creadoEn(Instant.parse("2026-10-21T10:00:00Z")).eliminadaEn(activa ? null : Instant.now()).build();
    }

    private void repositorioDevuelve(List<Escuela> elementos, long total) {
        when(escuelaRepository.listar(any(), anyInt(), anyInt())).thenReturn(new Pagina<>(elementos, total));
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    @Test
    void ejecutar_sinParametros_usaPaginaUnoYVeinteElementosYNoFiltra() {
        repositorioDevuelve(List.of(), 0);

        useCase.ejecutar(new ListarEscuelasInput(null, null, null, null));

        verify(escuelaRepository).listar(new FiltroEscuelas(null, null), 1, 20);
    }

    @Test
    void ejecutar_conTamanoMayorAlMaximo_loRecortaACien() {
        repositorioDevuelve(List.of(), 0);

        PaginaOutput<EscuelaOutput> pagina = useCase.ejecutar(new ListarEscuelasInput(2, 500, null, null));

        verify(escuelaRepository).listar(any(), eq(2), eq(100));
        assertThat(pagina.tamano()).isEqualTo(100);
    }

    @Test
    void ejecutar_conPaginaOTamanoInvalidos_vuelveALosValoresPorDefecto() {
        repositorioDevuelve(List.of(), 0);

        useCase.ejecutar(new ListarEscuelasInput(0, -3, null, null));

        verify(escuelaRepository).listar(any(), eq(1), eq(20));
    }

    @Test
    void ejecutar_pasaLaBusquedaYElEstadoAlRepositorio() {
        repositorioDevuelve(List.of(), 0);

        useCase.ejecutar(new ListarEscuelasInput(1, 10, "sur", EstadoEscuela.INACTIVA));

        ArgumentCaptor<FiltroEscuelas> filtro = ArgumentCaptor.forClass(FiltroEscuelas.class);
        verify(escuelaRepository).listar(filtro.capture(), eq(1), eq(10));
        assertThat(filtro.getValue()).isEqualTo(new FiltroEscuelas("sur", EstadoEscuela.INACTIVA));
    }

    @Test
    void ejecutar_traduceElEstadoYCalculaElTotalDePaginas() {
        repositorioDevuelve(List.of(escuela("viva", true), escuela("muerta", false)), 45);

        PaginaOutput<EscuelaOutput> pagina = useCase.ejecutar(new ListarEscuelasInput(1, 20, null, null));

        assertThat(pagina.elementos()).extracting(EscuelaOutput::estado)
            .containsExactly(EstadoEscuela.ACTIVA, EstadoEscuela.INACTIVA);
        assertThat(pagina.elementos().get(0).slug()).isEqualTo("viva");
        assertThat(pagina.totalElementos()).isEqualTo(45);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
        assertThat(pagina.pagina()).isEqualTo(1);
    }

    @Test
    void ejecutar_sinResultados_devuelveCeroElementosYCeroPaginas() {
        repositorioDevuelve(List.of(), 0);

        PaginaOutput<EscuelaOutput> pagina = useCase.ejecutar(new ListarEscuelasInput(1, 20, "nada", null));

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
    }
}
