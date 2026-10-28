package com.drivehub.academico.features.simulacros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
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
import org.springframework.data.domain.PageImpl;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.preguntas.model.OpcionPregunta;
import com.drivehub.academico.features.preguntas.model.Pregunta;
import com.drivehub.academico.features.preguntas.repository.PreguntaRepository;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;
import com.drivehub.academico.features.simulacros.dto.FinalizarIntentoRequest;
import com.drivehub.academico.features.simulacros.dto.FinalizarIntentoRequest.RespuestaRequest;
import com.drivehub.academico.features.simulacros.dto.IntentoIniciadoResponse;
import com.drivehub.academico.features.simulacros.dto.IntentoResultadoResponse;
import com.drivehub.academico.features.simulacros.enums.EstadoIntento;
import com.drivehub.academico.features.simulacros.model.IntentoSimulacro;
import com.drivehub.academico.features.simulacros.model.PreguntaIntento;
import com.drivehub.academico.features.simulacros.model.ReglaSimulacro;
import com.drivehub.academico.features.simulacros.model.SimulacroConfig;
import com.drivehub.academico.features.simulacros.repository.IntentoSimulacroRepository;
import com.drivehub.academico.features.simulacros.repository.SimulacroConfigRepository;

@ExtendWith(MockitoExtension.class)
class IntentoSimulacroServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID SIMULACRO = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-11-02T16:00:00Z");

    @Mock
    private SimulacroConfigRepository simulacroRepository;
    @Mock
    private IntentoSimulacroRepository intentoRepository;
    @Mock
    private PreguntaRepository preguntaRepository;
    @Mock
    private ResultadoRepository resultadoRepository;

    private IntentoSimulacroService service;
    private Clock reloj;

    @BeforeEach
    void preparar() {
        reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new IntentoSimulacroService(simulacroRepository, intentoRepository, preguntaRepository,
            resultadoRepository, reloj);
    }

    private static SimulacroConfig simulacro(boolean activo) {
        SimulacroConfig s = new SimulacroConfig();
        s.setEscuelaCodigo(ESCUELA);
        s.setNombre("Básico");
        s.setCategoria("B");
        s.setDuracionMin(20);
        s.setNroPreguntas(3);
        s.setPuntajeAprobacion(70);
        s.setReglas(List.of(new ReglaSimulacro("Señales", 2), new ReglaSimulacro("Prioridad", 1)));
        s.setActivo(activo);
        s.setId(7L);
        s.inicializarCodigo();
        return s;
    }

    private static Pregunta pregunta(String tema, long id) {
        Pregunta p = new Pregunta();
        p.setId(id);
        p.setTema(tema);
        p.setEnunciado("Pregunta " + id);
        p.setTipo(TipoPregunta.OPCION_UNICA);
        p.setOpciones(List.of(new OpcionPregunta("A", "Correcta", true), new OpcionPregunta("B", "Mala", false)));
        p.setExplicacion("Porque sí");
        p.inicializarCodigo();
        return p;
    }

    private void conSimulacro(SimulacroConfig s) {
        when(simulacroRepository.findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull(ESCUELA, SIMULACRO))
            .thenReturn(Optional.of(s));
    }

    private void conBanco() {
        when(preguntaRepository.activasDelTema(any(), any(), org.mockito.ArgumentMatchers.eq("Señales")))
            .thenReturn(List.of(pregunta("Señales", 1), pregunta("Señales", 2), pregunta("Señales", 3)));
        when(preguntaRepository.activasDelTema(any(), any(), org.mockito.ArgumentMatchers.eq("Prioridad")))
            .thenReturn(List.of(pregunta("Prioridad", 4)));
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    private static IntentoSimulacro intento(EstadoIntento estado, Instant iniciado) {
        IntentoSimulacro i = new IntentoSimulacro();
        i.setId(9L);
        i.setEscuelaCodigo(ESCUELA);
        i.setSimulacroCodigo(SIMULACRO);
        i.setSimulacroNombre("Básico");
        i.setDuracionMin(20);
        i.setPuntajeAprobacion(70);
        i.setEstudianteCodigo(ESTUDIANTE);
        i.setEstado(estado);
        i.setIniciadoEn(iniciado);
        i.setPreguntas(List.of(
            new PreguntaIntento(1L, UUID.randomUUID(), "P1", TipoPregunta.OPCION_UNICA,
                List.of(new OpcionPregunta("A", "Sí", true), new OpcionPregunta("B", "No", false)), "Expl", List.of(), false),
            new PreguntaIntento(2L, UUID.randomUUID(), "P2", TipoPregunta.OPCION_MULTIPLE,
                List.of(new OpcionPregunta("A", "Uno", true), new OpcionPregunta("B", "Dos", true),
                    new OpcionPregunta("C", "Tres", false)), null, List.of(), false)));
        i.inicializarCodigo();
        return i;
    }

    private void conIntento(IntentoSimulacro i) {
        when(intentoRepository.findByEstudianteCodigoAndCodigoIntentoSimulacroAndDeletedAtIsNull(
            ESTUDIANTE, i.getCodigoIntentoSimulacro())).thenReturn(Optional.of(i));
    }

    // ------------------------------------------------------------------ disponibles e iniciar

    @Test
    void disponibles_listaLosActivosDeLaEscuelaSinLasReglas() {
        when(simulacroRepository.findByEscuelaCodigoAndActivoTrueAndDeletedAtIsNullOrderByNombreAsc(ESCUELA))
            .thenReturn(List.of(simulacro(true)));

        var lista = service.disponibles(ESCUELA);

        assertThat(lista).singleElement().satisfies(s -> {
            assertThat(s.nombre()).isEqualTo("Básico");
            assertThat(s.nroPreguntas()).isEqualTo(3);
        });
    }

    @Test
    void iniciar_armaLasPreguntasPorReglaYNoMuestraLaRespuestaCorrecta() {
        conSimulacro(simulacro(true));
        conBanco();
        when(intentoRepository.findByEstudianteCodigoAndSimulacroCodigoAndEstadoAndDeletedAtIsNull(
            ESTUDIANTE, SIMULACRO, EstadoIntento.EN_CURSO)).thenReturn(List.of());
        when(intentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        IntentoIniciadoResponse respuesta = service.iniciar(ESCUELA, ESTUDIANTE, SIMULACRO);

        assertThat(respuesta.preguntas()).hasSize(3);
        assertThat(respuesta.venceEn()).isEqualTo(AHORA.plusSeconds(20 * 60));
        assertThat(respuesta.preguntas().get(0).opciones().get(0)).hasFieldOrProperty("texto").hasFieldOrProperty("id");
        ArgumentCaptor<IntentoSimulacro> guardado = ArgumentCaptor.forClass(IntentoSimulacro.class);
        verify(intentoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoIntento.EN_CURSO);
        assertThat(guardado.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(guardado.getValue().getSimulacroId()).isEqualTo(7L);
        // * Dos de Señales (1 a 3) y la única de Prioridad (4)
        assertThat(guardado.getValue().getPreguntas()).extracting(PreguntaIntento::enunciado)
            .hasSize(3).contains("Pregunta 4").doesNotHaveDuplicates();
    }

    @Test
    void iniciar_conSimulacroInexistenteODesactivado_lanzaErrAca21() {
        conSimulacro(simulacro(false));

        esperaCodigo(() -> service.iniciar(ESCUELA, ESTUDIANTE, SIMULACRO), AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO);

        when(simulacroRepository.findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull(ESCUELA, SIMULACRO))
            .thenReturn(Optional.empty());
        esperaCodigo(() -> service.iniciar(ESCUELA, ESTUDIANTE, SIMULACRO), AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO);
        verify(intentoRepository, never()).save(any());
    }

    @Test
    void iniciar_sinPreguntasSuficientesEnElBanco_lanzaErrAca28() {
        conSimulacro(simulacro(true));
        when(intentoRepository.findByEstudianteCodigoAndSimulacroCodigoAndEstadoAndDeletedAtIsNull(
            ESTUDIANTE, SIMULACRO, EstadoIntento.EN_CURSO)).thenReturn(List.of());
        when(preguntaRepository.activasDelTema(any(), any(), any())).thenReturn(List.of(pregunta("Señales", 1)));

        esperaCodigo(() -> service.iniciar(ESCUELA, ESTUDIANTE, SIMULACRO), AcademicoErrorCodes.SIMULACRO_SIN_PREGUNTAS);
        verify(intentoRepository, never()).save(any());
    }

    @Test
    void iniciar_conUnIntentoVigenteEnCurso_lanzaErrAca29_peroUnoVencidoSeExpiraYSigue() {
        conSimulacro(simulacro(true));
        IntentoSimulacro vigente = intento(EstadoIntento.EN_CURSO, AHORA.minusSeconds(60));
        when(intentoRepository.findByEstudianteCodigoAndSimulacroCodigoAndEstadoAndDeletedAtIsNull(
            ESTUDIANTE, SIMULACRO, EstadoIntento.EN_CURSO)).thenReturn(List.of(vigente));

        esperaCodigo(() -> service.iniciar(ESCUELA, ESTUDIANTE, SIMULACRO), AcademicoErrorCodes.INTENTO_EN_CURSO);

        IntentoSimulacro vencido = intento(EstadoIntento.EN_CURSO, AHORA.minusSeconds(3600));
        when(intentoRepository.findByEstudianteCodigoAndSimulacroCodigoAndEstadoAndDeletedAtIsNull(
            ESTUDIANTE, SIMULACRO, EstadoIntento.EN_CURSO)).thenReturn(List.of(vencido));
        conBanco();
        when(intentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.iniciar(ESCUELA, ESTUDIANTE, SIMULACRO);

        assertThat(vencido.getEstado()).isEqualTo(EstadoIntento.EXPIRADO);
        assertThat(vencido.getPuntaje()).isZero();
    }

    // ------------------------------------------------------------------ finalizar

    @Test
    void finalizar_califica_unaOpcionMultipleSoloAciertaConElConjuntoExactoYEscribeLaNota() {
        IntentoSimulacro i = intento(EstadoIntento.EN_CURSO, AHORA.minusSeconds(300));
        conIntento(i);
        when(intentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID p1 = i.getPreguntas().get(0).preguntaCodigo();
        UUID p2 = i.getPreguntas().get(1).preguntaCodigo();

        IntentoResultadoResponse resultado = service.finalizar(ESTUDIANTE, i.getCodigoIntentoSimulacro(),
            new FinalizarIntentoRequest(List.of(new RespuestaRequest(p1, List.of("A")),
                new RespuestaRequest(p2, List.of("A")))));

        assertThat(resultado.estado()).isEqualTo(EstadoIntento.FINALIZADO);
        assertThat(resultado.aciertos()).isEqualTo(1);
        assertThat(resultado.total()).isEqualTo(2);
        assertThat(resultado.puntaje()).isEqualTo(50.0);
        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.preguntas().get(0).correcta()).isTrue();
        assertThat(resultado.preguntas().get(1).correcta()).isFalse();
        assertThat(resultado.preguntas().get(0).explicacion()).isEqualTo("Expl");
        ArgumentCaptor<Resultado> nota = ArgumentCaptor.forClass(Resultado.class);
        verify(resultadoRepository).save(nota.capture());
        assertThat(nota.getValue().getOrigen()).isEqualTo(OrigenResultado.SIMULACRO);
        assertThat(nota.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(nota.getValue().getPuntaje()).isEqualTo(50.0);
        assertThat(nota.getValue().isAprobado()).isFalse();
    }

    @Test
    void finalizar_conTodoBienApruebaYIgnoraOpcionesInexistentesYPreguntasAjenas() {
        IntentoSimulacro i = intento(EstadoIntento.EN_CURSO, AHORA.minusSeconds(300));
        conIntento(i);
        when(intentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IntentoResultadoResponse resultado = service.finalizar(ESTUDIANTE, i.getCodigoIntentoSimulacro(),
            new FinalizarIntentoRequest(List.of(
                new RespuestaRequest(i.getPreguntas().get(0).preguntaCodigo(), List.of("A", "Z")),
                new RespuestaRequest(i.getPreguntas().get(1).preguntaCodigo(), List.of("B", "A")),
                new RespuestaRequest(UUID.randomUUID(), List.of("A")))));

        assertThat(resultado.puntaje()).isEqualTo(100.0);
        assertThat(resultado.aprobado()).isTrue();
        assertThat(resultado.preguntas().get(0).elegidas()).containsExactly("A");
    }

    @Test
    void finalizar_sinResponderNadaPuntuaCero() {
        IntentoSimulacro i = intento(EstadoIntento.EN_CURSO, AHORA.minusSeconds(300));
        conIntento(i);
        when(intentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IntentoResultadoResponse resultado = service.finalizar(ESTUDIANTE, i.getCodigoIntentoSimulacro(),
            new FinalizarIntentoRequest(List.of()));

        assertThat(resultado.puntaje()).isZero();
        assertThat(resultado.aciertos()).isZero();
    }

    @Test
    void finalizar_despuesDeLaGracia_expiraConCeroYSinNota() {
        IntentoSimulacro i = intento(EstadoIntento.EN_CURSO, AHORA.minusSeconds(20 * 60 + 31));
        conIntento(i);
        when(intentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IntentoResultadoResponse resultado = service.finalizar(ESTUDIANTE, i.getCodigoIntentoSimulacro(),
            new FinalizarIntentoRequest(List.of(new RespuestaRequest(i.getPreguntas().get(0).preguntaCodigo(), List.of("A")))));

        assertThat(resultado.estado()).isEqualTo(EstadoIntento.EXPIRADO);
        assertThat(resultado.puntaje()).isZero();
        verify(resultadoRepository, never()).save(any());
    }

    @Test
    void finalizar_dentroDeLaGracia_siCuenta() {
        IntentoSimulacro i = intento(EstadoIntento.EN_CURSO, AHORA.minusSeconds(20 * 60 + 10));
        conIntento(i);
        when(intentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.finalizar(ESTUDIANTE, i.getCodigoIntentoSimulacro(), new FinalizarIntentoRequest(List.of()))
            .estado()).isEqualTo(EstadoIntento.FINALIZADO);
    }

    @Test
    void finalizar_unoYaTerminadoOAjeno_lanzaErrAca31OErrAca30() {
        IntentoSimulacro terminado = intento(EstadoIntento.FINALIZADO, AHORA.minusSeconds(900));
        conIntento(terminado);
        esperaCodigo(() -> service.finalizar(ESTUDIANTE, terminado.getCodigoIntentoSimulacro(),
            new FinalizarIntentoRequest(List.of())), AcademicoErrorCodes.INTENTO_YA_FINALIZADO);

        when(intentoRepository.findByEstudianteCodigoAndCodigoIntentoSimulacroAndDeletedAtIsNull(any(), any()))
            .thenReturn(Optional.empty());
        esperaCodigo(() -> service.finalizar(ESTUDIANTE, UUID.randomUUID(), new FinalizarIntentoRequest(List.of())),
            AcademicoErrorCodes.INTENTO_NO_ENCONTRADO);
        verify(resultadoRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ historial y corrección

    @Test
    void obtener_deUnoEnCursoLanzaErrAca29_deUnoTerminadoDevuelveLaCorreccion() {
        IntentoSimulacro enCurso = intento(EstadoIntento.EN_CURSO, AHORA.minusSeconds(60));
        conIntento(enCurso);
        esperaCodigo(() -> service.obtener(ESTUDIANTE, enCurso.getCodigoIntentoSimulacro()),
            AcademicoErrorCodes.INTENTO_EN_CURSO);

        IntentoSimulacro terminado = intento(EstadoIntento.FINALIZADO, AHORA.minusSeconds(3600));
        conIntento(terminado);
        assertThat(service.obtener(ESTUDIANTE, terminado.getCodigoIntentoSimulacro()).preguntas()).hasSize(2);
    }

    @Test
    void listarMios_devuelveElHistorialPaginadoSinLosEnCurso() {
        IntentoSimulacro i = intento(EstadoIntento.FINALIZADO, AHORA.minusSeconds(3600));
        when(intentoRepository.findByEstudianteCodigoAndEstadoNotAndDeletedAtIsNullOrderByIniciadoEnDesc(
            any(), any(), any())).thenReturn(new PageImpl<>(List.of(i)));

        var pagina = service.listarMios(ESTUDIANTE, null, 5000);

        assertThat(pagina.getContent()).singleElement().satisfies(r -> assertThat(r.total()).isEqualTo(2));
        verify(intentoRepository).findByEstudianteCodigoAndEstadoNotAndDeletedAtIsNullOrderByIniciadoEnDesc(
            ESTUDIANTE, EstadoIntento.EN_CURSO, org.springframework.data.domain.PageRequest.of(0, 100));
    }
}
