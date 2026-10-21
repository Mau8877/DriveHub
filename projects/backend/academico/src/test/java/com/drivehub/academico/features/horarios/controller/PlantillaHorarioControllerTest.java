package com.drivehub.academico.features.horarios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.horarios.dto.CrearPlantillaHorarioRequest;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioCreadaResponse;
import com.drivehub.academico.features.horarios.dto.PlantillaHorarioResponse;
import com.drivehub.academico.features.horarios.service.PlantillaHorarioService;

@WebMvcTest(PlantillaHorarioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class PlantillaHorarioControllerTest {

    private static final String RUTA = "/api/v1/plantillas-horario";
    private static final String CUERPO_VALIDO = """
        {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 60}
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlantillaHorarioService service;

    private final UUID instructor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();

    private RequestPostProcessor comoInstructor() {
        return jwt().jwt(j -> j.subject(instructor.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
    }

    private PlantillaHorarioResponse horario() {
        return new PlantillaHorarioResponse(UUID.randomUUID(), 1, "08:00", "12:00", 60, LocalDate.of(2026, 10, 26),
            null, true);
    }

    // ---------------------------------------------------------------- GET

    @Test
    void listar_responde200ConDataYMeta_yElServiceRecibeElSubDelToken() throws Exception {
        when(service.listar(eq(instructor), eq(true), eq(1), eq(20)))
            .thenReturn(new PageImpl<>(List.of(horario()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(RUTA).param("filter[activa]", "true").with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Horarios obtenidos correctamente"))
            .andExpect(jsonPath("$.data[0].horaInicio").value("08:00"))
            .andExpect(jsonPath("$.data[0].duracionClaseMin").value(60))
            .andExpect(jsonPath("$.data[0].vigenteDesde").value("2026-10-26"))
            .andExpect(jsonPath("$.data[0].activa").value(true))
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.pageSize").value(20))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void listar_sinFiltroNiPaginacion_usaLosDefaults() throws Exception {
        when(service.listar(any(), any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(RUTA).with(comoInstructor())).andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.meta.totalItems").value(0));

        verify(service).listar(instructor, null, 1, 20);
    }

    @Test
    void listar_conPaginacionPedida_laPasaAlService() throws Exception {
        when(service.listar(any(), any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get(RUTA).param("page", "3").param("pageSize", "5").with(comoInstructor()))
            .andExpect(status().isOk());

        verify(service).listar(instructor, null, 3, 5);
    }

    @Test
    void listar_filtroDeActivaQueNoEsBooleano_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(RUTA).param("filter[activa]", "quizas").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(service, never()).listar(any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void listar_paginaQueNoEsNumero_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(RUTA).param("page", "abc").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void listar_sinToken_responde401ErrSys04() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    // ---------------------------------------------------------------- POST crear

    @Test
    void crear_datosValidos_responde201ConElHorarioYLasFranjasGeneradas() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(service.crear(eq(instructor), eq(escuela), any(CrearPlantillaHorarioRequest.class)))
            .thenReturn(new PlantillaHorarioCreadaResponse(codigo, 1, "08:00", "12:00", 60,
                LocalDate.of(2026, 10, 26), null, true, 14));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO)
                .with(comoInstructor()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Horario creado correctamente"))
            .andExpect(jsonPath("$.data.codigo").value(codigo.toString()))
            .andExpect(jsonPath("$.data.franjasGeneradas").value(14))
            .andExpect(jsonPath("$.data.activa").value(true));

        ArgumentCaptor<CrearPlantillaHorarioRequest> pedido = ArgumentCaptor
            .forClass(CrearPlantillaHorarioRequest.class);
        verify(service).crear(eq(instructor), eq(escuela), pedido.capture());
        org.assertj.core.api.Assertions.assertThat(pedido.getValue().horaFin()).isEqualTo("12:00");
        org.assertj.core.api.Assertions.assertThat(pedido.getValue().vigenteDesde()).isNull();
    }

    @Test
    void crear_conVigencias_laFechaViajaEnFormatoIso() throws Exception {
        when(service.crear(any(), any(), any())).thenReturn(new PlantillaHorarioCreadaResponse(UUID.randomUUID(), 1,
            "08:00", "12:00", 60, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 12, 1), true, 4));
        String cuerpo = """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 60,
             "vigenteDesde": "2026-11-02", "vigenteHasta": "2026-12-01"}
            """;

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(comoInstructor()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.vigenteHasta").value("2026-12-01"));

        ArgumentCaptor<CrearPlantillaHorarioRequest> pedido = ArgumentCaptor
            .forClass(CrearPlantillaHorarioRequest.class);
        verify(service).crear(any(), any(), pedido.capture());
        org.assertj.core.api.Assertions.assertThat(pedido.getValue().vigenteDesde())
            .isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void crear_tokenSinClaimDeEscuela_elServiceRecibeNull() throws Exception {
        when(service.crear(any(), any(), any())).thenReturn(new PlantillaHorarioCreadaResponse(UUID.randomUUID(), 1,
            "08:00", "12:00", 60, LocalDate.of(2026, 10, 26), null, true, 0));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO)
                .with(jwt().jwt(j -> j.subject(instructor.toString()))
                    .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
            .andExpect(status().isCreated());

        verify(service).crear(eq(instructor), isNull(), any(CrearPlantillaHorarioRequest.class));
    }

    @Test
    void crear_diaFueraDeRango_responde400ErrSys01SinLlamarAlService() throws Exception {
        String cuerpo = CUERPO_VALIDO.replace("\"diaSemana\": 1", "\"diaSemana\": 9");

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
            .andExpect(jsonPath("$.message").value("El día de la semana debe estar entre 1 (lunes) y 7 (domingo)"));

        verify(service, never()).crear(any(), any(), any());
    }

    @Test
    void crear_horaConFormatoInvalido_responde400ErrSys01() throws Exception {
        String cuerpo = CUERPO_VALIDO.replace("\"08:00\"", "\"25:00\"");

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void crear_faltaLaDuracion_responde400ErrSys01() throws Exception {
        String cuerpo = """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00"}
            """;

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void crear_jsonMalformado_responde400ErrSys01EnVezDe500() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{no es json")
                .with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
            .andExpect(jsonPath("$.message").value("La solicitud no es válida"));
    }

    @Test
    void crear_fechaInvalidaEnElCuerpo_responde400ErrSys01() throws Exception {
        String cuerpo = """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 60,
             "vigenteDesde": "mañana"}
            """;

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void crear_elServiceLanzaCruce_responde409ConSuCodigo() throws Exception {
        when(service.crear(any(), any(), any())).thenThrow(
            new ConflictException("ERR_ACA_04", "El horario se cruza con otro horario activo tuyo"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO)
                .with(comoInstructor()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ACA_04"))
            .andExpect(jsonPath("$.message").value("El horario se cruza con otro horario activo tuyo"))
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void crear_sinToken_responde401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    // ---------------------------------------------------------------- POST desactivar

    @Test
    void desactivar_responde200SinData() throws Exception {
        UUID codigo = UUID.randomUUID();

        mockMvc.perform(post(RUTA + "/" + codigo + "/desactivar").with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Horario desactivado correctamente"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(service).desactivar(instructor, codigo);
    }

    @Test
    void desactivar_horarioInexistente_responde404ErrAca05() throws Exception {
        UUID codigo = UUID.randomUUID();
        doThrow(new NotFoundException("ERR_ACA_05", "El horario no existe")).when(service)
            .desactivar(instructor, codigo);

        mockMvc.perform(post(RUTA + "/" + codigo + "/desactivar").with(comoInstructor()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ACA_05"));
    }

    @Test
    void desactivar_yaInactivo_responde409ErrAca06() throws Exception {
        UUID codigo = UUID.randomUUID();
        doThrow(new ConflictException("ERR_ACA_06", "El horario ya está desactivado")).when(service)
            .desactivar(instructor, codigo);

        mockMvc.perform(post(RUTA + "/" + codigo + "/desactivar").with(comoInstructor()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_06"));
    }

    @Test
    void desactivar_codigoQueNoEsUuid_responde400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA + "/no-es-un-uuid/desactivar").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(service, never()).desactivar(any(), any());
    }
}
