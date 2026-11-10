package com.drivehub.pagos.features.pagos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.pagos.controller.ReportePagosController;
import com.drivehub.pagos.features.pagos.dto.ReportePagoResponse;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.service.ReportePagosService;

/**
 * El contrato HTTP del reporte de pagos del Administrador (CU52). El service va mockeado: su lógica y el rol se
 * prueban en su test.
 */
@WebMvcTest(ReportePagosController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class ReportePagosControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final String RUTA = "/api/v1/reportes-pagos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportePagosService service;

    @Test
    void listar_devuelveArrayConMetaYLaEscuelaSaleDelToken() throws Exception {
        ReportePagoResponse pago = new ReportePagoResponse(UUID.randomUUID(), Instant.parse("2026-10-20T15:00:00Z"),
            Instant.parse("2026-10-20T15:01:00Z"), UUID.randomUUID(), "Ana Pérez", "Plan 10 horas", (short) 10,
            (short) 8, new BigDecimal("38.23"), new BigDecimal("450.00"), EstadoPagoVisible.CONFIRMADO);
        when(service.listar(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(pago), PageRequest.of(0, 500), 1));

        mockMvc.perform(get(RUTA)
                .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("escuela", ESCUELA.toString())))
                .param("filter[desde]", "2026-10-01").param("filter[hasta]", "2026-10-31")
                .param("filter[estado]", "CONFIRMADO").param("page", "1").param("pageSize", "500"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].estudianteNombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.data[0].montoBs").value(450.00))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.error").doesNotExist());

        verify(service).listar(ESCUELA, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
            EstadoPagoVisible.CONFIRMADO, 1, 500);
    }

    @Test
    void listar_conRangoInvertido_responde400ConSuCodigo() throws Exception {
        when(service.listar(any(), any(), any(), any(), any(), any())).thenThrow(new ValidationException(
            PagosErrorCodes.RANGO_DE_FECHAS_INVALIDO, "La fecha inicial no puede ser posterior a la final"));

        mockMvc.perform(get(RUTA)
                .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("escuela", ESCUELA.toString())))
                .param("filter[desde]", "2026-11-01").param("filter[hasta]", "2026-10-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value(PagosErrorCodes.RANGO_DE_FECHAS_INVALIDO));
    }

    @Test
    void listar_sinEscuelaEnElToken_responde403() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(PagosErrorCodes.TOKEN_SIN_ESCUELA));
    }
}
