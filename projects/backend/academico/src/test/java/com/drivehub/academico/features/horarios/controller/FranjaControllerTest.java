package com.drivehub.academico.features.horarios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.horarios.dto.FranjaResponse;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.service.FranjaService;

@WebMvcTest(FranjaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class FranjaControllerTest {

    private static final String RUTA = "/api/v1/franjas";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FranjaService service;

    private final UUID instructor = UUID.randomUUID();

    private RequestPostProcessor comoInstructor() {
        return jwt().jwt(j -> j.subject(instructor.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
    }

    private FranjaResponse franja(String inicio, String fin, EstadoFranja estado) {
        return new FranjaResponse(UUID.randomUUID(), FECHA, inicio, fin, estado);
    }

    // ---------------------------------------------------------------- GET

    @Test
    void listar_porFecha_responde200ConLasFranjasYMeta() throws Exception {
        when(service.listar(eq(instructor), eq(FECHA), eq(null), eq(1), eq(20))).thenReturn(new PageImpl<>(
            List.of(franja("08:00", "09:00", EstadoFranja.LIBRE), franja("09:00", "10:00", EstadoFranja.BLOQUEADA)),
            PageRequest.of(0, 20), 2));

        mockMvc.perform(get(RUTA).param("filter[fecha]", "2026-11-02").with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Franjas obtenidas correctamente"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].horaInicio").value("08:00"))
            .andExpect(jsonPath("$.data[0].fecha").value("2026-11-02"))
            .andExpect(jsonPath("$.data[0].estado").value("LIBRE"))
            .andExpect(jsonPath("$.data[1].estado").value("BLOQUEADA"))
            .andExpect(jsonPath("$.meta.totalItems").value(2))
            .andExpect(jsonPath("$.meta.page").value(1));
    }

    @Test
    void listar_fechaSinFranjas_respondeListaVaciaNoError() throws Exception {
        when(service.listar(any(), any(), any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(RUTA).param("filter[fecha]", "2026-11-03").with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    @Test
    void listar_conFiltroDeEstadoYPaginacion_losPasaAlService() throws Exception {
        when(service.listar(any(), any(), any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get(RUTA).param("filter[estado]", "LIBRE").param("page", "2").param("pageSize", "10")
                .with(comoInstructor()))
            .andExpect(status().isOk());

        verify(service).listar(instructor, null, EstadoFranja.LIBRE, 2, 10);
    }

    @Test
    void listar_sinFiltros_usaLosDefaults() throws Exception {
        when(service.listar(any(), any(), any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(RUTA).with(comoInstructor())).andExpect(status().isOk());

        verify(service).listar(instructor, null, null, 1, 20);
    }

    @Test
    void listar_fechaQueNoEsFecha_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(RUTA).param("filter[fecha]", "abc").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(service, never()).listar(any(), any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void listar_fechaConFormatoQueNoEsIso_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(RUTA).param("filter[fecha]", "02/11/2026").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void listar_estadoQueNoExiste_responde400ErrSys01() throws Exception {
        mockMvc.perform(get(RUTA).param("filter[estado]", "XYZ").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void listar_sinToken_responde401ErrSys04() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    // ---------------------------------------------------------------- bloquear / desbloquear

    @Test
    void bloquear_responde200ConLaFranjaBloqueada() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(service.bloquear(instructor, codigo))
            .thenReturn(new FranjaResponse(codigo, FECHA, "08:00", "09:00", EstadoFranja.BLOQUEADA));

        mockMvc.perform(post(RUTA + "/" + codigo + "/bloquear").with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Franja bloqueada correctamente"))
            .andExpect(jsonPath("$.data.codigo").value(codigo.toString()))
            .andExpect(jsonPath("$.data.estado").value("BLOQUEADA"));
    }

    @Test
    void bloquear_franjaNoLibre_responde409ErrAca08() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(service.bloquear(instructor, codigo)).thenThrow(
            new ConflictException("ERR_ACA_08", "La franja no está libre: solo se bloquean las libres"));

        mockMvc.perform(post(RUTA + "/" + codigo + "/bloquear").with(comoInstructor()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ACA_08"))
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bloquear_franjaInexistente_responde404ErrAca07() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(service.bloquear(instructor, codigo))
            .thenThrow(new NotFoundException("ERR_ACA_07", "La franja no existe"));

        mockMvc.perform(post(RUTA + "/" + codigo + "/bloquear").with(comoInstructor()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ACA_07"));
    }

    @Test
    void bloquear_franjaQueYaEmpezo_responde409ErrAca10() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(service.bloquear(instructor, codigo))
            .thenThrow(new ConflictException("ERR_ACA_10", "La franja ya empezó"));

        mockMvc.perform(post(RUTA + "/" + codigo + "/bloquear").with(comoInstructor()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_10"));
    }

    @Test
    void bloquear_codigoQueNoEsUuid_responde400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA + "/123/bloquear").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(service, never()).bloquear(any(), any());
    }

    @Test
    void desbloquear_responde200ConLaFranjaLibre() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(service.desbloquear(instructor, codigo))
            .thenReturn(new FranjaResponse(codigo, FECHA, "08:00", "09:00", EstadoFranja.LIBRE));

        mockMvc.perform(post(RUTA + "/" + codigo + "/desbloquear").with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Franja desbloqueada correctamente"))
            .andExpect(jsonPath("$.data.estado").value("LIBRE"));
    }

    @Test
    void desbloquear_franjaNoBloqueada_responde409ErrAca09() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(service.desbloquear(instructor, codigo)).thenThrow(
            new ConflictException("ERR_ACA_09", "La franja no está bloqueada"));

        mockMvc.perform(post(RUTA + "/" + codigo + "/desbloquear").with(comoInstructor()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_09"));
    }

    @Test
    void accionesSinToken_responden401() throws Exception {
        UUID codigo = UUID.randomUUID();

        mockMvc.perform(post(RUTA + "/" + codigo + "/bloquear")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA + "/" + codigo + "/desbloquear")).andExpect(status().isUnauthorized());
    }
}
