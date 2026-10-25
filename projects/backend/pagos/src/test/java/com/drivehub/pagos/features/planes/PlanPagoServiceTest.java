package com.drivehub.pagos.features.planes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.drivehub.pagos.common.exception.BusinessException;
import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.config.PagosProperties;
import com.drivehub.pagos.features.planes.dto.CrearPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.EditarPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.PlanPagoResponse;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;
import com.drivehub.pagos.features.planes.service.ConversionMoneda;
import com.drivehub.pagos.features.planes.service.PlanPagoService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class PlanPagoServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();

    @Mock
    private PlanPagoRepository repositorio;

    private PlanPagoService servicio;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void preparar() {
        servicio = new PlanPagoService(repositorio, new ConversionMoneda(new PagosProperties(new BigDecimal("6.96"))));
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(PlanPagoService.class)).addAppender(logs);
    }

    @AfterEach
    void limpiarLogs() {
        ((Logger) LoggerFactory.getLogger(PlanPagoService.class)).detachAppender(logs);
    }

    private static PlanPago plan(String nombre, int horas, String costo, boolean activo) {
        PlanPago plan = new PlanPago(ESCUELA, nombre, (short) horas, new BigDecimal(costo));
        plan.setActivo(activo);
        return plan;
    }

    private static BusinessException error(Runnable accion) {
        return (BusinessException) catchThrowable(accion);
    }

    private static Throwable catchThrowable(Runnable accion) {
        try {
            accion.run();
        } catch (Throwable e) {
            return e;
        }
        throw new AssertionError("Se esperaba una excepción");
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_recortaElNombre_dejaElPlanActivoYCalculaElDolar() {
        when(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(ESCUELA, "Plan 10")).thenReturn(false);
        when(repositorio.saveAndFlush(any(PlanPago.class))).thenAnswer(i -> i.getArgument(0));

        PlanPagoResponse respuesta = servicio.crear(ESCUELA,
            new CrearPlanPagoRequest("  Plan 10  ", 10, new BigDecimal("450.00")));

        ArgumentCaptor<PlanPago> guardado = ArgumentCaptor.forClass(PlanPago.class);
        verify(repositorio).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(guardado.getValue().getNombre()).isEqualTo("Plan 10");
        assertThat(guardado.getValue().isActivo()).isTrue();
        assertThat(respuesta.nombre()).isEqualTo("Plan 10");
        assertThat(respuesta.horas()).isEqualTo(10);
        assertThat(respuesta.costoBs()).isEqualByComparingTo("450.00");
        assertThat(respuesta.costoUsd()).isEqualByComparingTo("64.66");
        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void crear_conUnNombreYaUsadoEnLaEscuela_respondeErrPag01_sinGuardar() {
        when(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(ESCUELA, "plan 10")).thenReturn(true);

        BusinessException ex = error(() -> servicio.crear(ESCUELA,
            new CrearPlanPagoRequest("plan 10", 10, new BigDecimal("450.00"))));

        assertThat(ex).isInstanceOf(ConflictException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_01");
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void crear_siLaBaseRechazaElNombreEnUnaCarrera_tambienRespondeErrPag01() {
        when(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(any(), any())).thenReturn(false);
        when(repositorio.saveAndFlush(any(PlanPago.class))).thenThrow(new DataIntegrityViolationException("x",
            new RuntimeException("duplicate key value violates unique constraint \"uq_plan_pago_escuela_nombre\"")));

        BusinessException ex = error(() -> servicio.crear(ESCUELA,
            new CrearPlanPagoRequest("Plan 10", 10, new BigDecimal("450.00"))));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_01");
    }

    @Test
    void crear_otraViolacionDeIntegridad_noSeDisfrazaDeNombreEnUso() {
        when(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(any(), any())).thenReturn(false);
        when(repositorio.saveAndFlush(any(PlanPago.class))).thenThrow(new DataIntegrityViolationException("x",
            new RuntimeException("new row violates check constraint \"ck_plan_pago_horas\"")));

        assertThatThrownBy(() -> servicio.crear(ESCUELA, new CrearPlanPagoRequest("Plan", 10, new BigDecimal("1.00"))))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void crear_elMismoNombreEnOtraEscuela_seAcepta_porqueLaComprobacionEsPorEscuela() {
        UUID otraEscuela = UUID.randomUUID();
        when(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(otraEscuela, "Plan 10")).thenReturn(false);
        when(repositorio.saveAndFlush(any(PlanPago.class))).thenAnswer(i -> i.getArgument(0));

        PlanPagoResponse respuesta = servicio.crear(otraEscuela,
            new CrearPlanPagoRequest("Plan 10", 10, new BigDecimal("450.00")));

        assertThat(respuesta.nombre()).isEqualTo("Plan 10");
        // * La pregunta se hace con la escuela de quien crea, nunca con otra
        verify(repositorio).existsByEscuelaCodigoAndNombreIgnoreCase(otraEscuela, "Plan 10");
        verify(repositorio, never()).existsByEscuelaCodigoAndNombreIgnoreCase(eq(ESCUELA), any());
    }

    // ------------------------------------------------------------------ listar

    @SuppressWarnings("unchecked")
    private Pageable listarYCapturarPagina(Integer page, Integer pageSize, String search, Boolean activo) {
        when(repositorio.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<PlanPago>(List.of(plan("A", 10, "450.00", true))));
        servicio.listar(ESCUELA, page, pageSize, search, activo);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findAll(any(Specification.class), pagina.capture());
        return pagina.getValue();
    }

    @Test
    void listar_sinParametros_pideLaPagina1DeVeinteDelMasRecientePrimero() {
        Pageable pagina = listarYCapturarPagina(null, null, null, null);

        assertThat(pagina.getPageNumber()).isZero();
        assertThat(pagina.getPageSize()).isEqualTo(20);
        assertThat(pagina.getSort().toString()).contains("createdAt: DESC").contains("id: DESC");
    }

    @Test
    void listar_elTamanoNuncaPasaDeCien_yUnaPaginaInvalidaEsLaPrimera() {
        Pageable pagina = listarYCapturarPagina(0, 5000, null, null);

        assertThat(pagina.getPageSize()).isEqualTo(100);
        assertThat(pagina.getPageNumber()).isZero();
    }

    @Test
    void listar_respetaLaPaginaPedida() {
        Pageable pagina = listarYCapturarPagina(3, 10, "plan", true);

        assertThat(pagina.getPageNumber()).isEqualTo(2);
        assertThat(pagina.getPageSize()).isEqualTo(10);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listar_devuelveLosPlanesConSuCostoEnDolares() {
        when(repositorio.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<PlanPago>(List.of(plan("A", 10, "696.00", true))));

        Page<PlanPagoResponse> pagina = servicio.listar(ESCUELA, null, null, "  ", null);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).costoUsd()).isEqualByComparingTo("100.00");
    }

    // ------------------------------------------------------------------ obtener

    @Test
    void obtener_deOtraEscuelaOInexistente_respondeErrPag02() {
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA)).thenReturn(Optional.empty());

        BusinessException ex = error(() -> servicio.obtener(ESCUELA, CODIGO));

        assertThat(ex).isInstanceOf(NotFoundException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_02");
    }

    // ------------------------------------------------------------------ editar

    @Test
    void editar_soloLasHoras_dejaElRestoIgual_yNoCambiaElEstado() {
        PlanPago existente = plan("Plan 10", 10, "450.00", false);
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA)).thenReturn(Optional.of(existente));
        when(repositorio.saveAndFlush(any(PlanPago.class))).thenAnswer(i -> i.getArgument(0));

        PlanPagoResponse respuesta = servicio.editar(ESCUELA, CODIGO, new EditarPlanPagoRequest(null, 20, null));

        assertThat(respuesta.horas()).isEqualTo(20);
        assertThat(respuesta.nombre()).isEqualTo("Plan 10");
        assertThat(respuesta.costoBs()).isEqualByComparingTo("450.00");
        assertThat(respuesta.activo()).isFalse();
        verify(repositorio, never()).existsByEscuelaCodigoAndNombreIgnoreCaseAndCodigoNot(any(), any(), any());
    }

    @Test
    void editar_elCostoCambiaElDolar() {
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA))
            .thenReturn(Optional.of(plan("Plan 10", 10, "450.00", true)));
        when(repositorio.saveAndFlush(any(PlanPago.class))).thenAnswer(i -> i.getArgument(0));

        PlanPagoResponse respuesta = servicio.editar(ESCUELA, CODIGO,
            new EditarPlanPagoRequest(null, null, new BigDecimal("696.00")));

        assertThat(respuesta.costoBs()).isEqualByComparingTo("696.00");
        assertThat(respuesta.costoUsd()).isEqualByComparingTo("100.00");
    }

    @Test
    void editar_conUnCuerpoSinCampos_respondeErrSys01() {
        BusinessException ex = error(() -> servicio.editar(ESCUELA, CODIGO, new EditarPlanPagoRequest(null, null, null)));

        assertThat(ex).isInstanceOf(ValidationException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_SYS_01");
    }

    @Test
    void editar_conUnNombreEnBlanco_respondeErrSys01() {
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA))
            .thenReturn(Optional.of(plan("Plan 10", 10, "450.00", true)));

        BusinessException ex = error(() -> servicio.editar(ESCUELA, CODIGO, new EditarPlanPagoRequest("   ", null, null)));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_SYS_01");
    }

    @Test
    void editar_aUnNombreDeOtroPlan_respondeErrPag01() {
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA))
            .thenReturn(Optional.of(plan("Plan 10", 10, "450.00", true)));
        when(repositorio.existsByEscuelaCodigoAndNombreIgnoreCaseAndCodigoNot(ESCUELA, "Plan 20", CODIGO))
            .thenReturn(true);

        BusinessException ex = error(() -> servicio.editar(ESCUELA, CODIGO, new EditarPlanPagoRequest("Plan 20", null, null)));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_01");
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void editar_aSuPropioNombre_noChoca() {
        PlanPago existente = plan("Plan 10", 10, "450.00", true);
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA)).thenReturn(Optional.of(existente));
        when(repositorio.existsByEscuelaCodigoAndNombreIgnoreCaseAndCodigoNot(ESCUELA, "PLAN 10", CODIGO))
            .thenReturn(false);
        when(repositorio.saveAndFlush(any(PlanPago.class))).thenAnswer(i -> i.getArgument(0));

        PlanPagoResponse respuesta = servicio.editar(ESCUELA, CODIGO, new EditarPlanPagoRequest(" PLAN 10 ", null, null));

        assertThat(respuesta.nombre()).isEqualTo("PLAN 10");
    }

    @Test
    void editar_deOtraEscuelaOInexistente_respondeErrPag02() {
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA)).thenReturn(Optional.empty());

        BusinessException ex = error(() -> servicio.editar(ESCUELA, CODIGO, new EditarPlanPagoRequest("X", null, null)));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_02");
    }

    // ------------------------------------------------------------------ desactivar y activar

    @Test
    void desactivar_dejaElPlanDesactivado() {
        PlanPago existente = plan("Plan 10", 10, "450.00", true);
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA)).thenReturn(Optional.of(existente));
        when(repositorio.save(any(PlanPago.class))).thenAnswer(i -> i.getArgument(0));

        PlanPagoResponse respuesta = servicio.desactivar(ESCUELA, CODIGO);

        assertThat(respuesta.activo()).isFalse();
    }

    @Test
    void desactivar_unoYaDesactivado_respondeErrPag03() {
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA))
            .thenReturn(Optional.of(plan("Plan 10", 10, "450.00", false)));

        BusinessException ex = error(() -> servicio.desactivar(ESCUELA, CODIGO));

        assertThat(ex).isInstanceOf(ConflictException.class);
        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_03");
        verify(repositorio, never()).save(any());
    }

    @Test
    void activar_dejaElPlanActivo_yUnoYaActivoRespondeErrPag03() {
        PlanPago desactivado = plan("Plan 10", 10, "450.00", false);
        when(repositorio.findByCodigoAndEscuelaCodigo(CODIGO, ESCUELA)).thenReturn(Optional.of(desactivado));
        when(repositorio.save(any(PlanPago.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(servicio.activar(ESCUELA, CODIGO).activo()).isTrue();
        BusinessException ex = error(() -> servicio.activar(ESCUELA, CODIGO));

        assertThat(ex.getErrorCode()).isEqualTo("ERR_PAG_03");
    }

    @Test
    void desactivarYActivar_deOtraEscuelaOInexistente_respondenErrPag02() {
        when(repositorio.findByCodigoAndEscuelaCodigo(eq(CODIGO), any())).thenReturn(Optional.empty());

        assertThat(error(() -> servicio.desactivar(ESCUELA, CODIGO)).getErrorCode()).isEqualTo("ERR_PAG_02");
        assertThat(error(() -> servicio.activar(ESCUELA, CODIGO)).getErrorCode()).isEqualTo("ERR_PAG_02");
    }

    // ------------------------------------------------------------------ logs

    @Test
    void loQueSeRegistraNuncaIncluyeElNombreNiElCosto() {
        when(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(any(), any())).thenReturn(false);
        when(repositorio.saveAndFlush(any(PlanPago.class))).thenAnswer(i -> i.getArgument(0));

        servicio.crear(ESCUELA, new CrearPlanPagoRequest("Nombre secreto del plan", 10, new BigDecimal("1234.56")));

        assertThat(logs.list).isNotEmpty();
        assertThat(logs.list).allSatisfy(evento -> {
            String todo = evento.getFormattedMessage() + java.util.Arrays.toString(evento.getArgumentArray());
            assertThat(todo).doesNotContain("Nombre secreto").doesNotContain("1234.56");
            assertThat(todo).contains("planes");
        });
    }
}
