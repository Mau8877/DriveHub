package com.drivehub.academico.features.resultados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.resultados.dto.ResultadoResponse;
import com.drivehub.academico.features.resultados.enums.OrigenResultado;
import com.drivehub.academico.features.resultados.repository.FiltroResultados;
import com.drivehub.academico.features.resultados.service.ResultadoService;

@WebMvcTest(ResultadoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class ResultadoControllerTest {

    private static final String RUTA = "/api/v1/resultados";
    private static final String BEARER = "Bearer token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultadoService service;
    @MockitoBean
    private BearerActual bearerActual;

    private final UUID persona = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();

    private RequestPostProcessor como(String rol) {
        return jwt().jwt(j -> j.subject(persona.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private ResultadoResponse nota() {
        return new ResultadoResponse(UUID.randomUUID(), estudiante, "Ana Pérez", OrigenResultado.EXAMEN_CONDUCCION,
            "Examen de conducción, categoría B (intento 1)", 85.0, true, Instant.parse("2026-11-03T02:00:00Z"),
            LocalDate.of(2026, 11, 2));
    }

    @Test
    void sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/mios")).andExpect(status().isUnauthorized());
    }

    @Test
    void mios_usaElEstudianteDelTokenYPasaElOrigen() throws Exception {
        when(service.listarMios(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(nota()), PageRequest.of(1, 10), 11));

        mockMvc.perform(get(RUTA + "/mios").with(como("ESTUDIANTE")).param("page", "2").param("pageSize", "10")
                .param("filter[origen]", "SIMULACRO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].titulo").value("Examen de conducción, categoría B (intento 1)"))
            .andExpect(jsonPath("$.data[0].aprobado").value(true))
            .andExpect(jsonPath("$.data[0].dia").value("2026-11-02"))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.totalItems").value(11));

        verify(service).listarMios(persona, new FiltroResultados(OrigenResultado.SIMULACRO, null), 2, 10);
    }

    @Test
    void deLaEscuela_usaLaEscuelaYElBearerDelTokenYLosFiltros() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.listarDeEscuela(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(nota()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(RUTA).with(como("ADMIN_ESCUELA")).param("filter[origen]", "EXAMEN_CONDUCCION")
                .param("filter[estudianteCodigo]", estudiante.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].estudianteNombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.data[0].id").doesNotExist());

        verify(service).listarDeEscuela(escuela, BEARER,
            new FiltroResultados(OrigenResultado.EXAMEN_CONDUCCION, estudiante), null, null);
    }

    @Test
    void deLaEscuela_sinEscuelaEnElToken_responde403ErrAca16() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(persona.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_ESCUELA"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(AcademicoErrorCodes.TOKEN_SIN_ESCUELA));
        verify(service, never()).listarDeEscuela(any(), any(), any(), any(), any());
    }

    @Test
    void deMisAlumnos_usaElBearerDelTokenYPasaLosFiltros() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.listarDeAlumnos(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(nota()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(RUTA + "/de-mis-alumnos").with(como("INSTRUCTOR")).param("filter[origen]", "SIMULACRO")
                .param("filter[estudianteCodigo]", estudiante.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].estudianteNombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.data[0].id").doesNotExist());

        verify(service).listarDeAlumnos(BEARER, new FiltroResultados(OrigenResultado.SIMULACRO, estudiante), null, null);
    }

    @Test
    void deMisAlumnos_cuandoEscuelaNoRespondeEsUn503() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.listarDeAlumnos(any(), any(), any(), any())).thenThrow(new ServicioNoDisponibleException(
            AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "No pudimos consultar tus alumnos."));

        mockMvc.perform(get(RUTA + "/de-mis-alumnos").with(como("INSTRUCTOR")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("ERR_ACA_17"));
    }

    @Test
    void deMisAlumnos_sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA + "/de-mis-alumnos")).andExpect(status().isUnauthorized());
    }

    @Test
    void conUnOrigenInvalido_responde400() throws Exception {
        mockMvc.perform(get(RUTA + "/mios").with(como("ESTUDIANTE")).param("filter[origen]", "QUIZAS"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }
}
