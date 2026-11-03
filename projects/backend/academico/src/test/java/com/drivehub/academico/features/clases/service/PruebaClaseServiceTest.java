package com.drivehub.academico.features.clases.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.clases.dto.EvaluarClaseRequest;
import com.drivehub.academico.features.clases.dto.PruebaClaseResponse;
import com.drivehub.academico.features.clases.dto.PuntajeDeCriterio;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.model.Clase;
import com.drivehub.academico.features.clases.model.PruebaClase;
import com.drivehub.academico.features.clases.repository.ClaseRepository;
import com.drivehub.academico.features.clases.repository.PruebaClaseRepository;
import com.drivehub.academico.features.reservas.service.CierreDeReservaService;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.model.Resultado;
import com.drivehub.academico.features.resultados.repository.ResultadoRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * La autorización por rol NO se ejecuta acá (un servicio sin Spring no corre @PreAuthorize): se comprueba por
 * reflexión que esté declarada.
 */
@ExtendWith(MockitoExtension.class)
class PruebaClaseServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 26);
    private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private PruebaClaseRepository pruebaRepository;
    @Mock
    private ResultadoRepository resultadoRepository;
    @Mock
    private CierreDeReservaService cierre;

    private final UUID instructor = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private PruebaClaseService service;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        service = new PruebaClaseService(claseRepository, pruebaRepository, resultadoRepository, cierre,
            Clock.fixed(AHORA, ZoneOffset.UTC));
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(PruebaClaseService.class)).addAppender(logs);
    }

    @AfterEach
    void cerrarLogs() {
        ((Logger) LoggerFactory.getLogger(PruebaClaseService.class)).detachAppender(logs);
    }

    private Clase clase(Asistencia asistencia) {
        Clase clase = new Clase(50L, estudiante, instructor, FECHA, asistencia);
        clase.setId(90L);
        clase.inicializarCodigo();
        return clase;
    }

    private void laClaseExiste(Clase clase) {
        when(claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(clase.getCodigoClase(),
            instructor)).thenReturn(Optional.of(clase));
    }

    private static List<PuntajeDeCriterio> todosEn(TipoPrueba tipo, double puntaje) {
        return new ArrayList<>(CatalogoDePruebas.de(tipo).criterios().stream()
            .map(c -> new PuntajeDeCriterio(c.codigo(), puntaje)).toList());
    }

    private EvaluarClaseRequest pedido(Clase clase, TipoPrueba tipo, double puntaje, String observaciones) {
        return new EvaluarClaseRequest(clase.getCodigoClase(), tipo, todosEn(tipo, puntaje), observaciones);
    }

    private void elGuardadoAsignaId() {
        when(pruebaRepository.save(any(PruebaClase.class))).thenAnswer(i -> {
            PruebaClase p = i.getArgument(0);
            p.setId(300L);
            p.inicializarCodigo();
            return p;
        });
    }

    private static void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ================================================================ evaluar

    @Test
    void evaluar_conTodoBien_guardaLaPruebaYSuNotaAprobada() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(90L, TipoPrueba.MANEJO))
            .thenReturn(Optional.empty());
        elGuardadoAsignaId();

        PruebaClaseResponse respuesta = service.evaluar(instructor, escuela,
            pedido(clase, TipoPrueba.MANEJO, 8, "  Muy bien  "));

        assertThat(respuesta.claseCodigo()).isEqualTo(clase.getCodigoClase());
        assertThat(respuesta.tipo()).isEqualTo(TipoPrueba.MANEJO);
        assertThat(respuesta.puntajeTotal()).isEqualTo(80.0);
        assertThat(respuesta.resultado()).isEqualTo(ResultadoEvaluacion.APROBADO);
        assertThat(respuesta.observaciones()).isEqualTo("Muy bien");
        assertThat(respuesta.fecha()).isEqualTo(FECHA);
        assertThat(respuesta.criterios()).hasSize(5);

        ArgumentCaptor<PruebaClase> prueba = ArgumentCaptor.forClass(PruebaClase.class);
        verify(pruebaRepository).save(prueba.capture());
        assertThat(prueba.getValue().getClaseId()).isEqualTo(90L);
        assertThat(prueba.getValue().getEstudianteCodigo()).isEqualTo(estudiante);
        assertThat(prueba.getValue().getInstructorCodigo()).isEqualTo(instructor);

        ArgumentCaptor<Resultado> nota = ArgumentCaptor.forClass(Resultado.class);
        verify(resultadoRepository).save(nota.capture());
        assertThat(nota.getValue().getEscuelaCodigo()).isEqualTo(escuela);
        assertThat(nota.getValue().getEstudianteCodigo()).isEqualTo(estudiante);
        assertThat(nota.getValue().getOrigen()).isEqualTo(OrigenResultado.PRUEBA_CLASE);
        assertThat(nota.getValue().getOrigenId()).isEqualTo(300L);
        assertThat(nota.getValue().getTitulo()).isEqualTo("Prueba de clase: Manejo");
        assertThat(nota.getValue().getPuntaje()).isEqualTo(80.0);
        assertThat(nota.getValue().isAprobado()).isTrue();
        assertThat(nota.getValue().getFecha()).isEqualTo(AHORA);
    }

    @Test
    void evaluar_conTotalBajoSetenta_quedaReprobadaYLaNotaNoAprobada() {
        Clase clase = clase(Asistencia.TARDE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(90L, TipoPrueba.CIUDAD))
            .thenReturn(Optional.empty());
        elGuardadoAsignaId();

        PruebaClaseResponse respuesta = service.evaluar(instructor, escuela,
            pedido(clase, TipoPrueba.CIUDAD, 6, null));

        assertThat(respuesta.resultado()).isEqualTo(ResultadoEvaluacion.REPROBADO);
        assertThat(respuesta.observaciones()).isNull();
        ArgumentCaptor<Resultado> nota = ArgumentCaptor.forClass(Resultado.class);
        verify(resultadoRepository).save(nota.capture());
        assertThat(nota.getValue().isAprobado()).isFalse();
        assertThat(nota.getValue().getTitulo()).isEqualTo("Prueba de clase: Ciudad");
    }

    @Test
    void evaluar_conObservacionesEnBlanco_laGuardaNula() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(anyLong(), any())).thenReturn(Optional.empty());
        elGuardadoAsignaId();

        service.evaluar(instructor, escuela, pedido(clase, TipoPrueba.MANEJO, 9, "   "));

        ArgumentCaptor<PruebaClase> prueba = ArgumentCaptor.forClass(PruebaClase.class);
        verify(pruebaRepository).save(prueba.capture());
        assertThat(prueba.getValue().getObservaciones()).isNull();
    }

    @Test
    void evaluar_claseInexistenteOAjena_esErr35SinGuardarNada() {
        UUID otra = UUID.randomUUID();
        when(claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(otra, instructor))
            .thenReturn(Optional.empty());

        assertError(() -> service.evaluar(instructor, escuela, new EvaluarClaseRequest(otra, TipoPrueba.MANEJO,
            todosEn(TipoPrueba.MANEJO, 8), null)), "ERR_ACA_35");

        verifyNoInteractions(pruebaRepository, resultadoRepository, cierre);
    }

    @Test
    void evaluar_claseAusente_esErr39() {
        Clase clase = clase(Asistencia.AUSENTE);
        laClaseExiste(clase);

        assertError(() -> service.evaluar(instructor, escuela, pedido(clase, TipoPrueba.MANEJO, 8, null)),
            "ERR_ACA_39");

        verifyNoInteractions(pruebaRepository, resultadoRepository);
    }

    @Test
    void evaluar_conElCierreSinTerminar_esErr39() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(false);

        assertError(() -> service.evaluar(instructor, escuela, pedido(clase, TipoPrueba.MANEJO, 8, null)),
            "ERR_ACA_39");

        verifyNoInteractions(pruebaRepository, resultadoRepository);
    }

    @Test
    void evaluar_conCriteriosInvalidos_esErr41SinBuscarLaPruebaNiGuardarNada() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        List<PuntajeDeCriterio> incompletos = todosEn(TipoPrueba.MANEJO, 8);
        incompletos.remove(0);

        assertError(() -> service.evaluar(instructor, escuela,
            new EvaluarClaseRequest(clase.getCodigoClase(), TipoPrueba.MANEJO, incompletos, null)), "ERR_ACA_41");

        verifyNoInteractions(pruebaRepository, resultadoRepository);
    }

    @Test
    void evaluar_conLaPruebaYSuNotaYaGuardadas_esErr40SinGuardarNada() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        PruebaClase existente = pruebaExistente(clase);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(90L, TipoPrueba.MANEJO))
            .thenReturn(Optional.of(existente));
        when(resultadoRepository.existsByOrigenAndOrigenIdAndDeletedAtIsNull(OrigenResultado.PRUEBA_CLASE, 300L))
            .thenReturn(true);

        assertError(() -> service.evaluar(instructor, escuela, pedido(clase, TipoPrueba.MANEJO, 8, null)),
            "ERR_ACA_40");

        verify(pruebaRepository, never()).save(any());
        verify(resultadoRepository, never()).save(any());
    }

    @Test
    void evaluar_conLaPruebaSinNota_escribeSoloLaNotaYDevuelveLaPruebaGuardada() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        PruebaClase existente = pruebaExistente(clase);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(90L, TipoPrueba.MANEJO))
            .thenReturn(Optional.of(existente));
        when(resultadoRepository.existsByOrigenAndOrigenIdAndDeletedAtIsNull(OrigenResultado.PRUEBA_CLASE, 300L))
            .thenReturn(false);

        // * El reintento trae otros puntajes (3): la prueba guardada (8) no cambia
        PruebaClaseResponse respuesta = service.evaluar(instructor, escuela,
            pedido(clase, TipoPrueba.MANEJO, 3, null));

        assertThat(respuesta.codigo()).isEqualTo(existente.getCodigoPruebaClase());
        assertThat(respuesta.puntajeTotal()).isEqualTo(80.0);
        verify(pruebaRepository, never()).save(any());
        ArgumentCaptor<Resultado> nota = ArgumentCaptor.forClass(Resultado.class);
        verify(resultadoRepository).save(nota.capture());
        assertThat(nota.getValue().getOrigenId()).isEqualTo(300L);
        assertThat(nota.getValue().getPuntaje()).isEqualTo(80.0);
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
            .contains("Nota de prueba de clase completada");
    }

    @Test
    void evaluar_conOtraPeticionQueCreoLaPruebaALaVez_esErr40() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(90L, TipoPrueba.MANEJO))
            .thenReturn(Optional.empty());
        when(pruebaRepository.save(any(PruebaClase.class))).thenThrow(new DuplicateKeyException("duplicado"));

        assertError(() -> service.evaluar(instructor, escuela, pedido(clase, TipoPrueba.MANEJO, 8, null)),
            "ERR_ACA_40");

        verify(resultadoRepository, never()).save(any());
    }

    @Test
    void evaluar_conLaNotaEscritaPorOtraPeticionALaVez_devuelveLaPruebaSinFallar() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        PruebaClase existente = pruebaExistente(clase);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(90L, TipoPrueba.MANEJO))
            .thenReturn(Optional.of(existente));
        when(resultadoRepository.existsByOrigenAndOrigenIdAndDeletedAtIsNull(OrigenResultado.PRUEBA_CLASE, 300L))
            .thenReturn(false);
        when(resultadoRepository.save(any(Resultado.class))).thenThrow(new DuplicateKeyException("duplicado"));

        PruebaClaseResponse respuesta = service.evaluar(instructor, escuela, pedido(clase, TipoPrueba.MANEJO, 8, null));

        assertThat(respuesta.codigo()).isEqualTo(existente.getCodigoPruebaClase());
    }

    @Test
    void evaluar_otroTipoEnLaMismaClase_seGuarda() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(90L, TipoPrueba.ESTACIONAMIENTO))
            .thenReturn(Optional.empty());
        elGuardadoAsignaId();

        PruebaClaseResponse respuesta = service.evaluar(instructor, escuela,
            pedido(clase, TipoPrueba.ESTACIONAMIENTO, 10, null));

        assertThat(respuesta.puntajeTotal()).isEqualTo(100.0);
    }

    @Test
    void evaluar_sinEscuelaEnElToken_esErr16() {
        Clase clase = clase(Asistencia.PRESENTE);

        assertError(() -> service.evaluar(instructor, null, pedido(clase, TipoPrueba.MANEJO, 8, null)),
            "ERR_ACA_16");

        verifyNoInteractions(claseRepository, pruebaRepository, resultadoRepository);
    }

    @Test
    void evaluar_nuncaLogueaLasObservaciones() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(cierre.reservaCompletada(50L)).thenReturn(true);
        when(pruebaRepository.findByClaseIdAndTipoAndDeletedAtIsNull(anyLong(), any())).thenReturn(Optional.empty());
        elGuardadoAsignaId();

        service.evaluar(instructor, escuela, pedido(clase, TipoPrueba.MANEJO, 8, "texto-secreto-del-instructor"));

        assertThat(logs.list).isNotEmpty();
        assertThat(logs.list).allSatisfy(e -> assertThat(e.getFormattedMessage() + e.getArgumentArray()
            + e.getMarkerList()).doesNotContain("texto-secreto-del-instructor"));
        assertThat(logs.list.get(0).getFormattedMessage()).isEqualTo("Prueba de clase registrada");
    }

    private PruebaClase pruebaExistente(Clase clase) {
        PruebaClase existente = new PruebaClase(90L, estudiante, instructor, TipoPrueba.MANEJO,
            ReglasDePrueba.validarYArmarCriterios(TipoPrueba.MANEJO, todosEn(TipoPrueba.MANEJO, 8)), 80.0,
            ResultadoEvaluacion.APROBADO, null, FECHA);
        existente.setId(300L);
        existente.inicializarCodigo();
        return existente;
    }

    // ================================================================ listar y criterios

    @Test
    void listarPorClase_devuelveLasPruebasPaginadas() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        PruebaClase a = pruebaExistente(clase);
        when(pruebaRepository.findByClaseIdAndDeletedAtIsNull(eq(90L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(a), PageRequest.of(0, 20), 1));

        var pagina = service.listarPorClase(instructor, escuela, clase.getCodigoClase(), null, null);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).claseCodigo()).isEqualTo(clase.getCodigoClase());
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(pruebaRepository).findByClaseIdAndDeletedAtIsNull(eq(90L), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void listarPorClase_sinPruebas_devuelveVacioSinError() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(pruebaRepository.findByClaseIdAndDeletedAtIsNull(eq(90L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var pagina = service.listarPorClase(instructor, escuela, clase.getCodigoClase(), 1, 20);

        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
    }

    @Test
    void listarPorClase_acotaLaPaginaYElTamano() {
        Clase clase = clase(Asistencia.PRESENTE);
        laClaseExiste(clase);
        when(pruebaRepository.findByClaseIdAndDeletedAtIsNull(eq(90L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        service.listarPorClase(instructor, escuela, clase.getCodigoClase(), 0, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(pruebaRepository).findByClaseIdAndDeletedAtIsNull(eq(90L), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(PruebaClaseService.TAMANO_MAXIMO);
    }

    @Test
    void listarPorClase_claseAjena_esErr35YSinEscuelaErr16() {
        UUID otra = UUID.randomUUID();
        when(claseRepository.findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(otra, instructor))
            .thenReturn(Optional.empty());

        assertError(() -> service.listarPorClase(instructor, escuela, otra, 1, 20), "ERR_ACA_35");
        assertError(() -> service.listarPorClase(instructor, null, otra, 1, 20), "ERR_ACA_16");
        verifyNoInteractions(pruebaRepository);
    }

    @Test
    void criterios_devuelveLosTresTiposConCincoCriteriosCadaUno() {
        var catalogo = service.criterios();

        assertThat(catalogo.tipos()).hasSize(3);
        assertThat(catalogo.tipos()).allSatisfy(t -> assertThat(t.criterios()).hasSize(5));
    }

    // ================================================================ agenda

    @Test
    void tiposEvaluadosPorReserva_unaReservaSinClaseNoAparece_yUnaSinPruebasTieneConjuntoVacio() {
        Clase conPruebas = clase(Asistencia.PRESENTE);
        Clase sinPruebas = new Clase(51L, estudiante, instructor, FECHA, Asistencia.TARDE);
        sinPruebas.setId(91L);
        when(claseRepository.findByReservaIdInAndDeletedAtIsNull(List.of(50L, 51L, 52L)))
            .thenReturn(List.of(conPruebas, sinPruebas));
        PruebaClase prueba = pruebaExistente(conPruebas);
        when(pruebaRepository.findByClaseIdInAndDeletedAtIsNull(any())).thenReturn(List.of(prueba));

        Map<Long, Set<TipoPrueba>> tipos = service.tiposEvaluadosPorReserva(List.of(50L, 51L, 52L));

        assertThat(tipos).containsOnlyKeys(50L, 51L);
        assertThat(tipos.get(50L)).containsExactly(TipoPrueba.MANEJO);
        assertThat(tipos.get(51L)).isEmpty();
    }

    @Test
    void tiposEvaluadosPorReserva_sinReservasNoConsultaNada() {
        assertThat(service.tiposEvaluadosPorReserva(List.of())).isEmpty();
        verifyNoInteractions(claseRepository, pruebaRepository);
    }

    // ================================================================ seguridad

    @Test
    void losMetodosPublicosExigenElRolInstructor() throws NoSuchMethodException {
        Method evaluar = PruebaClaseService.class.getMethod("evaluar", UUID.class, UUID.class,
            EvaluarClaseRequest.class);
        Method listar = PruebaClaseService.class.getMethod("listarPorClase", UUID.class, UUID.class, UUID.class,
            Integer.class, Integer.class);
        Method criterios = PruebaClaseService.class.getMethod("criterios");

        for (Method metodo : List.of(evaluar, listar, criterios)) {
            assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('INSTRUCTOR')");
        }
    }
}
