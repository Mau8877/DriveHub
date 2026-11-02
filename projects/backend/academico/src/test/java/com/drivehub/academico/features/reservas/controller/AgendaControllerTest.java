package com.drivehub.academico.features.reservas.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.reservas.dto.CancelacionInstructorResponse;
import com.drivehub.academico.features.reservas.dto.ClaseAgendaResponse;
import com.drivehub.academico.features.reservas.dto.HoraDeCancelacion;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.service.AgendaService;

@WebMvcTest(AgendaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class AgendaControllerTest {

    private static final String AGENDA = "/api/v1/reservas/agenda";
    private static final String BEARER = "Bearer token-del-instructor";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgendaService service;
    @MockitoBean
    private BearerActual bearerActual;

    private final UUID instructor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();

    private RequestPostProcessor comoInstructor() {
        return jwt().jwt(j -> j.subject(instructor.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
    }

    private ClaseAgendaResponse clase(EstadoReserva estado, boolean cancelable) {
        return new ClaseAgendaResponse(UUID.randomUUID(), FECHA, "10:00", "11:00", estudiante, "Ana Pérez", estado,
            false, cancelable, !cancelable && estado == EstadoReserva.CONFIRMADA, null);
    }

    private String rutaDeCancelar(UUID codigo) {
        return "/api/v1/reservas/" + codigo + "/cancelar-por-instructor";
    }

    // ---------------------------------------------------------------- GET /reservas/agenda

    @Test
    void agenda_responde200ConLasClasesYElMeta() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.agenda(instructor, escuela, BEARER, null, 1, 20)).thenReturn(
            new PageImpl<>(List.of(clase(EstadoReserva.CONFIRMADA, true)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(AGENDA).with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Agenda obtenida correctamente"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].horaInicio").value("10:00"))
            .andExpect(jsonPath("$.data[0].horaFin").value("11:00"))
            .andExpect(jsonPath("$.data[0].estudianteNombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.data[0].estudianteCodigo").value(estudiante.toString()))
            .andExpect(jsonPath("$.data[0].estado").value("CONFIRMADA"))
            .andExpect(jsonPath("$.data[0].terminada").value(false))
            .andExpect(jsonPath("$.data[0].cancelable").value(true))
            .andExpect(jsonPath("$.data[0].cerrable").value(false))
            .andExpect(jsonPath("$.data[0].claseCodigo").doesNotExist())
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.pageSize").value(20))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void agenda_conFechaYPagina_losPasaAlServicio() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.agenda(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get(AGENDA).param("filter[fecha]", "2026-11-02").param("page", "2").param("pageSize", "10")
                .with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.meta.page").value(2));

        verify(service).agenda(instructor, escuela, BEARER, "2026-11-02", 2, 10);
    }

    @Test
    void agenda_conUnaFechaInvalida_responde400ErrSys01() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.agenda(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt()))
            .thenThrow(new ValidationException("ERR_SYS_01", "La fecha debe tener el formato aaaa-mm-dd"));

        mockMvc.perform(get(AGENDA).param("filter[fecha]", "mañana").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void agenda_conIdentidadCaida_responde503ErrAca17() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.agenda(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt()))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "No"));

        mockMvc.perform(get(AGENDA).with(comoInstructor()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("ERR_ACA_17"));
    }

    @Test
    void agenda_sinEscuelaEnElToken_responde403ErrAca16() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.agenda(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt()))
            .thenThrow(new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Sin escuela"));

        mockMvc.perform(get(AGENDA).with(jwt().jwt(j -> j.subject(instructor.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_ACA_16"));
        verify(service).agenda(instructor, null, BEARER, null, 1, 20);
    }

    @Test
    void agenda_sinSesion_responde401() throws Exception {
        mockMvc.perform(get(AGENDA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- POST /reservas/{codigo}/cancelar-por-instructor

    @Test
    void cancelar_responde200ConLaClaseCanceladaYLaHoraDevuelta() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(instructor, escuela, BEARER, codigo)).thenReturn(new CancelacionInstructorResponse(
            clase(EstadoReserva.CANCELADA, false), HoraDeCancelacion.DEVUELTA));

        mockMvc.perform(post(rutaDeCancelar(codigo)).with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Clase cancelada correctamente"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.clase.estado").value("CANCELADA"))
            .andExpect(jsonPath("$.data.clase.cancelable").value(false))
            .andExpect(jsonPath("$.data.clase.estudianteNombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.data.clase.id").doesNotExist())
            .andExpect(jsonPath("$.data.horaDevuelta").value("DEVUELTA"))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void cancelar_conLaHoraEnProceso_responde200ConEnProceso() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(instructor, escuela, BEARER, codigo)).thenReturn(new CancelacionInstructorResponse(
            clase(EstadoReserva.CANCELADA, false), HoraDeCancelacion.EN_PROCESO));

        mockMvc.perform(post(rutaDeCancelar(codigo)).with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.horaDevuelta").value("EN_PROCESO"));
    }

    @Test
    void cancelar_unaClaseQueNoExiste_responde404ConERR_ACA_35() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(instructor, escuela, BEARER, codigo)).thenThrow(new NotFoundException(
            AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA, "No encontramos esa clase"));

        mockMvc.perform(post(rutaDeCancelar(codigo)).with(comoInstructor()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ACA_35"));
    }

    @Test
    void cancelar_unaClaseNoCancelable_responde409ConERR_ACA_36() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(instructor, escuela, BEARER, codigo)).thenThrow(new ConflictException(
            AcademicoErrorCodes.CLASE_NO_CANCELABLE_POR_INSTRUCTOR, "Esa clase ya no se puede cancelar"));

        mockMvc.perform(post(rutaDeCancelar(codigo)).with(comoInstructor()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_36"));
    }

    @Test
    void cancelar_sinEscuelaEnElToken_responde403ErrAca16() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(any(), any(), any(), any()))
            .thenThrow(new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Sin escuela"));

        mockMvc.perform(post(rutaDeCancelar(codigo)).with(jwt().jwt(j -> j.subject(instructor.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_ACA_16"));
    }

    @Test
    void cancelar_conUnCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(post("/api/v1/reservas/no-es-un-uuid/cancelar-por-instructor").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(service);
    }

    @Test
    void cancelar_sinSesion_responde401() throws Exception {
        mockMvc.perform(post(rutaDeCancelar(UUID.randomUUID()))).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
