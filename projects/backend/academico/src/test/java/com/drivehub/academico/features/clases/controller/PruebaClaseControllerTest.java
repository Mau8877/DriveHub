package com.drivehub.academico.features.clases.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.clases.dto.CatalogoPruebasResponse;
import com.drivehub.academico.features.clases.dto.EvaluarClaseRequest;
import com.drivehub.academico.features.clases.dto.PruebaClaseResponse;
import com.drivehub.academico.features.clases.dto.PuntajeDeCriterio;
import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.model.CriterioEvaluacion;
import com.drivehub.academico.features.clases.service.PruebaClaseService;

@WebMvcTest(PruebaClaseController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class PruebaClaseControllerTest {

    private static final String PRUEBAS = "/api/v1/pruebas-clase";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PruebaClaseService service;

    private final UUID instructor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID clase = UUID.randomUUID();
    private final UUID codigoPrueba = UUID.randomUUID();

    private RequestPostProcessor comoInstructor() {
        return jwt().jwt(j -> j.subject(instructor.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
    }

    private PruebaClaseResponse prueba() {
        return new PruebaClaseResponse(codigoPrueba, clase, TipoPrueba.MANEJO,
            List.of(new CriterioEvaluacion("espejos", "Posición y uso de espejos", 8, 10)), 80.0,
            ResultadoEvaluacion.APROBADO, "Bien", FECHA);
    }

    private String cuerpo(String extra) {
        return "{\"claseCodigo\":\"" + clase + "\",\"tipo\":\"MANEJO\",\"criterios\":"
            + "[{\"codigo\":\"espejos\",\"puntaje\":8}]" + extra + "}";
    }

    // ---------------------------------------------------------------- POST /pruebas-clase

    @Test
    void evaluar_responde201ConLaPruebaYSinIdsInternos() throws Exception {
        when(service.evaluar(eq(instructor), eq(escuela), any(EvaluarClaseRequest.class))).thenReturn(prueba());

        mockMvc.perform(post(PRUEBAS).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(",\"observaciones\":\"Bien\"")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Prueba de clase registrada correctamente"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.codigo").value(codigoPrueba.toString()))
            .andExpect(jsonPath("$.data.claseCodigo").value(clase.toString()))
            .andExpect(jsonPath("$.data.tipo").value("MANEJO"))
            .andExpect(jsonPath("$.data.criterios[0].codigo").value("espejos"))
            .andExpect(jsonPath("$.data.criterios[0].puntaje").value(8.0))
            .andExpect(jsonPath("$.data.criterios[0].maximo").value(10.0))
            .andExpect(jsonPath("$.data.puntajeTotal").value(80.0))
            .andExpect(jsonPath("$.data.resultado").value("APROBADO"))
            .andExpect(jsonPath("$.data.observaciones").value("Bien"))
            .andExpect(jsonPath("$.data.fecha").value("2026-11-02"))
            .andExpect(jsonPath("$.data.id").doesNotExist())
            .andExpect(jsonPath("$.data.claseId").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void evaluar_lePasaAlServicioLaClaseElTipoYLosCriterios() throws Exception {
        when(service.evaluar(any(), any(), any())).thenReturn(prueba());

        mockMvc.perform(post(PRUEBAS).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("")))
            .andExpect(status().isCreated());

        ArgumentCaptor<EvaluarClaseRequest> pedido = ArgumentCaptor.forClass(EvaluarClaseRequest.class);
        verify(service).evaluar(eq(instructor), eq(escuela), pedido.capture());
        assertThat(pedido.getValue()).isEqualTo(new EvaluarClaseRequest(clase, TipoPrueba.MANEJO,
            List.of(new PuntajeDeCriterio("espejos", 8.0)), null));
    }

    @Test
    void evaluar_conErroresDeNegocio_responde404409400y403ConSuCodigo() throws Exception {
        when(service.evaluar(any(), any(), any()))
            .thenThrow(new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA, "No encontramos"))
            .thenThrow(new ConflictException(AcademicoErrorCodes.CLASE_NO_EVALUABLE, "No se puede evaluar"))
            .thenThrow(new ConflictException(AcademicoErrorCodes.PRUEBA_YA_REGISTRADA, "Ya existe"))
            .thenThrow(new ValidationException(AcademicoErrorCodes.DATOS_DE_PRUEBA_INVALIDOS, "Datos inválidos"))
            .thenThrow(new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Sin escuela"));

        String[][] esperado = {{"404", "ERR_ACA_35"}, {"409", "ERR_ACA_39"}, {"409", "ERR_ACA_40"},
            {"400", "ERR_ACA_41"}, {"403", "ERR_ACA_16"}};
        for (String[] e : esperado) {
            mockMvc.perform(post(PRUEBAS).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo("")))
                .andExpect(status().is(Integer.parseInt(e[0])))
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value(e[1]));
        }
    }

    @Test
    void evaluar_conUnCuerpoInvalido_responde400ErrSys01SinLlamarAlServicio() throws Exception {
        String[] cuerpos = {
            "{\"tipo\":\"MANEJO\",\"criterios\":[{\"codigo\":\"espejos\",\"puntaje\":8}]}",
            "{\"claseCodigo\":\"" + clase + "\",\"criterios\":[{\"codigo\":\"espejos\",\"puntaje\":8}]}",
            "{\"claseCodigo\":\"" + clase + "\",\"tipo\":\"MANEJO\"}",
            "{\"claseCodigo\":\"" + clase + "\",\"tipo\":\"MANEJO\",\"criterios\":[]}",
            "{\"claseCodigo\":\"" + clase + "\",\"tipo\":\"MANEJO\",\"criterios\":[{\"codigo\":\"espejos\"}]}",
            "{\"claseCodigo\":\"" + clase + "\",\"tipo\":\"OTRO\",\"criterios\":[{\"codigo\":\"a\",\"puntaje\":1}]}",
            cuerpo(",\"observaciones\":\"" + "a".repeat(501) + "\"")};
        for (String cuerpo : cuerpos) {
            mockMvc.perform(post(PRUEBAS).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void evaluar_sinSesion_responde401() throws Exception {
        mockMvc.perform(post(PRUEBAS).contentType(MediaType.APPLICATION_JSON).content(cuerpo("")))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- GET /pruebas-clase

    @Test
    void listar_responde200ConLasPruebasYSuMeta() throws Exception {
        when(service.listarPorClase(instructor, escuela, clase, null, null))
            .thenReturn(new PageImpl<>(List.of(prueba()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(PRUEBAS).param("filter[claseCodigo]", clase.toString()).with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Listado obtenido correctamente"))
            .andExpect(jsonPath("$.data[0].codigo").value(codigoPrueba.toString()))
            .andExpect(jsonPath("$.data[0].resultado").value("APROBADO"))
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.pageSize").value(20))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void listar_lePasaAlServicioLaPaginacion() throws Exception {
        when(service.listarPorClase(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get(PRUEBAS).param("filter[claseCodigo]", clase.toString()).param("page", "2")
                .param("pageSize", "5").with(comoInstructor()))
            .andExpect(status().isOk());

        verify(service).listarPorClase(instructor, escuela, clase, 2, 5);
    }

    @Test
    void listar_sinElFiltroOConUnCodigoInvalido_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(PRUEBAS).with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(get(PRUEBAS).param("filter[claseCodigo]", "no-es-un-uuid").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(service);
    }

    @Test
    void listar_deUnaClaseAjena_responde404ConERR_ACA_35() throws Exception {
        when(service.listarPorClase(any(), any(), any(), any(), any())).thenThrow(
            new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA, "No encontramos esa clase"));

        mockMvc.perform(get(PRUEBAS).param("filter[claseCodigo]", clase.toString()).with(comoInstructor()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ACA_35"));
    }

    @Test
    void listar_sinSesion_responde401() throws Exception {
        mockMvc.perform(get(PRUEBAS).param("filter[claseCodigo]", clase.toString()))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- GET /pruebas-clase/criterios

    @Test
    void criterios_responde200ConLosTresTipos() throws Exception {
        when(service.criterios()).thenReturn(CatalogoPruebasResponse.actual());

        mockMvc.perform(get(PRUEBAS + "/criterios").with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Criterios obtenidos correctamente"))
            .andExpect(jsonPath("$.data.tipos.length()").value(3))
            .andExpect(jsonPath("$.data.tipos[0].tipo").value("MANEJO"))
            .andExpect(jsonPath("$.data.tipos[0].etiqueta").value("Manejo"))
            .andExpect(jsonPath("$.data.tipos[0].criterios.length()").value(5))
            .andExpect(jsonPath("$.data.tipos[0].criterios[0].codigo").value("espejos"))
            .andExpect(jsonPath("$.data.tipos[0].criterios[0].maximo").value(10.0));
    }

    @Test
    void criterios_sinSesion_responde401() throws Exception {
        mockMvc.perform(get(PRUEBAS + "/criterios")).andExpect(status().isUnauthorized());
    }
}
