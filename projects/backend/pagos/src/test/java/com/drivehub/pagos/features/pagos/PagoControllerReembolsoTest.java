package com.drivehub.pagos.features.pagos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import com.drivehub.pagos.features.pagos.controller.PagoController;
import com.drivehub.pagos.features.pagos.dto.PagoDetalleResponse;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.enums.MotivoReembolso;
import com.drivehub.pagos.features.pagos.service.ConsultaPagosService;
import com.drivehub.pagos.features.pagos.service.PagoService;
import com.drivehub.pagos.features.pagos.service.ReversionService;

/**
 * El contrato HTTP del reembolso (CU31): status, sobre, validación del motivo y errores. El service va mockeado.
 */
@WebMvcTest(PagoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class PagoControllerReembolsoTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID PAGO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/pagos/" + PAGO + "/reembolso";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagoService pagoService;

    @MockitoBean
    private ConsultaPagosService consulta;

    @MockitoBean
    private ReversionService reversion;

    private static JwtRequestPostProcessor estudiante() {
        return jwt().jwt(j -> j.subject(ESTUDIANTE.toString()).claim("escuela", ESCUELA.toString()));
    }

    private static PagoDetalleResponse enProceso() {
        return new PagoDetalleResponse(PAGO, "Plan 10 horas", (short) 10, new BigDecimal("38.23"),
            new BigDecimal("450.00"), EstadoPagoVisible.REEMBOLSO_EN_PROCESO, Instant.parse("2026-10-25T12:00:00Z"),
            (short) 0, Instant.parse("2026-10-25T12:05:00Z"), false, null, List.of());
    }

    private org.springframework.test.web.servlet.ResultActions pedir(String cuerpo) throws Exception {
        return mockMvc.perform(post(RUTA).with(estudiante()).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void reembolsar_responde202ConElSobreYElEstadoNuevo_yElMotivoLlegaAlService() throws Exception {
        when(reversion.solicitarReembolso(ESCUELA, ESTUDIANTE, PAGO, MotivoReembolso.OTRO, "Perdí mi trabajo"))
            .thenReturn(enProceso());

        pedir("{\"motivo\":\"OTRO\",\"motivoDetalle\":\"Perdí mi trabajo\"}")
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(PAGO.toString()))
            .andExpect(jsonPath("$.data.estado").value("REEMBOLSO_EN_PROCESO"))
            .andExpect(jsonPath("$.data.id").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.timestamp").exists());
        verify(reversion).solicitarReembolso(ESCUELA, ESTUDIANTE, PAGO, MotivoReembolso.OTRO, "Perdí mi trabajo");
    }

    @Test
    void reembolsar_sinDetalle_loPasaNulo() throws Exception {
        when(reversion.solicitarReembolso(ESCUELA, ESTUDIANTE, PAGO, MotivoReembolso.MUDANZA, null))
            .thenReturn(enProceso());

        pedir("{\"motivo\":\"MUDANZA\"}").andExpect(status().isAccepted());
    }

    @Test
    void sinCuerpoOSinMotivoOConUnMotivoDesconocido_responde400ErrSys01_sinLlamarAlService() throws Exception {
        mockMvc.perform(post(RUTA).with(estudiante())).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        pedir("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        pedir("{\"motivo\":null}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        pedir("{\"motivo\":\"PORQUE_SI\"}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(reversion);
    }

    @Test
    void otroSinExplicacion_lovalidaElService_yResponde400() throws Exception {
        when(reversion.solicitarReembolso(ESCUELA, ESTUDIANTE, PAGO, MotivoReembolso.OTRO, null))
            .thenThrow(new ValidationException("ERR_SYS_01", "Cuéntanos el motivo, de 5 a 200 caracteres"));

        pedir("{\"motivo\":\"OTRO\"}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void unPagoAjenoOInexistente_responde404ErrPag07() throws Exception {
        when(reversion.solicitarReembolso(any(), any(), any(), any(), any()))
            .thenThrow(new NotFoundException("ERR_PAG_07", "El pago no existe"));

        pedir("{\"motivo\":\"MUDANZA\"}").andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_PAG_07"));
    }

    @Test
    void unPagoSinConfirmarOYaReembolsado_responde409ErrPag10() throws Exception {
        when(reversion.solicitarReembolso(any(), any(), any(), any(), any()))
            .thenThrow(new ConflictException("ERR_PAG_10", "No se puede reembolsar"));

        pedir("{\"motivo\":\"MUDANZA\"}").andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_10"));
    }

    @Test
    void unaCompraConHorasUsadas_responde409ErrPag11() throws Exception {
        when(reversion.solicitarReembolso(any(), any(), any(), any(), any()))
            .thenThrow(new ConflictException("ERR_PAG_11", "Ya usaste horas"));

        pedir("{\"motivo\":\"MUDANZA\"}").andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_11"));
    }

    @Test
    void laPasarelaCaida_responde503ErrPag06() throws Exception {
        when(reversion.solicitarReembolso(any(), any(), any(), any(), any()))
            .thenThrow(new ServicioNoDisponibleException("ERR_PAG_06", "Sin pasarela"));

        pedir("{\"motivo\":\"MUDANZA\"}").andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("ERR_PAG_06"));
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"MUDANZA\"}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(reversion);
    }

    @Test
    void conOtroRol_responde403() throws Exception {
        when(reversion.solicitarReembolso(any(), any(), any(), any(), any()))
            .thenThrow(new AccessDeniedException("rol"));

        pedir("{\"motivo\":\"MUDANZA\"}").andExpect(status().isForbidden());
    }
}
