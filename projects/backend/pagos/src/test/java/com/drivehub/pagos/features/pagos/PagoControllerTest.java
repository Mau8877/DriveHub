package com.drivehub.pagos.features.pagos;

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
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import com.drivehub.pagos.features.pagos.controller.PagoController;
import com.drivehub.pagos.features.pagos.dto.EstadoPagoResponse;
import com.drivehub.pagos.features.pagos.dto.PagoIniciadoResponse;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
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
    private static final String JSON_OK = "{\"planCodigo\":\"" + PLAN + "\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagoService servicio;

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
}
