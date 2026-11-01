package com.drivehub.academico.features.simulacros.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.simulacros.dto.IntentoIniciadoResponse;
import com.drivehub.academico.features.simulacros.dto.IntentoResultadoResponse;
import com.drivehub.academico.features.simulacros.dto.IntentoResumenResponse;
import com.drivehub.academico.features.simulacros.dto.SimulacroDisponibleResponse;
import com.drivehub.academico.features.simulacros.enums.EstadoIntento;
import com.drivehub.academico.features.simulacros.service.IntentoSimulacroService;

@WebMvcTest(IntentoSimulacroController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class IntentoSimulacroControllerTest {

    private static final String INTENTOS = "/api/v1/intentos-simulacro";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntentoSimulacroService service;

    private final UUID estudiante = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID simulacro = UUID.randomUUID();
    private final UUID intento = UUID.randomUUID();
    private final UUID pregunta = UUID.randomUUID();

    private RequestPostProcessor comoEstudiante() {
        return jwt().jwt(j -> j.subject(estudiante.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"));
    }

    private IntentoResultadoResponse resultado() {
        return new IntentoResultadoResponse(intento, "Básico", EstadoIntento.FINALIZADO,
            Instant.parse("2026-11-02T16:00:00Z"), Instant.parse("2026-11-02T16:10:00Z"), 1, 2, 50.0, false, 70.0,
            List.of(new IntentoResultadoResponse.Correccion(pregunta, "¿PARE?", TipoPregunta.OPCION_UNICA,
                List.of(new IntentoResultadoResponse.OpcionCorregida("A", "Detenerse", true)), List.of("A"), true,
                "Se detiene")));
    }

    @Test
    void todosLosEndpoints_sinToken_responden401() throws Exception {
        mockMvc.perform(get("/api/v1/simulacros/disponibles")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(INTENTOS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(INTENTOS).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void disponibles_devuelveLosSimulacrosDeLaEscuelaDelToken() throws Exception {
        when(service.disponibles(escuela)).thenReturn(
            List.of(new SimulacroDisponibleResponse(simulacro, "Básico", "B", 20, 5, 70.0)));

        mockMvc.perform(get("/api/v1/simulacros/disponibles").with(comoEstudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].codigo").value(simulacro.toString()))
            .andExpect(jsonPath("$.data[0].nroPreguntas").value(5))
            .andExpect(jsonPath("$.data[0].reglas").doesNotExist());
    }

    @Test
    void disponibles_conTokenSinEscuela_responde403ErrAca16() throws Exception {
        mockMvc.perform(get("/api/v1/simulacros/disponibles")
                .with(jwt().jwt(j -> j.subject(estudiante.toString())).authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(AcademicoErrorCodes.TOKEN_SIN_ESCUELA));
    }

    @Test
    void iniciar_responde201ConLasPreguntasSinLaRespuestaCorrecta() throws Exception {
        when(service.iniciar(escuela, estudiante, simulacro)).thenReturn(new IntentoIniciadoResponse(intento, "Básico",
            20, Instant.parse("2026-11-02T16:00:00Z"), Instant.parse("2026-11-02T16:20:00Z"),
            List.of(new IntentoIniciadoResponse.PreguntaParaRendir(pregunta, "¿PARE?", TipoPregunta.OPCION_UNICA,
                List.of(new IntentoIniciadoResponse.OpcionParaRendir("A", "Detenerse"))))));

        mockMvc.perform(post(INTENTOS).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"simulacroCodigo\":\"" + simulacro + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.codigo").value(intento.toString()))
            .andExpect(jsonPath("$.data.preguntas[0].opciones[0].texto").value("Detenerse"))
            .andExpect(jsonPath("$.data.preguntas[0].opciones[0].correcta").doesNotExist())
            .andExpect(jsonPath("$.data.preguntas[0].explicacion").doesNotExist());
    }

    @Test
    void iniciar_sinSimulacroOConUnoEnCurso_responde400Y409() throws Exception {
        mockMvc.perform(post(INTENTOS).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(service, never()).iniciar(any(), any(), any());

        when(service.iniciar(escuela, estudiante, simulacro))
            .thenThrow(new ConflictException(AcademicoErrorCodes.INTENTO_EN_CURSO, "en curso"));
        mockMvc.perform(post(INTENTOS).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"simulacroCodigo\":\"" + simulacro + "\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACA_29"));
    }

    @Test
    void finalizar_devuelveLaCorreccionYValidaElCuerpo() throws Exception {
        when(service.finalizar(any(), any(), any())).thenReturn(resultado());

        mockMvc.perform(post(INTENTOS + "/" + intento + "/finalizar").with(comoEstudiante())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"respuestas\":[{\"preguntaCodigo\":\"" + pregunta + "\",\"elegidas\":[\"A\"]}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.puntaje").value(50.0))
            .andExpect(jsonPath("$.data.preguntas[0].opciones[0].correcta").value(true))
            .andExpect(jsonPath("$.data.preguntas[0].explicacion").value("Se detiene"));
        mockMvc.perform(post(INTENTOS + "/" + intento + "/finalizar").with(comoEstudiante())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void listarYObtener_usanElEstudianteDelToken() throws Exception {
        when(service.listarMios(estudiante, 2, 10)).thenReturn(new PageImpl<>(List.of(new IntentoResumenResponse(intento,
            "Básico", EstadoIntento.FINALIZADO, Instant.parse("2026-11-02T16:00:00Z"),
            Instant.parse("2026-11-02T16:10:00Z"), 1, 2, 50.0, false)), PageRequest.of(1, 10), 11));
        when(service.obtener(estudiante, intento)).thenReturn(resultado());

        mockMvc.perform(get(INTENTOS).with(comoEstudiante()).param("page", "2").param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].codigo").value(intento.toString()))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.totalItems").value(11));
        mockMvc.perform(get(INTENTOS + "/" + intento).with(comoEstudiante()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.aciertos").value(1));
    }
}
