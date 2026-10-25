package com.drivehub.pagos.features.planes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.planes.controller.PlanPagoController;
import com.drivehub.pagos.features.planes.dto.CrearPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.EditarPlanPagoRequest;
import com.drivehub.pagos.features.planes.dto.PlanDisponibleResponse;
import com.drivehub.pagos.features.planes.dto.PlanPagoResponse;
import com.drivehub.pagos.features.planes.service.PlanPagoService;

/**
 * El contrato HTTP de los planes: status, forma del sobre y validación. El service va mockeado: su lógica y
 * el rol (`@PreAuthorize`) se prueban en sus tests y en el de integración.
 */
@WebMvcTest(PlanPagoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class PlanPagoControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();
    private static final String JSON_OK = "{\"nombre\":\"Plan 10\",\"horas\":10,\"costoBs\":450.00}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanPagoService servicio;

    private static JwtRequestPostProcessor conEscuela() {
        return jwt().jwt(j -> j.claim("escuela", ESCUELA.toString()));
    }

    private static PlanPagoResponse respuesta() {
        return new PlanPagoResponse(CODIGO, "Plan 10", 10, new BigDecimal("450.00"), new BigDecimal("38.23"), true,
            Instant.parse("2026-10-25T10:00:00Z"));
    }

    @Test
    void crear_responde201ConElPlanEnElSobre_yLaEscuelaSaleDelToken() throws Exception {
        when(servicio.crear(eq(ESCUELA), any(CrearPlanPagoRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/planes-pago").with(conEscuela()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON_OK))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.data.nombre").value("Plan 10"))
            .andExpect(jsonPath("$.data.horas").value(10))
            .andExpect(jsonPath("$.data.costoBs").value(450.00))
            .andExpect(jsonPath("$.data.costoUsd").value(38.23))
            .andExpect(jsonPath("$.data.activo").value(true))
            .andExpect(jsonPath("$.data.escuelaCodigo").doesNotExist())
            .andExpect(jsonPath("$.data.id").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void crear_ignoraUnaEscuelaEnElCuerpo_siempreUsaLaDelToken() throws Exception {
        when(servicio.crear(eq(ESCUELA), any(CrearPlanPagoRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/planes-pago").with(conEscuela()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Plan 10\",\"horas\":10,\"costoBs\":450.00,\"escuelaCodigo\":\""
                    + UUID.randomUUID() + "\"}"))
            .andExpect(status().isCreated());

        verify(servicio).crear(eq(ESCUELA), any(CrearPlanPagoRequest.class));
    }

    @Test
    void crear_conDatosInvalidos_responde400ConErrSys01_sinLlamarAlService() throws Exception {
        String[] invalidos = {
            "{\"nombre\":\"\",\"horas\":10,\"costoBs\":450}",
            "{\"nombre\":\"Plan\",\"horas\":0,\"costoBs\":450}",
            "{\"nombre\":\"Plan\",\"horas\":501,\"costoBs\":450}",
            "{\"nombre\":\"Plan\",\"costoBs\":450}",
            "{\"nombre\":\"Plan\",\"horas\":10,\"costoBs\":0}",
            "{\"nombre\":\"Plan\",\"horas\":10,\"costoBs\":-5}",
            "{\"nombre\":\"Plan\",\"horas\":10,\"costoBs\":10.123}",
            "{\"nombre\":\"Plan\",\"horas\":10}",
            "{\"nombre\":\"" + "x".repeat(81) + "\",\"horas\":10,\"costoBs\":450}",
        };
        for (String cuerpo : invalidos) {
            mockMvc.perform(post("/api/v1/planes-pago").with(conEscuela()).contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verifyNoInteractions(servicio);
    }

    @Test
    void crear_conUnCuerpoIlegible_responde400() throws Exception {
        mockMvc.perform(post("/api/v1/planes-pago").with(conEscuela()).contentType(MediaType.APPLICATION_JSON)
                .content("{ esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void crear_conUnNombreEnUso_respondeElErrorDelService() throws Exception {
        when(servicio.crear(eq(ESCUELA), any(CrearPlanPagoRequest.class)))
            .thenThrow(new ConflictException("ERR_PAG_01", "Ya existe un plan con ese nombre"));

        mockMvc.perform(post("/api/v1/planes-pago").with(conEscuela()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON_OK))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_01"));
    }

    @Test
    void listar_llevaLaPaginacionYLosFiltrosAlService_yRespondeConMeta() throws Exception {
        when(servicio.listar(ESCUELA, 2, 5, "plan", false))
            .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(1, 5), 11));

        mockMvc.perform(get("/api/v1/planes-pago").with(conEscuela())
                .param("page", "2").param("pageSize", "5").param("search", "plan").param("filter[activo]", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].nombre").value("Plan 10"))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(5))
            .andExpect(jsonPath("$.meta.totalItems").value(11))
            .andExpect(jsonPath("$.meta.totalPages").value(3));
    }

    @Test
    void listar_sinParametros_pasaNulosAlService() throws Exception {
        when(servicio.listar(ESCUELA, null, null, null, null))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/planes-pago").with(conEscuela()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    @Test
    void disponibles_respondeConElSobreYMeta_sinDatosInternos_yLaEscuelaSaleDelToken() throws Exception {
        when(servicio.disponibles(ESCUELA, 2, 5)).thenReturn(new PageImpl<>(
            List.of(new PlanDisponibleResponse(CODIGO, "Plan 10", (short) 10, new BigDecimal("450.00"),
                new BigDecimal("38.23"))), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/api/v1/planes-pago/disponibles").with(conEscuela())
                .param("page", "2").param("pageSize", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].codigo").value(CODIGO.toString()))
            .andExpect(jsonPath("$.data[0].horas").value(10))
            .andExpect(jsonPath("$.data[0].costoBs").value(450.00))
            .andExpect(jsonPath("$.data[0].costoUsd").value(38.23))
            .andExpect(jsonPath("$.data[0].activo").doesNotExist())
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.totalItems").value(6));
    }

    @Test
    void disponibles_noSeTomaComoElCodigoDeUnPlan() throws Exception {
        when(servicio.disponibles(ESCUELA, null, null))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/planes-pago/disponibles").with(conEscuela()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
        verify(servicio, never()).obtener(any(), any());
    }

    @Test
    void disponibles_sinEscuelaEnElToken_responde403ErrPag04() throws Exception {
        mockMvc.perform(get("/api/v1/planes-pago/disponibles").with(jwt()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_PAG_04"));
    }

    @Test
    void disponibles_sinSesion_responde401() throws Exception {
        mockMvc.perform(get("/api/v1/planes-pago/disponibles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listar_conUnFiltroInvalido_responde400() throws Exception {
        mockMvc.perform(get("/api/v1/planes-pago").with(conEscuela()).param("filter[activo]", "quizas"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void obtener_responde200_yUnCodigoQueNoEsUuidResponde400() throws Exception {
        when(servicio.obtener(ESCUELA, CODIGO)).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/planes-pago/" + CODIGO).with(conEscuela()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.codigo").value(CODIGO.toString()));
        mockMvc.perform(get("/api/v1/planes-pago/abc").with(conEscuela()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void obtener_deUnPlanQueNoExiste_responde404() throws Exception {
        when(servicio.obtener(ESCUELA, CODIGO)).thenThrow(new NotFoundException("ERR_PAG_02", "El plan no existe"));

        mockMvc.perform(get("/api/v1/planes-pago/" + CODIGO).with(conEscuela()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_PAG_02"));
    }

    @Test
    void editar_responde200_yValidaLosCamposQueLlegan() throws Exception {
        when(servicio.editar(eq(ESCUELA), eq(CODIGO), any(EditarPlanPagoRequest.class))).thenReturn(respuesta());

        mockMvc.perform(patch("/api/v1/planes-pago/" + CODIGO).with(conEscuela())
                .contentType(MediaType.APPLICATION_JSON).content("{\"horas\":20}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombre").value("Plan 10"));
        mockMvc.perform(patch("/api/v1/planes-pago/" + CODIGO).with(conEscuela())
                .contentType(MediaType.APPLICATION_JSON).content("{\"horas\":900}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void desactivarYActivar_responden200ConElPlan() throws Exception {
        when(servicio.desactivar(ESCUELA, CODIGO)).thenReturn(respuesta());
        when(servicio.activar(ESCUELA, CODIGO)).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/planes-pago/" + CODIGO + "/desactivar").with(conEscuela()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));
        mockMvc.perform(post("/api/v1/planes-pago/" + CODIGO + "/activar").with(conEscuela()))
            .andExpect(status().isOk());
    }

    @Test
    void desactivarUnPlanQueYaEstabaAsi_respondeElErrorDelService() throws Exception {
        when(servicio.desactivar(ESCUELA, CODIGO))
            .thenThrow(new ConflictException("ERR_PAG_03", "El plan ya está desactivado"));

        mockMvc.perform(post("/api/v1/planes-pago/" + CODIGO + "/desactivar").with(conEscuela()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_03"));
    }

    @Test
    void conUnTokenSinEscuela_cualquierEndpointResponde403ErrPag04_sinLlamarAlService() throws Exception {
        mockMvc.perform(get("/api/v1/planes-pago").with(jwt()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_PAG_04"));
        mockMvc.perform(post("/api/v1/planes-pago").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON_OK))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_PAG_04"));
        mockMvc.perform(post("/api/v1/planes-pago/" + CODIGO + "/activar").with(jwt()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_PAG_04"));

        verify(servicio, never()).listar(any(), any(), any(), any(), any());
        verify(servicio, never()).crear(any(), any());
        verify(servicio, never()).activar(any(), any());
    }

    @Test
    void sinToken_responde401ConErrSys04() throws Exception {
        mockMvc.perform(get("/api/v1/planes-pago"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
        verifyNoInteractions(servicio);
    }
}
