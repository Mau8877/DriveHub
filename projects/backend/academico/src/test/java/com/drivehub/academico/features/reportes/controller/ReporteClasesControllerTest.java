package com.drivehub.academico.features.reportes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.features.reportes.dto.ReporteClaseResponse;
import com.drivehub.academico.features.reportes.repository.FiltroReporteClases;
import com.drivehub.academico.features.reportes.service.ReporteClasesService;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;

@WebMvcTest(ReporteClasesController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReporteClasesControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final String RUTA = "/api/v1/reportes-clases";
    private static final String BEARER = "Bearer token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteClasesService service;
    @MockitoBean
    private BearerActual bearerActual;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("escuela", ESCUELA.toString()));
    }

    @Test
    void listar_devuelveArrayConMetaYPasaLosFiltros() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        ReporteClaseResponse clase = new ReporteClaseResponse(UUID.randomUUID(), LocalDate.of(2026, 10, 15), "08:00",
            "09:00", EstadoReserva.CANCELADA, UUID.randomUUID(), "Ana Pérez", INSTRUCTOR, "Luis Rojas", 1, false);
        when(service.listar(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(clase), PageRequest.of(0, 500), 1));

        mockMvc.perform(get(RUTA).with(sesion())
                .param("filter[desde]", "2026-10-01").param("filter[hasta]", "2026-10-31")
                .param("filter[estado]", "CANCELADA").param("filter[instructorCodigo]", INSTRUCTOR.toString())
                .param("page", "1").param("pageSize", "500"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].estudianteNombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.data[0].estado").value("CANCELADA"))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.error").doesNotExist());

        verify(service).listar(ESCUELA, BEARER, new FiltroReporteClases(LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31), EstadoReserva.CANCELADA, INSTRUCTOR), 1, 500);
    }

    @Test
    void listar_conEstadoInvalido_responde400() throws Exception {
        mockMvc.perform(get(RUTA).with(sesion()).param("filter[estado]", "INEXISTENTE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void listar_conRangoInvertido_responde400ConSuCodigo() throws Exception {
        when(service.listar(any(), any(), any(), any(), any())).thenThrow(new ValidationException(
            AcademicoErrorCodes.RANGO_DE_FECHAS_INVALIDO, "La fecha inicial no puede ser posterior a la final"));

        mockMvc.perform(get(RUTA).with(sesion()).param("filter[desde]", "2026-11-01").param("filter[hasta]", "2026-10-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(AcademicoErrorCodes.RANGO_DE_FECHAS_INVALIDO));
    }

    @Test
    void listar_sinEscuelaEnElToken_responde403() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(AcademicoErrorCodes.TOKEN_SIN_ESCUELA));
    }

    @Test
    void listar_sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }
}
