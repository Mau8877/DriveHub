package com.drivehub.academico.features.simulacros.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.features.simulacros.dto.CrearSimulacroRequest;
import com.drivehub.academico.features.simulacros.dto.SimulacroResponse;
import com.drivehub.academico.features.simulacros.repository.FiltroSimulacros;
import com.drivehub.academico.features.simulacros.service.SimulacroService;

@WebMvcTest(SimulacroController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SimulacroControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/simulacros";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimulacroService simulacroService;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(AUTOR.toString()).claim("escuela", ESCUELA.toString()));
    }

    private static SimulacroResponse simulacro(boolean activo) {
        return new SimulacroResponse(CODIGO, "Simulacro básico", "GENERAL", 20, 5, 70.0,
            List.of(new SimulacroResponse.ReglaResponse("Señales", 3),
                new SimulacroResponse.ReglaResponse("Prioridad", 2)),
            activo, Instant.parse("2026-10-28T10:00:00Z"));
    }

    private static String cuerpo() {
        return """
            {"nombre":"Simulacro básico","categoria":"GENERAL","duracionMin":20,"nroPreguntas":5,
             "puntajeAprobacion":70,"reglas":[{"tema":"Señales","cantidad":3},{"tema":"Prioridad","cantidad":2}]}
            """;
    }

    private ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void todosLosEndpoints_sinToken_responden401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/" + CODIGO)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(RUTA + "/" + CODIGO).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/desactivar")).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_conDatosValidos_responde201TomandoEscuelaYAutorDelToken() throws Exception {
        when(simulacroService.crear(any(), any(), any())).thenReturn(simulacro(true));

        crear(cuerpo())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.data.reglas[0].tema").value("Señales"))
            .andExpect(jsonPath("$.data.activo").value(true))
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<CrearSimulacroRequest> request = ArgumentCaptor.forClass(CrearSimulacroRequest.class);
        verify(simulacroService).crear(eq(ESCUELA), eq(AUTOR), request.capture());
        assertThat(request.getValue().reglas()).hasSize(2);
        assertThat(request.getValue().nroPreguntas()).isEqualTo(5);
    }

    @Test
    void crear_conCamposInvalidos_responde400ErrSys01SinLlamarAlService() throws Exception {
        String[][] cambios = {
            {"\"nombre\":\"Simulacro básico\"", "\"nombre\":\"\""},
            {"\"categoria\":\"GENERAL\"", "\"categoria\":\"general\""},
            {"\"duracionMin\":20", "\"duracionMin\":4"},
            {"\"nroPreguntas\":5", "\"nroPreguntas\":0"},
            {"\"puntajeAprobacion\":70", "\"puntajeAprobacion\":101"},
            {"\"puntajeAprobacion\":70", "\"puntajeAprobacion\":null"},
            {"{\"tema\":\"Prioridad\",\"cantidad\":2}", "{\"tema\":\"\",\"cantidad\":2}"},
            {"{\"tema\":\"Prioridad\",\"cantidad\":2}", "{\"tema\":\"Prioridad\",\"cantidad\":0}"},
        };
        for (String[] cambio : cambios) {
            crear(cuerpo().replace(cambio[0], cambio[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        crear(cuerpo().replaceAll("\"reglas\":\\[.*?\\]\\}", "\"reglas\":[]}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crear("esto no es json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(simulacroService, never()).crear(any(), any(), any());
    }

    @Test
    void crear_conTokenSinEscuela_responde403ErrAca16() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(AUTOR.toString())))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(AcademicoErrorCodes.TOKEN_SIN_ESCUELA));
        verify(simulacroService, never()).crear(any(), any(), any());
    }

    @Test
    void listar_devuelveArrayConMetaYPasaLosFiltros() throws Exception {
        when(simulacroService.listar(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(simulacro(true)), PageRequest.of(1, 10), 11));

        mockMvc.perform(get(RUTA).with(sesion()).param("page", "2").param("pageSize", "10").param("search", "básico")
                .param("filter[categoria]", "B").param("filter[activo]", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(10))
            .andExpect(jsonPath("$.meta.totalItems").value(11))
            .andExpect(jsonPath("$.meta.totalPages").value(2));

        verify(simulacroService).listar(ESCUELA, new FiltroSimulacros("básico", "B", false), 2, 10);
    }

    @Test
    void listar_conFiltroInvalido_responde400() throws Exception {
        mockMvc.perform(get(RUTA).with(sesion()).param("filter[activo]", "quizas"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void obtener_conSimulacroInexistente_responde404ErrAca21YUnCodigoMalFormado400() throws Exception {
        when(simulacroService.obtener(ESCUELA, CODIGO))
            .thenThrow(new NotFoundException(AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO, "no existe"));

        mockMvc.perform(get(RUTA + "/" + CODIGO).with(sesion()))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACA_21"));
        mockMvc.perform(get(RUTA + "/no-es-uuid").with(sesion()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void editar_responde200ConElSimulacroYValidaElCuerpo() throws Exception {
        when(simulacroService.editar(eq(ESCUELA), eq(CODIGO), any())).thenReturn(simulacro(true));

        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracionMin\":30}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()));
        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracionMin\":999}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void desactivarYActivar_llamanAlServiceConSuEstadoYUnConflictoSale409() throws Exception {
        when(simulacroService.cambiarActivo(ESCUELA, CODIGO, false)).thenReturn(simulacro(false));
        when(simulacroService.cambiarActivo(ESCUELA, CODIGO, true))
            .thenThrow(new ConflictException(AcademicoErrorCodes.SIMULACRO_YA_EN_ESE_ESTADO, "ya activo"));

        mockMvc.perform(post(RUTA + "/" + CODIGO + "/desactivar").with(sesion()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.activo").value(false));
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/activar").with(sesion()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACA_22"));
    }
}
