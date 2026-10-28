package com.drivehub.academico.features.examenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.examenes.dto.ExamenResponse;
import com.drivehub.academico.features.examenes.dto.ProgramarExamenRequest;
import com.drivehub.academico.features.examenes.dto.RegistrarResultadoExamenRequest;
import com.drivehub.academico.features.examenes.dto.ReprogramarExamenRequest;
import com.drivehub.academico.features.examenes.enums.EstadoExamen;
import com.drivehub.academico.features.examenes.model.ExamenConduccion;
import com.drivehub.academico.features.examenes.repository.ExamenConduccionRepository;
import com.drivehub.academico.features.examenes.repository.FiltroExamenes;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;

@ExtendWith(MockitoExtension.class)
class ExamenConduccionServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();
    private static final String BEARER = "Bearer x";
    // * 2026-11-02 12:00 en La Paz (UTC-4)
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-02T16:00:00Z"), ZoneId.of("America/La_Paz"));

    @Mock
    private ExamenConduccionRepository repository;
    @Mock
    private ResultadoRepository resultadoRepository;
    @Mock
    private IdentidadClient identidad;

    private ExamenConduccionService service;

    @BeforeEach
    void preparar() {
        service = new ExamenConduccionService(repository, resultadoRepository, identidad, RELOJ);
    }

    private static ProgramarExamenRequest programar(LocalDate fecha, String hora) {
        return new ProgramarExamenRequest(ESTUDIANTE, "B", fecha, hora, UUID.randomUUID(), UUID.randomUUID());
    }

    private static ExamenConduccion existente(EstadoExamen estado) {
        ExamenConduccion e = new ExamenConduccion();
        e.setEscuelaCodigo(ESCUELA);
        e.setEstudianteCodigo(ESTUDIANTE);
        e.setCategoria("B");
        e.setFechaProgramada(Instant.parse("2026-11-10T14:00:00Z"));
        e.setEvaluadorCodigo(UUID.randomUUID());
        e.setVehiculoCodigo(UUID.randomUUID());
        e.setIntento(1);
        e.setEstado(estado);
        e.inicializarCodigo();
        return e;
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    private void conExamen(ExamenConduccion examen) {
        when(repository.findByEscuelaCodigoAndCodigoExamenConduccionAndDeletedAtIsNull(ESCUELA, CODIGO))
            .thenReturn(Optional.of(examen));
    }

    // ------------------------------------------------------------------ programar

    @Test
    void programar_conDatosValidos_guardaProgramadoConIntentoCalculadoYHoraDeLaEscuela() {
        when(identidad.nombresDeEstudiantes(List.of(ESTUDIANTE), BEARER)).thenReturn(Map.of(ESTUDIANTE, "Ana Pérez"));
        when(repository.countByEscuelaCodigoAndEstudianteCodigoAndCategoriaAndEstadoNotAndDeletedAtIsNull(
            ESCUELA, ESTUDIANTE, "B", EstadoExamen.CANCELADO)).thenReturn(2L);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ExamenResponse respuesta = service.programar(ESCUELA, AUTOR, BEARER, programar(LocalDate.of(2026, 11, 10), "10:00"));

        ArgumentCaptor<ExamenConduccion> guardado = ArgumentCaptor.forClass(ExamenConduccion.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(guardado.getValue().getCreadoPorCodigo()).isEqualTo(AUTOR);
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoExamen.PROGRAMADO);
        assertThat(guardado.getValue().getFechaProgramada()).isEqualTo(Instant.parse("2026-11-10T14:00:00Z"));
        assertThat(respuesta.intento()).isEqualTo(3);
        assertThat(respuesta.estudianteNombre()).isEqualTo("Ana Pérez");
        assertThat(respuesta.fecha()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(respuesta.hora()).isEqualTo("10:00");
    }

    @Test
    void programar_conFechaPasada_lanzaErrSys01SinGuardar() {
        esperaCodigo(() -> service.programar(ESCUELA, AUTOR, BEARER, programar(LocalDate.of(2026, 11, 2), "11:00")),
            "ERR_SYS_01");
        verify(repository, never()).save(any());
    }

    @Test
    void programar_conEstudianteQueNoEsDeLaEscuela_lanzaErrAca27() {
        when(identidad.nombresDeEstudiantes(any(), eq(BEARER))).thenReturn(Map.of());

        esperaCodigo(() -> service.programar(ESCUELA, AUTOR, BEARER, programar(LocalDate.of(2026, 11, 10), "10:00")),
            AcademicoErrorCodes.ESTUDIANTE_NO_ENCONTRADO);
        verify(repository, never()).save(any());
    }

    @Test
    void programar_conOtroExamenProgramadoEnLaMismaCategoria_lanzaConflictErrAca26() {
        when(identidad.nombresDeEstudiantes(any(), eq(BEARER))).thenReturn(Map.of(ESTUDIANTE, "Ana"));
        when(repository.existsByEscuelaCodigoAndEstudianteCodigoAndCategoriaAndEstadoAndDeletedAtIsNull(
            ESCUELA, ESTUDIANTE, "B", EstadoExamen.PROGRAMADO)).thenReturn(true);

        esperaCodigo(() -> service.programar(ESCUELA, AUTOR, BEARER, programar(LocalDate.of(2026, 11, 10), "10:00")),
            AcademicoErrorCodes.EXAMEN_YA_PROGRAMADO);
        verify(repository, never()).save(any());
    }

    // ------------------------------------------------------------------ listar y obtener

    @Test
    void listar_unaConsultaDeNombresPorPaginaYTamanoLimitado() {
        ExamenConduccion examen = existente(EstadoExamen.PROGRAMADO);
        FiltroExamenes filtro = new FiltroExamenes(null, null, null);
        when(repository.listar(any(), any(), any())).thenReturn(new PageImpl<>(List.of(examen)));
        when(identidad.nombresDeEstudiantes(any(), eq(BEARER))).thenReturn(Map.of(ESTUDIANTE, "Ana Pérez"));

        var pagina = service.listar(ESCUELA, BEARER, filtro, null, null);
        service.listar(ESCUELA, BEARER, filtro, 3, 5000);

        assertThat(pagina.getContent().get(0).estudianteNombre()).isEqualTo("Ana Pérez");
        verify(repository).listar(ESCUELA, filtro, PageRequest.of(0, 20));
        verify(repository).listar(ESCUELA, filtro, PageRequest.of(2, 100));
    }

    @Test
    void listar_sinExamenes_noConsultaAIdentidad() {
        when(repository.listar(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.listar(ESCUELA, BEARER, new FiltroExamenes(null, null, null), null, null);

        verify(identidad, never()).nombresDeEstudiantes(any(), any());
    }

    @Test
    void obtener_conExamenInexistenteODeOtraEscuela_lanzaErrAca24() {
        when(repository.findByEscuelaCodigoAndCodigoExamenConduccionAndDeletedAtIsNull(ESCUELA, CODIGO))
            .thenReturn(Optional.empty());

        esperaCodigo(() -> service.obtener(ESCUELA, BEARER, CODIGO), AcademicoErrorCodes.EXAMEN_NO_ENCONTRADO);
    }

    // ------------------------------------------------------------------ reprogramar, cancelar, no asistio

    @Test
    void reprogramar_soloCambiaLoEnviadoYConservaLaOtraParteDeLaFecha() {
        ExamenConduccion examen = existente(EstadoExamen.PROGRAMADO);
        conExamen(examen);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        UUID vehiculo = UUID.randomUUID();

        ExamenResponse respuesta = service.reprogramar(ESCUELA, CODIGO,
            new ReprogramarExamenRequest(null, "15:30", null, vehiculo));

        assertThat(respuesta.fecha()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(respuesta.hora()).isEqualTo("15:30");
        assertThat(respuesta.vehiculoCodigo()).isEqualTo(vehiculo);
    }

    @Test
    void reprogramar_aUnaFechaPasada_lanzaErrSys01() {
        conExamen(existente(EstadoExamen.PROGRAMADO));

        esperaCodigo(() -> service.reprogramar(ESCUELA, CODIGO,
            new ReprogramarExamenRequest(LocalDate.of(2026, 10, 1), null, null, null)), "ERR_SYS_01");
        verify(repository, never()).save(any());
    }

    @Test
    void cancelarYMarcarNoAsistio_soloSobreUnoProgramado() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        conExamen(existente(EstadoExamen.PROGRAMADO));
        assertThat(service.cancelar(ESCUELA, CODIGO).estado()).isEqualTo(EstadoExamen.CANCELADO);
        conExamen(existente(EstadoExamen.PROGRAMADO));
        assertThat(service.marcarNoAsistio(ESCUELA, CODIGO).estado()).isEqualTo(EstadoExamen.NO_ASISTIO);

        conExamen(existente(EstadoExamen.REALIZADO));
        esperaCodigo(() -> service.cancelar(ESCUELA, CODIGO), AcademicoErrorCodes.EXAMEN_NO_ESTA_PROGRAMADO);
        conExamen(existente(EstadoExamen.CANCELADO));
        esperaCodigo(() -> service.marcarNoAsistio(ESCUELA, CODIGO), AcademicoErrorCodes.EXAMEN_NO_ESTA_PROGRAMADO);
        esperaCodigo(() -> service.reprogramar(ESCUELA, CODIGO, new ReprogramarExamenRequest(null, null, null, null)),
            AcademicoErrorCodes.EXAMEN_NO_ESTA_PROGRAMADO);
    }

    // ------------------------------------------------------------------ registrar resultado

    @Test
    void registrarResultado_conPuntajeDesde70_apruebaYEscribeLaNotaConsolidada() {
        conExamen(existente(EstadoExamen.PROGRAMADO));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ExamenResponse respuesta = service.registrarResultado(ESCUELA, CODIGO,
            new RegistrarResultadoExamenRequest(70.0, "  Buen manejo  "));

        assertThat(respuesta.estado()).isEqualTo(EstadoExamen.REALIZADO);
        assertThat(respuesta.resultado()).isEqualTo(ResultadoEvaluacion.APROBADO);
        assertThat(respuesta.observaciones()).isEqualTo("Buen manejo");
        ArgumentCaptor<Resultado> nota = ArgumentCaptor.forClass(Resultado.class);
        verify(resultadoRepository).save(nota.capture());
        assertThat(nota.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(nota.getValue().getEstudianteCodigo()).isEqualTo(ESTUDIANTE);
        assertThat(nota.getValue().getOrigen()).isEqualTo(OrigenResultado.EXAMEN_CONDUCCION);
        assertThat(nota.getValue().isAprobado()).isTrue();
        assertThat(nota.getValue().getPuntaje()).isEqualTo(70.0);
        assertThat(nota.getValue().getTitulo()).contains("categoría B").contains("intento 1");
    }

    @Test
    void registrarResultado_conPuntajeBajo70_reprueba() {
        conExamen(existente(EstadoExamen.PROGRAMADO));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ExamenResponse respuesta = service.registrarResultado(ESCUELA, CODIGO,
            new RegistrarResultadoExamenRequest(69.5, null));

        assertThat(respuesta.resultado()).isEqualTo(ResultadoEvaluacion.REPROBADO);
        assertThat(respuesta.observaciones()).isNull();
    }

    @Test
    void registrarResultado_enUnoQueNoEstaProgramado_lanzaErrAca25SinEscribirNada() {
        conExamen(existente(EstadoExamen.REALIZADO));

        esperaCodigo(() -> service.registrarResultado(ESCUELA, CODIGO, new RegistrarResultadoExamenRequest(80.0, null)),
            AcademicoErrorCodes.EXAMEN_NO_ESTA_PROGRAMADO);
        verify(repository, never()).save(any());
        verify(resultadoRepository, never()).save(any());
    }
}
