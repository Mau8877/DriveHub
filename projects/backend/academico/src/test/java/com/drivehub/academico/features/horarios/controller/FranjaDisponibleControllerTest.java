package com.drivehub.academico.features.horarios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.horarios.dto.FranjaDisponibleResponse;
import com.drivehub.academico.features.horarios.dto.InstructorDisponibleResponse;
import com.drivehub.academico.features.horarios.service.FranjaDisponibleService;
import com.drivehub.academico.features.horarios.service.FranjaService;

/**
 * La vista del estudiante de las franjas (CU14): contrato HTTP y forma del sobre. La lógica está en
 * `FranjaDisponibleServiceTest`.
 */
@WebMvcTest(FranjaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class FranjaDisponibleControllerTest {

    private static final String RUTA = "/api/v1/franjas";
    private static final String BEARER = "Bearer token-del-estudiante";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FranjaDisponibleService disponibleService;
    @MockitoBean
    private FranjaService franjaService;
    @MockitoBean
    private BearerActual bearerActual;

    private final UUID estudiante = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();

    private RequestPostProcessor comoEstudiante() {
        return jwt().jwt(j -> j.subject(estudiante.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"));
    }

    private FranjaDisponibleResponse franja(String inicio, String fin) {
        return new FranjaDisponibleResponse(UUID.randomUUID(), FECHA, inicio, fin, instructor, "Ivan Alfa");
    }

    // ---------------------------------------------------------------- GET /franjas/disponibles

    @Test
    void disponibles_responde200ConLasFranjasElInstructorYMeta() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(disponibleService.listar(eq(escuela), eq(BEARER), eq(FECHA), eq(FECHA), eq(instructor), eq("12:00"),
            eq("18:00"), eq(1), eq(20))).thenReturn(new PageImpl<>(
            List.of(franja("12:00", "13:00"), franja("13:00", "14:00")), PageRequest.of(0, 20), 2));

        mockMvc.perform(get(RUTA + "/disponibles").param("filter[fecha]", "2026-11-02")
                .param("filter[instructor]", instructor.toString()).param("filter[horaDesde]", "12:00")
                .param("filter[horaHasta]", "18:00").with(comoEstudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Franjas disponibles obtenidas correctamente"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].horaInicio").value("12:00"))
            .andExpect(jsonPath("$.data[0].instructorNombre").value("Ivan Alfa"))
            .andExpect(jsonPath("$.data[0].instructorCodigo").value(instructor.toString()))
            .andExpect(jsonPath("$.data[0].estado").doesNotExist())
            .andExpect(jsonPath("$.data[0].vehiculoCodigo").doesNotExist())
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.totalItems").value(2));
    }

    @Test
    void disponibles_conUnRango_pasaLasDosFechasYLaPagina() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(disponibleService.listar(any(), any(), any(), any(), any(), any(), any(), eq(2), eq(50)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 50), 0));

        mockMvc.perform(get(RUTA + "/disponibles").param("filter[fechaDesde]", "2026-11-02")
                .param("filter[fechaHasta]", "2026-11-08").param("page", "2").param("pageSize", "50")
                .with(comoEstudiante()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));

        verify(disponibleService).listar(escuela, BEARER, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), null,
            null, null, 2, 50);
    }

    @Test
    void disponibles_laFechaExactaManda_sobreElRango() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(disponibleService.listar(any(), any(), any(), any(), any(), any(), any(), eq(1), eq(20)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(RUTA + "/disponibles").param("filter[fecha]", "2026-11-02")
                .param("filter[fechaDesde]", "2026-11-05").param("filter[fechaHasta]", "2026-11-09")
                .with(comoEstudiante()))
            .andExpect(status().isOk());

        verify(disponibleService).listar(escuela, BEARER, FECHA, FECHA, null, null, null, 1, 20);
    }

    @Test
    void disponibles_conUnRangoInvalido_responde400ErrAca13() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(disponibleService.listar(any(), any(), any(), any(), any(), any(), any(), eq(1), eq(20)))
            .thenThrow(new ValidationException(AcademicoErrorCodes.RANGO_FECHAS_INVALIDO, "Rango inválido"));

        mockMvc.perform(get(RUTA + "/disponibles").param("filter[fechaDesde]", "2026-11-02")
                .with(comoEstudiante()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ACA_13"));
    }

    @Test
    void disponibles_conUnaFechaOUnInstructorMalFormados_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(RUTA + "/disponibles").param("filter[fecha]", "mañana").with(comoEstudiante()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(get(RUTA + "/disponibles").param("filter[instructor]", "no-es-uuid")
                .with(comoEstudiante()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(disponibleService);
    }

    @Test
    void disponibles_siIdentidadNoResponde_responde503ErrAca17() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(disponibleService.listar(any(), any(), any(), any(), any(), any(), any(), eq(1), eq(20)))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "No"));

        mockMvc.perform(get(RUTA + "/disponibles").with(comoEstudiante()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("ERR_ACA_17"));
    }

    @Test
    void disponibles_sinSesion_responde401() throws Exception {
        mockMvc.perform(get(RUTA + "/disponibles")).andExpect(status().isUnauthorized());
        verifyNoInteractions(disponibleService);
    }

    // ---------------------------------------------------------------- GET /franjas/instructores

    @Test
    void instructores_responde200ConLaListaYSuMeta() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(disponibleService.instructores(escuela, BEARER, FECHA, FECHA)).thenReturn(List.of(
            new InstructorDisponibleResponse(instructor, "Ivan Alfa")));

        mockMvc.perform(get(RUTA + "/instructores").param("filter[fecha]", "2026-11-02").with(comoEstudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].codigo").value(instructor.toString()))
            .andExpect(jsonPath("$.data[0].nombre").value("Ivan Alfa"))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void instructores_sinNinguno_respondeListaVaciaConMeta() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(disponibleService.instructores(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get(RUTA + "/instructores").with(comoEstudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.meta.totalItems").value(0));
    }
}
