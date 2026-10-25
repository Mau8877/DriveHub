package com.drivehub.pagos.features.billetera;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.billetera.controller.HorasAlumnosController;
import com.drivehub.pagos.features.billetera.dto.ConsumoClaseResponse;
import com.drivehub.pagos.features.billetera.dto.HoraAlumnoResponse;
import com.drivehub.pagos.features.billetera.dto.HorasAlumnoDetalleResponse;
import com.drivehub.pagos.features.billetera.dto.PagoAlumnoResponse;
import com.drivehub.pagos.features.billetera.service.HorasAlumnosService;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;

/**
 * El contrato HTTP de las horas de los alumnos (CU29): status, forma del sobre y de la paginación. El service va
 * mockeado: su lógica y el rol (`@PreAuthorize`) se prueban en sus tests.
 */
@WebMvcTest(HorasAlumnosController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class HorasAlumnosControllerTest {

    private static final String RUTA = "/api/v1/horas-alumnos";
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID ALUMNO = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HorasAlumnosService servicio;

    private static JwtRequestPostProcessor instructor() {
        return jwt().jwt(j -> j.subject(INSTRUCTOR.toString()).claim("escuela", ESCUELA.toString()));
    }

    private static HoraAlumnoResponse fila() {
        return new HoraAlumnoResponse(ALUMNO, "Ana", "Pérez", 20, 6, 14);
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/" + ALUMNO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void sinEscuelaEnElToken_responde403ErrPag04() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(INSTRUCTOR.toString()))))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_PAG_04"));
        verifyNoInteractions(servicio);
    }

    @Test
    void listar_respondeElSobreConLosAlumnosYElMeta_sinIdsInternos() throws Exception {
        when(servicio.listar(ESCUELA, INSTRUCTOR, null, null, null))
            .thenReturn(new PageImpl<>(List.of(fila()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(RUTA).with(instructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].estudianteCodigo").value(ALUMNO.toString()))
            .andExpect(jsonPath("$.data[0].nombres").value("Ana"))
            .andExpect(jsonPath("$.data[0].apellidos").value("Pérez"))
            .andExpect(jsonPath("$.data[0].horasCompradas").value(20))
            .andExpect(jsonPath("$.data[0].horasGastadasConmigo").value(6))
            .andExpect(jsonPath("$.data[0].horasRestantes").value(14))
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.data[0].escuelaCodigo").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(1))
            .andExpect(jsonPath("$.meta.pageSize").value(20))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void listar_traduceLaPaginacionYLaBusqueda() throws Exception {
        when(servicio.listar(ESCUELA, INSTRUCTOR, "ana", 2, 5))
            .thenReturn(new PageImpl<>(List.of(fila()), PageRequest.of(1, 5), 6));

        mockMvc.perform(get(RUTA).with(instructor()).param("page", "2").param("pageSize", "5").param("search", "ana"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(5))
            .andExpect(jsonPath("$.meta.totalItems").value(6))
            .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void listar_conOtroRol_responde403() throws Exception {
        when(servicio.listar(ESCUELA, INSTRUCTOR, null, null, null)).thenThrow(new AccessDeniedException("rol"));

        mockMvc.perform(get(RUTA).with(instructor())).andExpect(status().isForbidden());
    }

    @Test
    void listar_conEscuelaOIdentidadCaidas_responde503ErrPag15() throws Exception {
        when(servicio.listar(ESCUELA, INSTRUCTOR, null, null, null))
            .thenThrow(new ServicioNoDisponibleException("ERR_PAG_15", "No pudimos consultar tus alumnos"));

        mockMvc.perform(get(RUTA).with(instructor()))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value("ERR_PAG_15"));
    }

    @Test
    void detalle_respondeResumenConsumosYPagos() throws Exception {
        UUID clase = UUID.randomUUID();
        when(servicio.detalle(ESCUELA, INSTRUCTOR, ALUMNO)).thenReturn(new HorasAlumnoDetalleResponse(fila(),
            List.of(new ConsumoClaseResponse(clase, Instant.parse("2026-10-20T15:00:00Z"), 3)),
            List.of(new PagoAlumnoResponse(UUID.randomUUID(), "Plan 10 horas", (short) 10,
                EstadoPagoVisible.CONFIRMADO, Instant.parse("2026-10-01T12:00:00Z")))));

        mockMvc.perform(get(RUTA + "/" + ALUMNO).with(instructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.resumen.estudianteCodigo").value(ALUMNO.toString()))
            .andExpect(jsonPath("$.data.resumen.horasGastadasConmigo").value(6))
            .andExpect(jsonPath("$.data.consumos[0].claseCodigo").value(clase.toString()))
            .andExpect(jsonPath("$.data.consumos[0].horas").value(3))
            .andExpect(jsonPath("$.data.consumos[0].fecha").exists())
            .andExpect(jsonPath("$.data.pagos[0].planNombre").value("Plan 10 horas"))
            .andExpect(jsonPath("$.data.pagos[0].horas").value(10))
            .andExpect(jsonPath("$.data.pagos[0].estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.data.pagos[0].creadoEn").exists())
            .andExpect(jsonPath("$.data.pagos[0].id").doesNotExist());
    }

    @Test
    void detalle_deUnAlumnoAjeno_responde404ErrPag12() throws Exception {
        when(servicio.detalle(ESCUELA, INSTRUCTOR, ALUMNO))
            .thenThrow(new NotFoundException("ERR_PAG_12", "El alumno no existe o no es tuyo"));

        mockMvc.perform(get(RUTA + "/" + ALUMNO).with(instructor()))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PAG_12"));
    }

    @Test
    void detalle_conEscuelaCaida_responde503ErrPag15() throws Exception {
        when(servicio.detalle(ESCUELA, INSTRUCTOR, ALUMNO))
            .thenThrow(new ServicioNoDisponibleException("ERR_PAG_15", "No pudimos consultar tus alumnos"));

        mockMvc.perform(get(RUTA + "/" + ALUMNO).with(instructor()))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value("ERR_PAG_15"));
    }

    @Test
    void detalle_conOtroRol_responde403_yConUnCodigoQueNoEsUuid_responde400() throws Exception {
        when(servicio.detalle(ESCUELA, INSTRUCTOR, ALUMNO)).thenThrow(new AccessDeniedException("rol"));

        mockMvc.perform(get(RUTA + "/" + ALUMNO).with(instructor())).andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA + "/no-es-uuid").with(instructor()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }
}
