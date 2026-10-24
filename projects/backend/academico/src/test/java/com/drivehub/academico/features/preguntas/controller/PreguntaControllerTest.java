package com.drivehub.academico.features.preguntas.controller;

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
import com.drivehub.academico.features.preguntas.dto.CrearPreguntaRequest;
import com.drivehub.academico.features.preguntas.dto.PreguntaResponse;
import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.preguntas.repository.FiltroPreguntas;
import com.drivehub.academico.features.preguntas.service.PreguntaService;

@WebMvcTest(PreguntaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PreguntaControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/preguntas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreguntaService preguntaService;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(AUTOR.toString()).claim("escuela", ESCUELA.toString()));
    }

    private static PreguntaResponse pregunta(boolean activa) {
        return new PreguntaResponse(CODIGO, "GENERAL", "Señales", TipoPregunta.OPCION_UNICA, "¿Qué indica el PARE?",
            List.of(new PreguntaResponse.OpcionResponse("A", "Detenerse", true),
                new PreguntaResponse.OpcionResponse("B", "Seguir", false)), null, 2, activa,
            Instant.parse("2026-10-24T10:00:00Z"));
    }

    private static String cuerpo() {
        return """
            {"categoria":"GENERAL","tema":"Señales","tipo":"OPCION_UNICA","enunciado":"¿Qué indica el PARE?",
             "opciones":[{"texto":"Detenerse","correcta":true},{"texto":"Seguir","correcta":false}],
             "explicacion":"Se detiene","dificultad":2}
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
        when(preguntaService.crear(any(), any(), any())).thenReturn(pregunta(true));

        crear(cuerpo())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.data.opciones[0].id").value("A"))
            .andExpect(jsonPath("$.data.activa").value(true))
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<CrearPreguntaRequest> request = ArgumentCaptor.forClass(CrearPreguntaRequest.class);
        verify(preguntaService).crear(eq(ESCUELA), eq(AUTOR), request.capture());
        assertThat(request.getValue().tipo()).isEqualTo(TipoPregunta.OPCION_UNICA);
        assertThat(request.getValue().opciones()).hasSize(2);
    }

    @Test
    void crear_conCamposInvalidos_responde400ErrSys01SinLlamarAlService() throws Exception {
        String[][] cambios = {
            {"\"categoria\":\"GENERAL\"", "\"categoria\":\"general\""},
            {"\"tema\":\"Señales\"", "\"tema\":\"\""},
            {"\"tipo\":\"OPCION_UNICA\"", "\"tipo\":\"ADIVINAR\""},
            {"\"tipo\":\"OPCION_UNICA\"", "\"tipo\":null"},
            {"\"enunciado\":\"¿Qué indica el PARE?\"", "\"enunciado\":\" \""},
            {"\"dificultad\":2", "\"dificultad\":4"},
            {"\"dificultad\":2", "\"dificultad\":null"},
            {"{\"texto\":\"Seguir\",\"correcta\":false}", "{\"texto\":\"\",\"correcta\":false}"},
        };
        for (String[] cambio : cambios) {
            crear(cuerpo().replace(cambio[0], cambio[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        crear(cuerpo().replaceAll("\"opciones\":\\[.*?\\],", "\"opciones\":[],"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crear("esto no es json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(preguntaService, never()).crear(any(), any(), any());
    }

    @Test
    void crear_conTokenSinEscuela_responde403ErrAca03() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(AUTOR.toString())))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value(AcademicoErrorCodes.TOKEN_SIN_ESCUELA));
        verify(preguntaService, never()).crear(any(), any(), any());
    }

    @Test
    void listar_devuelveArrayConMetaYPasaLosFiltros() throws Exception {
        when(preguntaService.listar(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(pregunta(true)), PageRequest.of(1, 10), 11));

        mockMvc.perform(get(RUTA).with(sesion()).param("page", "2").param("pageSize", "10").param("search", "pare")
                .param("filter[categoria]", "B").param("filter[tema]", "Señales").param("filter[tipo]", "OPCION_UNICA")
                .param("filter[dificultad]", "2").param("filter[activa]", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(10))
            .andExpect(jsonPath("$.meta.totalItems").value(11))
            .andExpect(jsonPath("$.meta.totalPages").value(2));

        verify(preguntaService).listar(ESCUELA,
            new FiltroPreguntas("pare", "B", "Señales", TipoPregunta.OPCION_UNICA, 2, false), 2, 10);
    }

    @Test
    void listar_conFiltrosInvalidos_responde400() throws Exception {
        mockMvc.perform(get(RUTA).with(sesion()).param("filter[tipo]", "ADIVINAR"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(get(RUTA).with(sesion()).param("filter[activa]", "quizas"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void obtener_conPreguntaInexistente_responde404ErrAca01YUnCodigoMalFormado400() throws Exception {
        when(preguntaService.obtener(ESCUELA, CODIGO))
            .thenThrow(new NotFoundException(AcademicoErrorCodes.PREGUNTA_NO_ENCONTRADA, "no existe"));

        mockMvc.perform(get(RUTA + "/" + CODIGO).with(sesion()))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACA_14"));
        mockMvc.perform(get(RUTA + "/no-es-uuid").with(sesion()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void editar_responde200ConLaPreguntaYValidaElCuerpo() throws Exception {
        when(preguntaService.editar(eq(ESCUELA), eq(CODIGO), any())).thenReturn(pregunta(true));

        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tema\":\"Prioridad\",\"dificultad\":3}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()));
        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dificultad\":9}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void desactivarYActivar_llamanAlServiceConSuEstadoYUnConflictoSale409() throws Exception {
        when(preguntaService.cambiarActiva(ESCUELA, CODIGO, false)).thenReturn(pregunta(false));
        when(preguntaService.cambiarActiva(ESCUELA, CODIGO, true))
            .thenThrow(new ConflictException(AcademicoErrorCodes.PREGUNTA_YA_EN_ESE_ESTADO, "ya activa"));

        mockMvc.perform(post(RUTA + "/" + CODIGO + "/desactivar").with(sesion()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.activa").value(false));
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/activar").with(sesion()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACA_15"));
    }
}
