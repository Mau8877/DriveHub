package com.drivehub.pagos.features.pagos;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.comprobantes.dto.ComprobanteResumen;
import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;
import com.drivehub.pagos.features.pagos.controller.PagoController;
import com.drivehub.pagos.features.pagos.dto.EstadoPagoResponse;
import com.drivehub.pagos.features.pagos.dto.PagoDetalleResponse;
import com.drivehub.pagos.features.pagos.dto.PagoIniciadoResponse;
import com.drivehub.pagos.features.pagos.dto.PagoResumenResponse;
import com.drivehub.pagos.features.pagos.dto.ReembolsoPrevisto;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.service.ConsultaPagosService;
import com.drivehub.pagos.features.pagos.service.PagoService;

/**
 * El contrato HTTP de los pagos del estudiante (CU26): status, forma del sobre y validación. El service va
 * mockeado: su lógica y el rol (`@PreAuthorize`) se prueban en sus tests.
 */
@WebMvcTest(PagoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class PagoControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID PAGO = UUID.randomUUID();
    private static final UUID COMPROBANTE = UUID.randomUUID();
    private static final String JSON_OK = "{\"planCodigo\":\"" + PLAN + "\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagoService servicio;

    @MockitoBean
    private ConsultaPagosService consulta;

    private static JwtRequestPostProcessor estudiante() {
        return jwt().jwt(j -> j.subject(ESTUDIANTE.toString()).claim("escuela", ESCUELA.toString()));
    }

    private static PagoIniciadoResponse pago(EstadoPago estado, String url) {
        return new PagoIniciadoResponse(PAGO, estado, url, Instant.parse("2026-10-25T12:31:00Z"), "Plan 10 horas",
            (short) 10, new BigDecimal("38.23"), new BigDecimal("450.00"));
    }

    @Test
    void iniciar_conUnaCompraNueva_responde201ConElPagoEnElSobre_yElEstudianteYLaEscuelaSalenDelToken()
            throws Exception {
        when(servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, "clave-1")).thenReturn(
            new PagoService.ResultadoIniciar(pago(EstadoPago.PENDIENTE, "https://pago/1"), true));

        mockMvc.perform(post("/api/v1/pagos").with(estudiante()).header("Idempotency-Key", "clave-1")
                .contentType(MediaType.APPLICATION_JSON).content(JSON_OK))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(PAGO.toString()))
            .andExpect(jsonPath("$.data.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.data.urlPago").value("https://pago/1"))
            .andExpect(jsonPath("$.data.planNombre").value("Plan 10 horas"))
            .andExpect(jsonPath("$.data.horas").value(10))
            .andExpect(jsonPath("$.data.montoUsd").value(38.23))
            .andExpect(jsonPath("$.data.montoBs").value(450.00))
            .andExpect(jsonPath("$.data.escuelaCodigo").doesNotExist())
            .andExpect(jsonPath("$.data.id").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void iniciar_conUnReintentoDeLaMismaClave_responde200ConElMismoPago() throws Exception {
        when(servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, "clave-1")).thenReturn(
            new PagoService.ResultadoIniciar(pago(EstadoPago.PENDIENTE, "https://pago/1"), false));

        mockMvc.perform(post("/api/v1/pagos").with(estudiante()).header("Idempotency-Key", "clave-1")
                .contentType(MediaType.APPLICATION_JSON).content(JSON_OK))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.codigo").value(PAGO.toString()));
    }

    @Test
    void iniciar_sinElHeaderDeLaClave_pasaNuloAlService_queResponde400ErrSys01() throws Exception {
        when(servicio.iniciar(ESCUELA, ESTUDIANTE, PLAN, null))
            .thenThrow(new ValidationException("ERR_SYS_01", "La clave de la compra es obligatoria"));

        mockMvc.perform(post("/api/v1/pagos").with(estudiante()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON_OK))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void iniciar_sinPlanOConUnCodigoQueNoEsUuid_responde400SinLlamarAlService() throws Exception {
        mockMvc.perform(post("/api/v1/pagos").with(estudiante()).header("Idempotency-Key", "clave-1")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
            .andExpect(jsonPath("$.message").value("Elige el plan que quieres comprar"));
        mockMvc.perform(post("/api/v1/pagos").with(estudiante()).header("Idempotency-Key", "clave-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"planCodigo\":\"no-es-uuid\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(servicio);
    }

    @Test
    void iniciar_traduceLosErroresDeNegocioConSuCodigo() throws Exception {
        when(servicio.iniciar(eq(ESCUELA), eq(ESTUDIANTE), eq(PLAN), any()))
            .thenThrow(new NotFoundException("ERR_PAG_05", "El plan ya no está disponible"));
        mockMvc.perform(post("/api/v1/pagos").with(estudiante()).header("Idempotency-Key", "a")
                .contentType(MediaType.APPLICATION_JSON).content(JSON_OK))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PAG_05"));

        when(servicio.iniciar(eq(ESCUELA), eq(ESTUDIANTE), eq(PLAN), any()))
            .thenThrow(new ConflictException("ERR_PAG_18", "Esa clave de compra ya se usó"));
        mockMvc.perform(post("/api/v1/pagos").with(estudiante()).header("Idempotency-Key", "b")
                .contentType(MediaType.APPLICATION_JSON).content(JSON_OK))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_PAG_18"));

        when(servicio.iniciar(eq(ESCUELA), eq(ESTUDIANTE), eq(PLAN), any()))
            .thenThrow(new ServicioNoDisponibleException("ERR_PAG_06", "No pudimos conectar con el pago"));
        mockMvc.perform(post("/api/v1/pagos").with(estudiante()).header("Idempotency-Key", "c")
                .contentType(MediaType.APPLICATION_JSON).content(JSON_OK))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value("ERR_PAG_06"));
    }

    @Test
    void iniciar_sinEscuelaEnElToken_responde403ErrPag04() throws Exception {
        mockMvc.perform(post("/api/v1/pagos").with(jwt().jwt(j -> j.subject(ESTUDIANTE.toString())))
                .header("Idempotency-Key", "clave-1").contentType(MediaType.APPLICATION_JSON).content(JSON_OK))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_PAG_04"));
        verifyNoInteractions(servicio);
    }

    @Test
    void iniciar_sinSesion_responde401() throws Exception {
        mockMvc.perform(post("/api/v1/pagos").header("Idempotency-Key", "clave-1")
                .contentType(MediaType.APPLICATION_JSON).content(JSON_OK))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void estado_respondeConElEstadoDelPago() throws Exception {
        when(servicio.estado(ESCUELA, ESTUDIANTE, PAGO)).thenReturn(new EstadoPagoResponse(PAGO, EstadoPago.CONFIRMADO));

        mockMvc.perform(get("/api/v1/pagos/{codigo}/estado", PAGO).with(estudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(PAGO.toString()))
            .andExpect(jsonPath("$.data.estado").value("CONFIRMADO"));
    }

    @Test
    void estado_deUnPagoAjeno_responde404ErrPag07() throws Exception {
        when(servicio.estado(ESCUELA, ESTUDIANTE, PAGO))
            .thenThrow(new NotFoundException("ERR_PAG_07", "El pago no existe"));

        mockMvc.perform(get("/api/v1/pagos/{codigo}/estado", PAGO).with(estudiante()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_PAG_07"));
    }

    @Test
    void estado_conUnCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/no-es-uuid/estado").with(estudiante()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void estado_sinSesion_responde401() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/{codigo}/estado", PAGO)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ historial (CU28)

    private static PagoResumenResponse resumen(EstadoPagoVisible estado) {
        return new PagoResumenResponse(PAGO, "Plan 10 horas", (short) 10, new BigDecimal("38.23"),
            new BigDecimal("450.00"), estado, Instant.parse("2026-10-25T12:00:00Z"));
    }

    @Test
    void listar_respondeElSobreConLosPagosYElMeta_yNoExponeElIdNiLaEscuela() throws Exception {
        when(consulta.listar(ESCUELA, ESTUDIANTE, null, null, null))
            .thenReturn(new PageImpl<>(List.of(resumen(EstadoPagoVisible.CONFIRMADO)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/pagos").with(estudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].codigo").value(PAGO.toString()))
            .andExpect(jsonPath("$.data[0].planNombre").value("Plan 10 horas"))
            .andExpect(jsonPath("$.data[0].horas").value(10))
            .andExpect(jsonPath("$.data[0].montoUsd").value(38.23))
            .andExpect(jsonPath("$.data[0].montoBs").value(450.00))
            .andExpect(jsonPath("$.data[0].estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.data[0].creadoEn").exists())
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.data[0].escuelaCodigo").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.pageSize").value(20))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void listar_traduceLosParametrosDePaginacionYElFiltroDeEstado() throws Exception {
        when(consulta.listar(ESCUELA, ESTUDIANTE, 2, 5, EstadoPagoVisible.REEMBOLSO_EN_PROCESO))
            .thenReturn(new PageImpl<>(List.of(resumen(EstadoPagoVisible.REEMBOLSO_EN_PROCESO)),
                PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/api/v1/pagos").with(estudiante()).param("page", "2").param("pageSize", "5")
                .param("filter[estado]", "REEMBOLSO_EN_PROCESO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].estado").value("REEMBOLSO_EN_PROCESO"))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void listar_conUnEstadoDesconocido_responde400ErrSys01SinLlamarAlService() throws Exception {
        mockMvc.perform(get("/api/v1/pagos").with(estudiante()).param("filter[estado]", "INVENTADO"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(consulta);
    }

    @Test
    void listar_sinSesion_responde401_yConOtroRol_responde403() throws Exception {
        mockMvc.perform(get("/api/v1/pagos")).andExpect(status().isUnauthorized());

        when(consulta.listar(ESCUELA, ESTUDIANTE, null, null, null)).thenThrow(new AccessDeniedException("rol"));
        mockMvc.perform(get("/api/v1/pagos").with(estudiante()))
            .andExpect(status().isForbidden());
    }

    @Test
    void detalle_respondeTodosLosCampos_incluidoElReembolsoPrevisto() throws Exception {
        when(consulta.detalle(ESCUELA, ESTUDIANTE, PAGO)).thenReturn(new PagoDetalleResponse(PAGO, "Plan 10 horas",
            (short) 10, new BigDecimal("100.00"), new BigDecimal("450.00"), EstadoPagoVisible.CONFIRMADO,
            Instant.parse("2026-10-25T12:00:00Z"), (short) 10, Instant.parse("2026-10-25T12:01:00Z"), true,
            new ReembolsoPrevisto(new BigDecimal("95.00"), new BigDecimal("5.00"), 5),
            List.of(new ComprobanteResumen(COMPROBANTE, TipoComprobante.PAGO, "CMP-2026-000184", true))));

        mockMvc.perform(get("/api/v1/pagos/{codigo}", PAGO).with(estudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(PAGO.toString()))
            .andExpect(jsonPath("$.data.estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.data.horasRestantes").value(10))
            .andExpect(jsonPath("$.data.confirmadoEn").exists())
            .andExpect(jsonPath("$.data.reembolsable").value(true))
            .andExpect(jsonPath("$.data.reembolso.montoDevuelto").value(95.00))
            .andExpect(jsonPath("$.data.reembolso.montoRetenido").value(5.00))
            .andExpect(jsonPath("$.data.reembolso.porcentajeRetenido").value(5))
            .andExpect(jsonPath("$.data.comprobantes[0].codigo").value(COMPROBANTE.toString()))
            .andExpect(jsonPath("$.data.comprobantes[0].tipo").value("PAGO"))
            .andExpect(jsonPath("$.data.comprobantes[0].numero").value("CMP-2026-000184"))
            .andExpect(jsonPath("$.data.comprobantes[0].disponible").value(true))
            .andExpect(jsonPath("$.data.id").doesNotExist())
            .andExpect(jsonPath("$.data.escuelaCodigo").doesNotExist());
    }

    @Test
    void detalle_sinReembolsoPrevisto_respondeReembolsableFalseYReembolsoNulo() throws Exception {
        when(consulta.detalle(ESCUELA, ESTUDIANTE, PAGO)).thenReturn(new PagoDetalleResponse(PAGO, "Plan 10 horas",
            (short) 10, new BigDecimal("100.00"), new BigDecimal("450.00"), EstadoPagoVisible.PENDIENTE,
            Instant.parse("2026-10-25T12:00:00Z"), null, null, false, null, List.of()));

        mockMvc.perform(get("/api/v1/pagos/{codigo}", PAGO).with(estudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reembolsable").value(false))
            .andExpect(jsonPath("$.data.reembolso").value(nullValue()))
            .andExpect(jsonPath("$.data.comprobantes").isEmpty());
    }

    @Test
    void detalle_deUnPagoAjeno_responde404ErrPag07() throws Exception {
        when(consulta.detalle(ESCUELA, ESTUDIANTE, PAGO))
            .thenThrow(new NotFoundException("ERR_PAG_07", "El pago no existe"));

        mockMvc.perform(get("/api/v1/pagos/{codigo}", PAGO).with(estudiante()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_PAG_07"));
    }

    @Test
    void detalle_sinSesion_responde401_yConUnCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(get("/api/v1/pagos/{codigo}", PAGO)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/pagos/no-es-uuid").with(estudiante()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }
}
