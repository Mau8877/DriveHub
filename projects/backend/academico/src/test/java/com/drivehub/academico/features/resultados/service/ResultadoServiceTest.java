package com.drivehub.academico.features.resultados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.FiltroResultados;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;

@ExtendWith(MockitoExtension.class)
class ResultadoServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final String BEARER = "Bearer x";
    private static final Clock RELOJ = Clock.systemUTC().withZone(ZoneId.of("America/La_Paz"));

    @Mock
    private ResultadoRepository repository;
    @Mock
    private IdentidadClient identidad;

    private ResultadoService service;

    @BeforeEach
    void preparar() {
        service = new ResultadoService(repository, identidad, RELOJ);
    }

    private static Resultado nota() {
        Resultado r = new Resultado();
        r.setEscuelaCodigo(ESCUELA);
        r.setEstudianteCodigo(ESTUDIANTE);
        r.setOrigen(OrigenResultado.SIMULACRO);
        r.setTitulo("Simulacro: Básico");
        r.setPuntaje(80);
        r.setAprobado(true);
        // * 2026-11-03 02:00 UTC es todavía el 2 de noviembre en La Paz (UTC-4)
        r.setFecha(Instant.parse("2026-11-03T02:00:00Z"));
        r.inicializarCodigo();
        return r;
    }

    @Test
    void listarMios_usaElEstudianteDelTokenIgnoraElFiltroPorEstudianteYNoPideNombres() {
        FiltroResultados filtro = new FiltroResultados(OrigenResultado.SIMULACRO, UUID.randomUUID());
        when(repository.listarDeEstudiante(any(), any(), any())).thenReturn(new PageImpl<>(List.of(nota())));

        var pagina = service.listarMios(ESTUDIANTE, filtro, null, null);

        verify(repository).listarDeEstudiante(ESTUDIANTE, new FiltroResultados(OrigenResultado.SIMULACRO, null),
            PageRequest.of(0, 20));
        verify(identidad, never()).nombresDeEstudiantes(any(), any());
        assertThat(pagina.getContent()).singleElement().satisfies(r -> {
            assertThat(r.estudianteNombre()).isNull();
            assertThat(r.titulo()).isEqualTo("Simulacro: Básico");
            assertThat(r.dia()).isEqualTo(LocalDate.of(2026, 11, 2));
            assertThat(r.aprobado()).isTrue();
        });
    }

    @Test
    void listarDeEscuela_traeLasNotasDeLaEscuelaConElNombreDeCadaEstudiante() {
        FiltroResultados filtro = new FiltroResultados(null, ESTUDIANTE);
        when(repository.listarDeEscuela(any(), any(), any())).thenReturn(new PageImpl<>(List.of(nota())));
        when(identidad.nombresDeEstudiantes(any(), eq(BEARER))).thenReturn(Map.of(ESTUDIANTE, "Ana Pérez"));

        var pagina = service.listarDeEscuela(ESCUELA, BEARER, filtro, 3, 5000);

        verify(repository).listarDeEscuela(ESCUELA, filtro, PageRequest.of(2, 100));
        assertThat(pagina.getContent().get(0).estudianteNombre()).isEqualTo("Ana Pérez");
    }

    @Test
    void listarDeEscuela_sinNotas_noConsultaAIdentidad() {
        when(repository.listarDeEscuela(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.listarDeEscuela(ESCUELA, BEARER, new FiltroResultados(null, null), null, null);

        verify(identidad, never()).nombresDeEstudiantes(any(), any());
    }
}
