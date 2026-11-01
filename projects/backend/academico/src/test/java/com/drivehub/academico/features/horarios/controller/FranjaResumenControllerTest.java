package com.drivehub.academico.features.horarios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.horarios.dto.ResumenFranjasInstructorResponse;
import com.drivehub.academico.features.horarios.service.FranjaDisponibleService;
import com.drivehub.academico.features.horarios.service.FranjaResumenService;
import com.drivehub.academico.features.horarios.service.FranjaService;

/**
 * El resumen de franjas por instructor del Administrador (CU11): contrato HTTP y forma del sobre. La lógica
 * está en `FranjaResumenServiceTest`; el 403 por rol lo decide el @PreAuthorize del servicio (mockeado acá).
 */
@WebMvcTest(FranjaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class FranjaResumenControllerTest {

    private static final String RUTA = "/api/v1/franjas/resumen";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FranjaResumenService resumenService;
    @MockitoBean
    private FranjaService franjaService;
    @MockitoBean
    private FranjaDisponibleService disponibleService;
    @MockitoBean
    private BearerActual bearerActual;

    private final UUID administrador = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();

    private RequestPostProcessor comoAdministrador() {
        return jwt().jwt(j -> j.subject(administrador.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_ESCUELA"));
    }

    @Test
    void resumen_responde200ConLosConteosPorInstructorYMeta() throws Exception {
        when(resumenService.resumirDelDia(escuela, FECHA, "12:00", "18:00"))
            .thenReturn(List.of(new ResumenFranjasInstructorResponse(instructor, 6, 3, 2, 1)));

        mockMvc.perform(get(RUTA).param("filter[fecha]", "2026-11-02").param("filter[horaDesde]", "12:00")
                .param("filter[horaHasta]", "18:00").with(comoAdministrador()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Resumen de franjas obtenido correctamente"))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].instructorCodigo").value(instructor.toString()))
            .andExpect(jsonPath("$.data[0].total").value(6))
            .andExpect(jsonPath("$.data[0].libres").value(3))
            .andExpect(jsonPath("$.data[0].reservadas").value(2))
            .andExpect(jsonPath("$.data[0].bloqueadas").value(1))
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void resumen_sinFiltros_laEscuelaSaleDelTokenYTodoLoDemasVaNulo() throws Exception {
        when(resumenService.resumirDelDia(escuela, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get(RUTA).with(comoAdministrador()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.meta.totalItems").value(0));

        verify(resumenService).resumirDelDia(escuela, null, null, null);
    }

    @Test
    void resumen_conUnaFechaInvalida_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(RUTA).param("filter[fecha]", "mañana").with(comoAdministrador()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verifyNoInteractions(resumenService);
    }

    @Test
    void resumen_conUnaHoraInvalida_responde400ErrSys01() throws Exception {
        when(resumenService.resumirDelDia(any(), any(), any(), any()))
            .thenThrow(new ValidationException(AcademicoErrorCodes.VALIDACION, "La hora debe tener el formato HH:mm"));

        mockMvc.perform(get(RUTA).param("filter[horaDesde]", "25:00").with(comoAdministrador()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void resumen_conUnTokenSinEscuela_responde403ErrAca16() throws Exception {
        when(resumenService.resumirDelDia(any(), any(), any(), any()))
            .thenThrow(new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Sin escuela"));

        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(administrador.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_ESCUELA"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_ACA_16"));
    }

    @Test
    void resumen_sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());

        verifyNoInteractions(resumenService);
    }
}
