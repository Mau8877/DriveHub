package com.drivehub.escuela.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.escuela.application.dto.AsignacionVehiculoOutput;
import com.drivehub.escuela.application.dto.CambiarAsignacionInput;
import com.drivehub.escuela.application.dto.HistorialAsignacionOutput;
import com.drivehub.escuela.application.dto.InstructorAsignadoOutput;
import com.drivehub.escuela.application.dto.ListarAsignacionesInput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.AsignarVehiculoUseCase;
import com.drivehub.escuela.application.usecase.CambiarInstructorVehiculoUseCase;
import com.drivehub.escuela.application.usecase.LiberarVehiculoUseCase;
import com.drivehub.escuela.application.usecase.ListarAsignacionesVehiculoUseCase;
import com.drivehub.escuela.application.usecase.ObtenerHistorialVehiculoUseCase;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.GlobalExceptionHandler;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.repository.FiltroAsignaciones;
import com.drivehub.escuela.infrastructure.config.SecurityConfig;

@WebMvcTest(AsignacionVehiculoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AsignacionVehiculoControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID VEHICULO = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final String RUTA = "/api/v1/asignaciones-vehiculo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarAsignacionesVehiculoUseCase listarAsignaciones;
    @MockitoBean
    private AsignarVehiculoUseCase asignarVehiculo;
    @MockitoBean
    private CambiarInstructorVehiculoUseCase cambiarInstructor;
    @MockitoBean
    private LiberarVehiculoUseCase liberarVehiculo;
    @MockitoBean
    private ObtenerHistorialVehiculoUseCase obtenerHistorial;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.claim("escuela", ESCUELA.toString()));
    }

    private static AsignacionVehiculoOutput salida(boolean conInstructor) {
        return new AsignacionVehiculoOutput(VEHICULO, "ABC-123", "Toyota", "Yaris", EstadoVehiculo.DISPONIBLE,
            conInstructor ? new InstructorAsignadoOutput(INSTRUCTOR, "Ana Pérez") : null,
            conInstructor ? LocalDate.of(2026, 10, 1) : null);
    }

    private static String cuerpo(String fecha) {
        return "{\"instructorCodigo\":\"" + INSTRUCTOR + "\"" + (fecha == null ? "" : ",\"fecha\":\"" + fecha + "\"")
            + "}";
    }

    @Test
    void todosLosEndpoints_sinToken_responden401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/" + VEHICULO + "/historial")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/asignar").contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(null))).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/cambiar-instructor").contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(null))).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/liberar")).andExpect(status().isUnauthorized());
    }

    @Test
    void listar_devuelveArrayConMetaYPasaLosFiltros() throws Exception {
        when(listarAsignaciones.ejecutar(any())).thenReturn(new PaginaOutput<>(
            List.of(salida(true), salida(false)), 2, 10, 11, 2));

        mockMvc.perform(get(RUTA).with(sesion()).param("page", "2").param("pageSize", "10")
                .param("search", "ana").param("filter[asignacion]", "ASIGNADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].vehiculoCodigo").value(VEHICULO.toString()))
            .andExpect(jsonPath("$.data[0].instructor.nombreCompleto").value("Ana Pérez"))
            .andExpect(jsonPath("$.data[0].fechaInicio").value("2026-10-01"))
            .andExpect(jsonPath("$.data[1].instructor").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(10))
            .andExpect(jsonPath("$.meta.totalItems").value(11))
            .andExpect(jsonPath("$.meta.totalPages").value(2));

        verify(listarAsignaciones).ejecutar(
            new ListarAsignacionesInput(ESCUELA, 2, 10, "ana", FiltroAsignaciones.Asignacion.ASIGNADO));
    }

    @Test
    void listar_conFiltroDesconocido_responde400() throws Exception {
        mockMvc.perform(get(RUTA).with(sesion()).param("filter[asignacion]", "QUIZAS"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void asignar_conDatosValidos_responde201TomandoLaEscuelaDelToken() throws Exception {
        when(asignarVehiculo.ejecutar(any())).thenReturn(salida(true));

        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/asignar").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo("2026-10-20")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.instructor.codigo").value(INSTRUCTOR.toString()))
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<CambiarAsignacionInput> input = ArgumentCaptor.forClass(CambiarAsignacionInput.class);
        verify(asignarVehiculo).ejecutar(input.capture());
        assertThat(input.getValue()).isEqualTo(
            new CambiarAsignacionInput(ESCUELA, VEHICULO, INSTRUCTOR, LocalDate.of(2026, 10, 20)));
    }

    @Test
    void asignar_conCuerpoInvalido_responde400SinLlamarAlCasoDeUso() throws Exception {
        for (String json : new String[] {"{}", "{\"instructorCodigo\":null}", "{\"instructorCodigo\":\"no-es-uuid\"}",
            cuerpo("no-es-fecha")}) {
            mockMvc.perform(post(RUTA + "/" + VEHICULO + "/asignar").with(sesion())
                    .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verify(asignarVehiculo, never()).ejecutar(any());
    }

    @Test
    void asignar_conTokenSinEscuela_responde403ErrEsc11() throws Exception {
        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/asignar").with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(null)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(EscuelaErrorCodes.TOKEN_SIN_ESCUELA));
        verify(asignarVehiculo, never()).ejecutar(any());
    }

    @Test
    void asignar_conAutoYaAsignado_responde409ErrEsc16() throws Exception {
        when(asignarVehiculo.ejecutar(any()))
            .thenThrow(new ConflictException(EscuelaErrorCodes.VEHICULO_YA_ASIGNADO, "ya asignado"));

        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/asignar").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(null)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ESC_16"));
    }

    @Test
    void cambiarInstructor_responde200ConElNuevoInstructor() throws Exception {
        when(cambiarInstructor.ejecutar(any())).thenReturn(salida(true));

        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/cambiar-instructor").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(null)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.instructor.codigo").value(INSTRUCTOR.toString()));

        verify(cambiarInstructor).ejecutar(new CambiarAsignacionInput(ESCUELA, VEHICULO, INSTRUCTOR, null));
    }

    @Test
    void liberar_funcionaConYSinCuerpo() throws Exception {
        when(liberarVehiculo.ejecutar(any(), any(), any())).thenReturn(salida(false));

        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/liberar").with(sesion()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.instructor").doesNotExist());
        mockMvc.perform(post(RUTA + "/" + VEHICULO + "/liberar").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"fecha\":\"2026-10-20\"}"))
            .andExpect(status().isOk());

        verify(liberarVehiculo).ejecutar(ESCUELA, VEHICULO, null);
        verify(liberarVehiculo).ejecutar(ESCUELA, VEHICULO, LocalDate.of(2026, 10, 20));
    }

    @Test
    void historial_devuelveArrayConMeta() throws Exception {
        when(obtenerHistorial.ejecutar(ESCUELA, VEHICULO, 1, 100)).thenReturn(new PaginaOutput<>(List.of(
            new HistorialAsignacionOutput(UUID.randomUUID(), new InstructorAsignadoOutput(INSTRUCTOR, "Ana Pérez"),
                LocalDate.of(2026, 10, 1), null)), 1, 100, 1, 1));

        mockMvc.perform(get(RUTA + "/" + VEHICULO + "/historial").with(sesion())
                .param("page", "1").param("pageSize", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].instructor.nombreCompleto").value("Ana Pérez"))
            .andExpect(jsonPath("$.data[0].fechaInicio").value("2026-10-01"))
            .andExpect(jsonPath("$.data[0].fechaFin").doesNotExist())
            .andExpect(jsonPath("$.meta.totalItems").value(1));
    }
}
