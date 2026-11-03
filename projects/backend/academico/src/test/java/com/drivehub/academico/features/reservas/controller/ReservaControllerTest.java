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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.reservas.dto.CancelacionResponse;
import com.drivehub.academico.features.reservas.dto.HoraDeCancelacion;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.features.reservas.dto.FranjaRechazadaResponse;
import com.drivehub.academico.features.reservas.dto.ReservaResponse;
import com.drivehub.academico.features.reservas.dto.ReservasResultadoResponse;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.service.ReservaService;

@WebMvcTest(ReservaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class ReservaControllerTest {

    private static final String RUTA = "/api/v1/reservas";
    private static final String BEARER = "Bearer token-del-estudiante";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService service;
    @MockitoBean
    private BearerActual bearerActual;

    private final UUID estudiante = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();
    private final UUID franjaA = UUID.randomUUID();
    private final UUID franjaB = UUID.randomUUID();

    private RequestPostProcessor comoEstudiante() {
        return jwt().jwt(j -> j.subject(estudiante.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"));
    }

    private static String cuerpo(UUID... franjas) {
        return "{\"franjaCodigos\":[" + java.util.Arrays.stream(franjas).map(f -> "\"" + f + "\"")
            .collect(Collectors.joining(",")) + "]}";
    }

    private ReservaResponse reserva() {
        return new ReservaResponse(UUID.randomUUID(), FECHA, "10:00", "11:00", instructor, "Ivan Alfa",
            EstadoReserva.CONFIRMADA, false, true);
    }

    @Test
    void reservar_conExitoParcial_responde200ConLasReservadasYLasRechazadas() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.reservar(estudiante, escuela, BEARER, List.of(franjaA, franjaB))).thenReturn(
            new ReservasResultadoResponse(List.of(reserva()), List.of(new FranjaRechazadaResponse(franjaB, FECHA,
                "11:00", "12:00", "Ivan Alfa", "ERR_ACA_18", "La franja ya no está disponible")), 4));

        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(franjaA, franjaB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Reservas procesadas correctamente"))
            .andExpect(jsonPath("$.data.reservadas.length()").value(1))
            .andExpect(jsonPath("$.data.reservadas[0].estado").value("CONFIRMADA"))
            .andExpect(jsonPath("$.data.reservadas[0].instructorNombre").value("Ivan Alfa"))
            .andExpect(jsonPath("$.data.reservadas[0].terminada").value(false))
            .andExpect(jsonPath("$.data.reservadas[0].id").doesNotExist())
            .andExpect(jsonPath("$.data.rechazadas.length()").value(1))
            .andExpect(jsonPath("$.data.rechazadas[0].franjaCodigo").value(franjaB.toString()))
            .andExpect(jsonPath("$.data.rechazadas[0].horaInicio").value("11:00"))
            .andExpect(jsonPath("$.data.rechazadas[0].error").value("ERR_ACA_18"))
            .andExpect(jsonPath("$.data.rechazadas[0].mensaje").value("La franja ya no está disponible"))
            .andExpect(jsonPath("$.data.horasDisponibles").value(4))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void reservar_siTodasSeRechazan_sigueSiendo200() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.reservar(any(), any(), any(), any())).thenReturn(new ReservasResultadoResponse(List.of(),
            List.of(new FranjaRechazadaResponse(franjaA, FECHA, "10:00", "11:00", "Ivan Alfa", "ERR_ACA_19",
                "No tienes horas suficientes")), 0));

        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(franjaA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reservadas.length()").value(0))
            .andExpect(jsonPath("$.data.rechazadas[0].error").value("ERR_ACA_19"))
            .andExpect(jsonPath("$.data.horasDisponibles").value(0));
    }

    @Test
    void reservar_conElSaldoDesconocido_devuelveHorasDisponiblesNulas() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.reservar(any(), any(), any(), any())).thenReturn(
            new ReservasResultadoResponse(List.of(reserva()), List.of(), null));

        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(franjaA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.horasDisponibles").doesNotExist());
    }

    @Test
    void reservar_conUnCuerpoInvalido_responde400ErrSys01SinLlamarAlServicio() throws Exception {
        String once = IntStream.range(0, 11).mapToObj(i -> "\"" + UUID.randomUUID() + "\"")
            .collect(Collectors.joining(","));

        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"franjaCodigos\":[]}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"franjaCodigos\":[" + once + "]}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"franjaCodigos\":[\"no-es-uuid\"]}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"franjaCodigos\":[null]}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA).with(comoEstudiante()).contentType(MediaType.APPLICATION_JSON).content("{no"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(service);
    }

    @Test
    void reservar_sinEscuelaEnElToken_responde403ErrAca16() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.reservar(any(), any(), any(), any()))
            .thenThrow(new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Sin escuela"));

        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(estudiante.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(franjaA)))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_ACA_16"));
        verify(service).reservar(estudiante, null, BEARER, List.of(franjaA));
    }

    // ---------------------------------------------------------------- GET /reservas

    @Test
    void listar_sinVista_pideLasProximasYResponde200ConMeta() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.listar(estudiante, BEARER, null, 1, 20)).thenReturn(
            new PageImpl<>(List.of(reserva()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(RUTA).with(comoEstudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Clases obtenidas correctamente"))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].horaInicio").value("10:00"))
            .andExpect(jsonPath("$.data[0].instructorNombre").value("Ivan Alfa"))
            .andExpect(jsonPath("$.data[0].estado").value("CONFIRMADA"))
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.pageSize").value(20))
            .andExpect(jsonPath("$.meta.totalItems").value(1));
    }

    @Test
    void listar_conVistaYPagina_losPasaAlServicio() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.listar(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get(RUTA).param("filter[vista]", "historial").param("page", "2").param("pageSize", "10")
                .with(comoEstudiante()))
            .andExpect(status().isOk());

        verify(service).listar(estudiante, BEARER, "historial", 2, 10);
    }

    @Test
    void listar_conUnaVistaInvalida_responde400ErrSys01() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.listar(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt())).thenThrow(new ValidationException("ERR_SYS_01", "Vista inválida"));

        mockMvc.perform(get(RUTA).param("filter[vista]", "futuras").with(comoEstudiante()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void listar_sinSesion_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void reservar_sinSesion_responde401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo(franjaA)))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- cancelar y reprogramar (CU15)

    @Test
    void cancelar_aTiempo_responde200ConLaHoraDevueltaYElSaldo() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(estudiante, BEARER, codigo)).thenReturn(
            new CancelacionResponse(reserva(), HoraDeCancelacion.DEVUELTA, 5));

        mockMvc.perform(post(RUTA + "/" + codigo + "/cancelar").with(comoEstudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Clase cancelada correctamente"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.horaDevuelta").value("DEVUELTA"))
            .andExpect(jsonPath("$.data.horasDisponibles").value(5))
            .andExpect(jsonPath("$.data.reserva.instructorNombre").value("Ivan Alfa"))
            .andExpect(jsonPath("$.data.reserva.cancelacionATiempo").value(true))
            .andExpect(jsonPath("$.data.reserva.id").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void cancelar_tardeYEnProceso_respondenConSuHoraYSaldoNuloSiCorresponde() throws Exception {
        UUID tarde = UUID.randomUUID();
        UUID enProceso = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(estudiante, BEARER, tarde)).thenReturn(
            new CancelacionResponse(reserva(), HoraDeCancelacion.PERDIDA, 3));
        when(service.cancelar(estudiante, BEARER, enProceso)).thenReturn(
            new CancelacionResponse(reserva(), HoraDeCancelacion.EN_PROCESO, null));

        mockMvc.perform(post(RUTA + "/" + tarde + "/cancelar").with(comoEstudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.horaDevuelta").value("PERDIDA"))
            .andExpect(jsonPath("$.data.horasDisponibles").value(3));
        mockMvc.perform(post(RUTA + "/" + enProceso + "/cancelar").with(comoEstudiante()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.horaDevuelta").value("EN_PROCESO"))
            .andExpect(jsonPath("$.data.horasDisponibles").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void cancelar_unaReservaQueNoExiste_responde404ConERR_ACA_32() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(estudiante, BEARER, codigo)).thenThrow(
            new NotFoundException(AcademicoErrorCodes.RESERVA_NO_ENCONTRADA, "No encontramos esa clase"));

        mockMvc.perform(post(RUTA + "/" + codigo + "/cancelar").with(comoEstudiante()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ACA_32"));
    }

    @Test
    void cancelar_unaClaseNoCancelable_responde409ConERR_ACA_33() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cancelar(estudiante, BEARER, codigo)).thenThrow(
            new ConflictException(AcademicoErrorCodes.RESERVA_NO_MODIFICABLE, "Esa clase ya no se puede cancelar"));

        mockMvc.perform(post(RUTA + "/" + codigo + "/cancelar").with(comoEstudiante()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_33"));
    }

    @Test
    void cancelar_conUnCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(post(RUTA + "/no-es-un-uuid/cancelar").with(comoEstudiante()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void cancelar_sinSesion_responde401() throws Exception {
        UUID codigo = UUID.randomUUID();

        mockMvc.perform(post(RUTA + "/" + codigo + "/cancelar"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    @Test
    void reprogramar_responde200ConLaReservaEnLaFranjaNueva() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.reprogramar(estudiante, escuela, BEARER, codigo, franjaA)).thenReturn(reserva());

        mockMvc.perform(post(RUTA + "/" + codigo + "/reprogramar").with(comoEstudiante())
                .contentType(MediaType.APPLICATION_JSON).content("{\"franjaCodigo\":\"" + franjaA + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Clase reprogramada correctamente"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.estado").value("CONFIRMADA"))
            .andExpect(jsonPath("$.data.instructorNombre").value("Ivan Alfa"))
            .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void reprogramar_losErroresDeNegocioConservanSuCodigoYSuStatus() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.reprogramar(estudiante, escuela, BEARER, codigo, franjaA)).thenThrow(
            new ConflictException(AcademicoErrorCodes.FRANJA_NO_DISPONIBLE, "La franja ya no está disponible"));
        when(service.reprogramar(estudiante, escuela, BEARER, codigo, franjaB)).thenThrow(
            new ConflictException(AcademicoErrorCodes.RESERVA_FUERA_DE_PLAZO, "Fuera de plazo"));

        mockMvc.perform(post(RUTA + "/" + codigo + "/reprogramar").with(comoEstudiante())
                .contentType(MediaType.APPLICATION_JSON).content("{\"franjaCodigo\":\"" + franjaA + "\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_18"));
        mockMvc.perform(post(RUTA + "/" + codigo + "/reprogramar").with(comoEstudiante())
                .contentType(MediaType.APPLICATION_JSON).content("{\"franjaCodigo\":\"" + franjaB + "\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_34"));
    }

    @Test
    void reprogramar_conElCuerpoInvalido_responde400() throws Exception {
        UUID codigo = UUID.randomUUID();

        mockMvc.perform(post(RUTA + "/" + codigo + "/reprogramar").with(comoEstudiante())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA + "/" + codigo + "/reprogramar").with(comoEstudiante())
                .contentType(MediaType.APPLICATION_JSON).content("{\"franjaCodigo\":\"no-es-uuid\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA + "/no-es-un-uuid/reprogramar").with(comoEstudiante())
                .contentType(MediaType.APPLICATION_JSON).content("{\"franjaCodigo\":\"" + franjaA + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void reprogramar_sinSesion_responde401() throws Exception {
        UUID codigo = UUID.randomUUID();

        mockMvc.perform(post(RUTA + "/" + codigo + "/reprogramar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"franjaCodigo\":\"" + franjaA + "\"}"))
            .andExpect(status().isUnauthorized());
    }
}
