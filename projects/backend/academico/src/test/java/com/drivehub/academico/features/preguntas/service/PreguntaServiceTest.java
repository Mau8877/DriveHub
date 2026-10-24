package com.drivehub.academico.features.preguntas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.preguntas.dto.CrearPreguntaRequest;
import com.drivehub.academico.features.preguntas.dto.EditarPreguntaRequest;
import com.drivehub.academico.features.preguntas.dto.OpcionRequest;
import com.drivehub.academico.features.preguntas.dto.PreguntaResponse;
import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.preguntas.model.OpcionPregunta;
import com.drivehub.academico.features.preguntas.model.Pregunta;
import com.drivehub.academico.features.preguntas.repository.FiltroPreguntas;
import com.drivehub.academico.features.preguntas.repository.PreguntaRepository;

@ExtendWith(MockitoExtension.class)
class PreguntaServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();

    @Mock
    private PreguntaRepository preguntaRepository;

    @InjectMocks
    private PreguntaService service;

    private static OpcionRequest op(String texto, boolean correcta) {
        return new OpcionRequest(texto, correcta);
    }

    private static CrearPreguntaRequest crear(TipoPregunta tipo, List<OpcionRequest> opciones) {
        return new CrearPreguntaRequest("GENERAL", "  Señales ", tipo, " ¿Qué indica el PARE? ", opciones, "  ", 2);
    }

    private static Pregunta existente(TipoPregunta tipo, boolean activa) {
        Pregunta p = new Pregunta();
        p.setEscuelaCodigo(ESCUELA);
        p.setCategoria("GENERAL");
        p.setTema("Señales");
        p.setTipo(tipo);
        p.setEnunciado("¿Qué indica el PARE?");
        p.setOpciones(List.of(new OpcionPregunta("A", "Detenerse", true), new OpcionPregunta("B", "Seguir", false)));
        p.setDificultad(1);
        p.setActiva(activa);
        p.inicializarCodigo();
        return p;
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    private void conPreguntaGuardada(Pregunta pregunta) {
        when(preguntaRepository.findByEscuelaCodigoAndCodigoPreguntaAndDeletedAtIsNull(ESCUELA, CODIGO))
            .thenReturn(Optional.of(pregunta));
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_conDatosValidos_guardaActivaConEscuelaAutorLimpiezaEIdsPorOrden() {
        when(preguntaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PreguntaResponse respuesta = service.crear(ESCUELA, AUTOR,
            crear(TipoPregunta.OPCION_UNICA, List.of(op(" Detenerse ", true), op("Seguir", false), op("Tocar bocina", false))));

        ArgumentCaptor<Pregunta> guardada = ArgumentCaptor.forClass(Pregunta.class);
        verify(preguntaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(guardada.getValue().getCreadaPorCodigo()).isEqualTo(AUTOR);
        assertThat(guardada.getValue().isActiva()).isTrue();
        assertThat(guardada.getValue().getExplicacion()).isNull();
        assertThat(respuesta.tema()).isEqualTo("Señales");
        assertThat(respuesta.enunciado()).isEqualTo("¿Qué indica el PARE?");
        assertThat(respuesta.opciones()).extracting(PreguntaResponse.OpcionResponse::id).containsExactly("A", "B", "C");
        assertThat(respuesta.opciones().get(0).texto()).isEqualTo("Detenerse");
    }

    @Test
    void crear_conOpcionMultiple_aceptaVariasCorrectasPeroNoNinguna() {
        when(preguntaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.crear(ESCUELA, AUTOR,
            crear(TipoPregunta.OPCION_MULTIPLE, List.of(op("A1", true), op("B1", true), op("C1", false)))).opciones())
            .hasSize(3);
        esperaCodigo(() -> service.crear(ESCUELA, AUTOR,
            crear(TipoPregunta.OPCION_MULTIPLE, List.of(op("A1", false), op("B1", false)))), "ERR_SYS_01");
    }

    @Test
    void crear_conOpcionesQueNoCumplenElTipo_lanzaErrSys01SinGuardar() {
        List<List<OpcionRequest>> malas = List.of(
            List.of(op("Solo una", true)),
            List.of(op("A1", true), op("B1", true)),
            List.of(op("A1", false), op("B1", false)),
            List.of(op("Igual", true), op("igual", false)),
            List.of(op("A1", true), op(" ", false)),
            List.of(op("1", true), op("2", false), op("3", false), op("4", false), op("5", false), op("6", false), op("7", false)));
        for (List<OpcionRequest> opciones : malas) {
            esperaCodigo(() -> service.crear(ESCUELA, AUTOR, crear(TipoPregunta.OPCION_UNICA, opciones)), "ERR_SYS_01");
        }
        esperaCodigo(() -> service.crear(ESCUELA, AUTOR,
            crear(TipoPregunta.VERDADERO_FALSO, List.of(op("V", true), op("F", false), op("Quizá", false)))), "ERR_SYS_01");
        verify(preguntaRepository, never()).save(any());
    }

    @Test
    void crear_conCamposInvalidos_lanzaErrSys01SinGuardar() {
        List<OpcionRequest> bien = List.of(op("A1", true), op("B1", false));
        CrearPreguntaRequest[] malas = {
            new CrearPreguntaRequest("general", "Tema", TipoPregunta.OPCION_UNICA, "Enunciado válido", bien, null, 1),
            new CrearPreguntaRequest("GENERAL", "T", TipoPregunta.OPCION_UNICA, "Enunciado válido", bien, null, 1),
            new CrearPreguntaRequest("GENERAL", "Tema", TipoPregunta.OPCION_UNICA, "Cort", bien, null, 1),
            new CrearPreguntaRequest("GENERAL", "Tema", TipoPregunta.OPCION_UNICA, "e".repeat(501), bien, null, 1),
            new CrearPreguntaRequest("GENERAL", "Tema", TipoPregunta.OPCION_UNICA, "Enunciado válido", bien, "x".repeat(501), 1),
            new CrearPreguntaRequest("GENERAL", "Tema", TipoPregunta.OPCION_UNICA, "Enunciado válido", bien, null, 4),
            new CrearPreguntaRequest("GENERAL", "Tema", TipoPregunta.OPCION_UNICA, "Enunciado válido", bien, null, null),
            new CrearPreguntaRequest("GENERAL", "Tema", null, "Enunciado válido", bien, null, 1),
        };
        for (CrearPreguntaRequest mala : malas) {
            esperaCodigo(() -> service.crear(ESCUELA, AUTOR, mala), "ERR_SYS_01");
        }
        verify(preguntaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ listar y obtener

    @Test
    void listar_sinParametros_usaPagina1De20_conTamanoGrande_loLimitaA100() {
        when(preguntaRepository.listar(any(), any(), any())).thenReturn(new PageImpl<>(List.of(existente(TipoPregunta.OPCION_UNICA, true))));
        FiltroPreguntas filtro = new FiltroPreguntas(null, null, null, null, null, null);

        service.listar(ESCUELA, filtro, null, null);
        service.listar(ESCUELA, filtro, 3, 5000);

        verify(preguntaRepository).listar(ESCUELA, filtro, PageRequest.of(0, 20));
        verify(preguntaRepository).listar(ESCUELA, filtro, PageRequest.of(2, 100));
    }

    @Test
    void obtener_conPreguntaInexistenteODeOtraEscuela_lanzaErrAca01() {
        when(preguntaRepository.findByEscuelaCodigoAndCodigoPreguntaAndDeletedAtIsNull(ESCUELA, CODIGO))
            .thenReturn(Optional.empty());

        esperaCodigo(() -> service.obtener(ESCUELA, CODIGO), AcademicoErrorCodes.PREGUNTA_NO_ENCONTRADA);
    }

    // ------------------------------------------------------------------ editar

    @Test
    void editar_soloCambiaLoEnviadoYDejaLoDemas() {
        conPreguntaGuardada(existente(TipoPregunta.OPCION_UNICA, true));
        when(preguntaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PreguntaResponse respuesta = service.editar(ESCUELA, CODIGO,
            new EditarPreguntaRequest(null, " Prioridad ", null, null, null, "Porque sí", 3));

        assertThat(respuesta.tema()).isEqualTo("Prioridad");
        assertThat(respuesta.explicacion()).isEqualTo("Porque sí");
        assertThat(respuesta.dificultad()).isEqualTo(3);
        assertThat(respuesta.enunciado()).isEqualTo("¿Qué indica el PARE?");
        assertThat(respuesta.opciones()).hasSize(2);
        assertThat(respuesta.tipo()).isEqualTo(TipoPregunta.OPCION_UNICA);
    }

    @Test
    void editar_conOpcionesNuevas_reemplazaTodasYReasignaIds() {
        conPreguntaGuardada(existente(TipoPregunta.OPCION_UNICA, true));
        when(preguntaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PreguntaResponse respuesta = service.editar(ESCUELA, CODIGO, new EditarPreguntaRequest(null, null, null, null,
            List.of(op("Uno", false), op("Dos", true), op("Tres", false)), null, null));

        assertThat(respuesta.opciones()).extracting(PreguntaResponse.OpcionResponse::texto)
            .containsExactly("Uno", "Dos", "Tres");
        assertThat(respuesta.opciones()).extracting(PreguntaResponse.OpcionResponse::id).containsExactly("A", "B", "C");
    }

    @Test
    void editar_cambiandoSoloElTipo_revalidaLasOpcionesGuardadas() {
        conPreguntaGuardada(existente(TipoPregunta.OPCION_UNICA, true));
        when(preguntaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.editar(ESCUELA, CODIGO, new EditarPreguntaRequest(null, null, TipoPregunta.VERDADERO_FALSO,
            null, null, null, null)).tipo()).isEqualTo(TipoPregunta.VERDADERO_FALSO);

        Pregunta tres = existente(TipoPregunta.OPCION_UNICA, true);
        tres.setOpciones(List.of(new OpcionPregunta("A", "Uno", true), new OpcionPregunta("B", "Dos", false),
            new OpcionPregunta("C", "Tres", false)));
        conPreguntaGuardada(tres);
        esperaCodigo(() -> service.editar(ESCUELA, CODIGO, new EditarPreguntaRequest(null, null,
            TipoPregunta.VERDADERO_FALSO, null, null, null, null)), "ERR_SYS_01");
    }

    @Test
    void editar_conPreguntaInexistente_lanzaErrAca01() {
        when(preguntaRepository.findByEscuelaCodigoAndCodigoPreguntaAndDeletedAtIsNull(ESCUELA, CODIGO))
            .thenReturn(Optional.empty());

        esperaCodigo(() -> service.editar(ESCUELA, CODIGO,
            new EditarPreguntaRequest(null, "Tema", null, null, null, null, null)), AcademicoErrorCodes.PREGUNTA_NO_ENCONTRADA);
        verify(preguntaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ activar y desactivar

    @Test
    void cambiarActiva_cambiaElEstadoYSiYaEstabaAsiLanzaErrAca02() {
        Pregunta pregunta = existente(TipoPregunta.OPCION_UNICA, true);
        conPreguntaGuardada(pregunta);
        when(preguntaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.cambiarActiva(ESCUELA, CODIGO, false).activa()).isFalse();

        esperaCodigo(() -> service.cambiarActiva(ESCUELA, CODIGO, false), AcademicoErrorCodes.PREGUNTA_YA_EN_ESE_ESTADO);
        assertThat(service.cambiarActiva(ESCUELA, CODIGO, true).activa()).isTrue();
        esperaCodigo(() -> service.cambiarActiva(ESCUELA, CODIGO, true), AcademicoErrorCodes.PREGUNTA_YA_EN_ESE_ESTADO);
    }
}
