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
            EstadoReserva.CONFIRMADA, false);
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
}
