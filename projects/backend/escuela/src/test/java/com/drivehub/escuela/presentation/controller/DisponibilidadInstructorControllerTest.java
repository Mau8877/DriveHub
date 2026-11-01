package com.drivehub.escuela.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.escuela.application.dto.AutoDeInstructorOutput;
import com.drivehub.escuela.application.dto.ConsultarDisponibilidadInput;
import com.drivehub.escuela.application.dto.DisponibilidadInstructorOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.ConsultarDisponibilidadUseCase;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.GlobalExceptionHandler;
import com.drivehub.escuela.common.exception.ServicioNoDisponibleException;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.Disponibilidad;
import com.drivehub.escuela.domain.entity.EstadoAutos;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.MomentoDelDia;
import com.drivehub.escuela.infrastructure.config.SecurityConfig;

/**
 * CU11: contrato HTTP y forma del sobre. La lógica está en `ConsultarDisponibilidadUseCaseTest`; el 403 por
 * rol lo decide el @PreAuthorize del caso de uso (mockeado acá): se verifica contra el servicio levantado.
 */
@WebMvcTest(DisponibilidadInstructorController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DisponibilidadInstructorControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID VEHICULO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/disponibilidad-instructores";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarDisponibilidadUseCase consultarDisponibilidad;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.claim("escuela", ESCUELA.toString()));
    }

    private static DisponibilidadInstructorOutput salida(boolean conAuto) {
        return new DisponibilidadInstructorOutput(INSTRUCTOR, "Ana Pérez", EstadoInstructor.ACTIVO,
            Disponibilidad.DISPONIBLE, conAuto ? EstadoAutos.DISPONIBLE : EstadoAutos.SIN_AUTO,
            conAuto ? List.of(new AutoDeInstructorOutput(VEHICULO, "ABC-123", "Toyota", "Yaris",
                EstadoVehiculo.DISPONIBLE)) : List.of(), 3, 2, 1);
    }

    @Test
    void sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());

        verify(consultarDisponibilidad, never()).ejecutar(any());
    }

    @Test
    void listar_devuelveArrayConMetaYElContratoSinIdsInternos() throws Exception {
        when(consultarDisponibilidad.ejecutar(any())).thenReturn(new PaginaOutput<>(
            List.of(salida(true), salida(false)), 1, 20, 2, 1));

        mockMvc.perform(get(RUTA).with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Disponibilidad obtenida correctamente"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].instructorCodigo").value(INSTRUCTOR.toString()))
            .andExpect(jsonPath("$.data[0].nombreCompleto").value("Ana Pérez"))
            .andExpect(jsonPath("$.data[0].estadoInstructor").value("ACTIVO"))
            .andExpect(jsonPath("$.data[0].disponibilidad").value("DISPONIBLE"))
            .andExpect(jsonPath("$.data[0].estadoAutos").value("DISPONIBLE"))
            .andExpect(jsonPath("$.data[0].libres").value(3))
            .andExpect(jsonPath("$.data[0].reservadas").value(2))
            .andExpect(jsonPath("$.data[0].bloqueadas").value(1))
            .andExpect(jsonPath("$.data[0].autos[0].vehiculoCodigo").value(VEHICULO.toString()))
            .andExpect(jsonPath("$.data[0].autos[0].placa").value("ABC-123"))
            .andExpect(jsonPath("$.data[0].autos[0].estado").value("DISPONIBLE"))
            .andExpect(jsonPath("$.data[1].estadoAutos").value("SIN_AUTO"))
            .andExpect(jsonPath("$.data[1].autos.length()").value(0))
            .andExpect(jsonPath("$.data[0].usuarioCodigo").doesNotExist())
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.pageSize").value(20))
            .andExpect(jsonPath("$.meta.totalItems").value(2))
            .andExpect(jsonPath("$.meta.totalPages").value(1))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void listar_pasaLaEscuelaDelTokenYTodosLosFiltrosAlCasoDeUso() throws Exception {
        when(consultarDisponibilidad.ejecutar(any())).thenReturn(new PaginaOutput<>(List.of(), 2, 10, 0, 0));

        mockMvc.perform(get(RUTA).with(sesion()).param("page", "2").param("pageSize", "10")
                .param("search", "ana").param("filter[fecha]", "2026-11-02").param("filter[momento]", "TARDE")
                .param("filter[disponibilidad]", "DISPONIBLE").param("filter[auto]", "SIN_AUTO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.meta.totalItems").value(0));

        verify(consultarDisponibilidad).ejecutar(new ConsultarDisponibilidadInput(ESCUELA, 2, 10, "ana",
            LocalDate.of(2026, 11, 2), MomentoDelDia.TARDE, Disponibilidad.DISPONIBLE, EstadoAutos.SIN_AUTO));
    }

    @Test
    void listar_sinFiltros_todoLoOpcionalVaNulo() throws Exception {
        when(consultarDisponibilidad.ejecutar(any())).thenReturn(new PaginaOutput<>(List.of(), 1, 20, 0, 0));

        mockMvc.perform(get(RUTA).with(sesion())).andExpect(status().isOk());

        verify(consultarDisponibilidad).ejecutar(
            new ConsultarDisponibilidadInput(ESCUELA, null, null, null, null, null, null, null));
    }

    @Test
    void listar_conFiltrosDesconocidosOFechaInvalida_responde400SinLlamarAlCasoDeUso() throws Exception {
        for (String[] parametro : new String[][] {{"filter[momento]", "MEDIODIA"},
            {"filter[disponibilidad]", "QUIZAS"}, {"filter[auto]", "VARIOS"}, {"filter[fecha]", "mañana"},
            {"filter[fecha]", "2026-13-45"}}) {
            mockMvc.perform(get(RUTA).with(sesion()).param(parametro[0], parametro[1]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verify(consultarDisponibilidad, never()).ejecutar(any());
    }

    @Test
    void listar_conUnaFechaAnteriorAHoy_responde400ErrSys01() throws Exception {
        when(consultarDisponibilidad.ejecutar(any()))
            .thenThrow(new ValidationException(EscuelaErrorCodes.VALIDACION, "La fecha no puede ser anterior a hoy"));

        mockMvc.perform(get(RUTA).with(sesion()).param("filter[fecha]", "2020-01-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void listar_conTokenSinEscuela_responde403ErrEsc11() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(EscuelaErrorCodes.TOKEN_SIN_ESCUELA));

        verify(consultarDisponibilidad, never()).ejecutar(any());
    }

    @Test
    void listar_siAcademicoNoResponde_responde503ErrEsc20() throws Exception {
        when(consultarDisponibilidad.ejecutar(any())).thenThrow(new ServicioNoDisponibleException(
            EscuelaErrorCodes.HORARIOS_NO_DISPONIBLES, "No pudimos consultar los horarios"));

        mockMvc.perform(get(RUTA).with(sesion()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ESC_20"))
            .andExpect(jsonPath("$.data").doesNotExist());
    }
}
