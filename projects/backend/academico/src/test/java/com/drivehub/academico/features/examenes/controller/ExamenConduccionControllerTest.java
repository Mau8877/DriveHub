package com.drivehub.academico.features.examenes.controller;

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
import java.time.LocalDate;
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
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.features.examenes.dto.ExamenResponse;
import com.drivehub.academico.features.examenes.dto.ProgramarExamenRequest;
import com.drivehub.academico.features.examenes.enums.EstadoExamen;
import com.drivehub.academico.features.examenes.repository.FiltroExamenes;
import com.drivehub.academico.features.examenes.service.ExamenConduccionService;

@WebMvcTest(ExamenConduccionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ExamenConduccionControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final String RUTA = "/api/v1/examenes-conduccion";
    private static final String BEARER = "Bearer token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamenConduccionService examenService;
    @MockitoBean
    private BearerActual bearerActual;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(AUTOR.toString()).claim("escuela", ESCUELA.toString()));
    }

    private static ExamenResponse examen(EstadoExamen estado) {
        return new ExamenResponse(CODIGO, ESTUDIANTE, "Ana Pérez", "B", Instant.parse("2026-11-10T14:00:00Z"),
            LocalDate.of(2026, 11, 10), "10:00", UUID.randomUUID(), UUID.randomUUID(), 1, estado, null, null, null);
    }

    private static String cuerpo() {
        return """
            {"estudianteCodigo":"%s","categoria":"B","fecha":"2026-11-10","hora":"10:00",
             "evaluadorCodigo":"%s","vehiculoCodigo":"%s"}
            """.formatted(ESTUDIANTE, UUID.randomUUID(), UUID.randomUUID());
    }

    private ResultActions programar(String json) throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
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
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/cancelar")).andExpect(status().isUnauthorized());
    }

    @Test
    void programar_conDatosValidos_responde201TomandoEscuelaAutorYBearerDelToken() throws Exception {
        when(examenService.programar(any(), any(), any(), any())).thenReturn(examen(EstadoExamen.PROGRAMADO));

        programar(cuerpo())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.data.estudianteNombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.data.estado").value("PROGRAMADO"))
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<ProgramarExamenRequest> request = ArgumentCaptor.forClass(ProgramarExamenRequest.class);
        verify(examenService).programar(eq(ESCUELA), eq(AUTOR), eq(BEARER), request.capture());
        assertThat(request.getValue().hora()).isEqualTo("10:00");
    }

    @Test
    void programar_conCamposInvalidos_responde400ErrSys01SinLlamarAlService() throws Exception {
        String[][] cambios = {
            {"\"categoria\":\"B\"", "\"categoria\":\"b\""},
            {"\"fecha\":\"2026-11-10\"", "\"fecha\":\"no-es-fecha\""},
            {"\"hora\":\"10:00\"", "\"hora\":\"25:00\""},
            {"\"hora\":\"10:00\"", "\"hora\":\"\""},
            {"\"estudianteCodigo\":\"" + ESTUDIANTE + "\"", "\"estudianteCodigo\":null"},
        };
        for (String[] cambio : cambios) {
            programar(cuerpo().replace(cambio[0], cambio[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        programar("esto no es json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(examenService, never()).programar(any(), any(), any(), any());
    }

    @Test
    void programar_conTokenSinEscuela_responde403ErrAca16() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(AUTOR.toString())))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(AcademicoErrorCodes.TOKEN_SIN_ESCUELA));
    }

    @Test
    void listar_devuelveArrayConMetaYPasaLosFiltros() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(examenService.listar(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(examen(EstadoExamen.PROGRAMADO)), PageRequest.of(1, 10), 11));

        mockMvc.perform(get(RUTA).with(sesion()).param("page", "2").param("pageSize", "10")
                .param("filter[categoria]", "B").param("filter[estado]", "PROGRAMADO")
                .param("filter[estudianteCodigo]", ESTUDIANTE.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.totalItems").value(11));

        verify(examenService).listar(ESCUELA, BEARER, new FiltroExamenes("B", EstadoExamen.PROGRAMADO, ESTUDIANTE), 2, 10);
    }

    @Test
    void listar_conFiltroInvalido_responde400() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        mockMvc.perform(get(RUTA).with(sesion()).param("filter[estado]", "QUIZAS"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void obtener_conExamenInexistente_responde404ErrAca24() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(examenService.obtener(ESCUELA, BEARER, CODIGO))
            .thenThrow(new NotFoundException(AcademicoErrorCodes.EXAMEN_NO_ENCONTRADO, "no existe"));

        mockMvc.perform(get(RUTA + "/" + CODIGO).with(sesion()))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACA_24"));
    }

    @Test
    void reprogramar_responde200YValidaLaHora() throws Exception {
        when(examenService.reprogramar(eq(ESCUELA), eq(CODIGO), any())).thenReturn(examen(EstadoExamen.PROGRAMADO));

        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"hora\":\"11:30\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()));
        mockMvc.perform(patch(RUTA + "/" + CODIGO).with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"hora\":\"99:99\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void cancelarYNoAsistio_respondenConElExamenYUnConflictoSale409() throws Exception {
        when(examenService.cancelar(ESCUELA, CODIGO)).thenReturn(examen(EstadoExamen.CANCELADO));
        when(examenService.marcarNoAsistio(ESCUELA, CODIGO))
            .thenThrow(new ConflictException(AcademicoErrorCodes.EXAMEN_NO_ESTA_PROGRAMADO, "ya no"));

        mockMvc.perform(post(RUTA + "/" + CODIGO + "/cancelar").with(sesion()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("CANCELADO"));
        mockMvc.perform(post(RUTA + "/" + CODIGO + "/no-asistio").with(sesion()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACA_25"));
    }

    @Test
    void registrarResultado_validaElPuntajeYResponde200() throws Exception {
        when(examenService.registrarResultado(eq(ESCUELA), eq(CODIGO), any())).thenReturn(examen(EstadoExamen.REALIZADO));

        mockMvc.perform(post(RUTA + "/" + CODIGO + "/registrar-resultado").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntaje\":85,\"observaciones\":\"ok\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("REALIZADO"));
        for (String malo : List.of("{\"puntaje\":101}", "{\"puntaje\":-1}", "{}")) {
            mockMvc.perform(post(RUTA + "/" + CODIGO + "/registrar-resultado").with(sesion())
                    .contentType(MediaType.APPLICATION_JSON).content(malo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
    }
}
