package com.drivehub.academico.features.clases.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ConflictException;
import com.drivehub.academico.common.exception.ForbiddenException;
import com.drivehub.academico.common.exception.GlobalExceptionHandler;
import com.drivehub.academico.common.exception.NotFoundException;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.config.BearerActual;
import com.drivehub.academico.config.SecurityConfig;
import com.drivehub.academico.config.WebMvcConfig;
import com.drivehub.academico.features.clases.dto.ActualizarClaseRequest;
import com.drivehub.academico.features.clases.dto.CerrarClaseRequest;
import com.drivehub.academico.features.clases.dto.ClaseResponse;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.enums.EstadoClase;
import com.drivehub.academico.features.clases.service.ClaseService;

@WebMvcTest(ClaseController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class ClaseControllerTest {

    private static final String CLASES = "/api/v1/clases";
    private static final String BEARER = "Bearer token-del-instructor";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaseService service;
    @MockitoBean
    private BearerActual bearerActual;

    private final UUID instructor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID reserva = UUID.randomUUID();
    private final UUID codigoClase = UUID.randomUUID();

    private RequestPostProcessor comoInstructor() {
        return jwt().jwt(j -> j.subject(instructor.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
    }

    private ClaseResponse clase(Asistencia asistencia) {
        return new ClaseResponse(codigoClase, FECHA, estudiante, "Ana Pérez", asistencia, "Giros", "Bien", 100, 140,
            EstadoClase.CERRADA);
    }

    private String cuerpoDeCierre(String asistencia, String extra) {
        return "{\"reservaCodigo\":\"" + reserva + "\",\"asistencia\":\"" + asistencia + "\"" + extra + "}";
    }

    // ---------------------------------------------------------------- POST /clases

    @Test
    void cerrar_responde201ConLaClaseYSinIdsInternos() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cerrar(eq(instructor), eq(escuela), eq(BEARER), any(CerrarClaseRequest.class)))
            .thenReturn(clase(Asistencia.PRESENTE));

        mockMvc.perform(post(CLASES).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDeCierre("PRESENTE",
                    ",\"tema\":\"Giros\",\"observaciones\":\"Bien\",\"kmInicio\":100,\"kmFin\":140")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Clase cerrada correctamente"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.codigo").value(codigoClase.toString()))
            .andExpect(jsonPath("$.data.fecha").value("2026-11-02"))
            .andExpect(jsonPath("$.data.estudianteCodigo").value(estudiante.toString()))
            .andExpect(jsonPath("$.data.estudianteNombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.data.asistencia").value("PRESENTE"))
            .andExpect(jsonPath("$.data.tema").value("Giros"))
            .andExpect(jsonPath("$.data.observaciones").value("Bien"))
            .andExpect(jsonPath("$.data.kmInicio").value(100))
            .andExpect(jsonPath("$.data.kmFin").value(140))
            .andExpect(jsonPath("$.data.estado").value("CERRADA"))
            .andExpect(jsonPath("$.data.id").doesNotExist())
            .andExpect(jsonPath("$.data.reservaId").doesNotExist())
            .andExpect(jsonPath("$.data.vehiculoCodigo").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void cerrar_lePasaAlServicioLaReservaLaAsistenciaYLosDatos() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cerrar(any(), any(), any(), any())).thenReturn(clase(Asistencia.TARDE));

        mockMvc.perform(post(CLASES).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDeCierre("TARDE", ",\"tema\":\"Giros\",\"kmInicio\":100")))
            .andExpect(status().isCreated());

        ArgumentCaptor<CerrarClaseRequest> pedido = ArgumentCaptor.forClass(CerrarClaseRequest.class);
        verify(service).cerrar(eq(instructor), eq(escuela), eq(BEARER), pedido.capture());
        org.assertj.core.api.Assertions.assertThat(pedido.getValue()).isEqualTo(
            new CerrarClaseRequest(reserva, Asistencia.TARDE, "Giros", null, 100, null));
    }

    @Test
    void cerrar_conErroresDeNegocio_responde404409400y403ConSuCodigo() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.cerrar(any(), any(), any(), any()))
            .thenThrow(new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA, "No encontramos"))
            .thenThrow(new ConflictException(AcademicoErrorCodes.CLASE_NO_CERRABLE, "No se puede cerrar"))
            .thenThrow(new ValidationException(AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS, "Datos inválidos"))
            .thenThrow(new ForbiddenException(AcademicoErrorCodes.TOKEN_SIN_ESCUELA, "Sin escuela"));
        String cuerpo = cuerpoDeCierre("PRESENTE", "");

        mockMvc.perform(post(CLASES).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ACA_35"));
        mockMvc.perform(post(CLASES).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACA_37"));
        mockMvc.perform(post(CLASES).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ACA_38"));
        mockMvc.perform(post(CLASES).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_ACA_16"));
    }

    @Test
    void cerrar_conUnCuerpoInvalido_responde400ErrSys01SinLlamarAlServicio() throws Exception {
        String[] cuerpos = {
            "{\"asistencia\":\"PRESENTE\"}",
            "{\"reservaCodigo\":\"" + reserva + "\"}",
            cuerpoDeCierre("PRESENTE", ",\"tema\":\"" + "a".repeat(121) + "\""),
            cuerpoDeCierre("PRESENTE", ",\"observaciones\":\"" + "b".repeat(501) + "\""),
            cuerpoDeCierre("PRESENTE", ",\"kmInicio\":-1"),
            cuerpoDeCierre("PRESENTE", ",\"kmFin\":10000000"),
            cuerpoDeCierre("INVENTADA", ""),
            "no es json",
        };

        for (String cuerpo : cuerpos) {
            mockMvc.perform(post(CLASES).with(comoInstructor()).contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void cerrar_sinSesion_responde401() throws Exception {
        mockMvc.perform(post(CLASES).contentType(MediaType.APPLICATION_JSON).content(cuerpoDeCierre("PRESENTE", "")))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- GET /clases/{codigo}

    @Test
    void obtener_responde200ConLaClase() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.obtener(instructor, escuela, BEARER, codigoClase)).thenReturn(clase(Asistencia.PRESENTE));

        mockMvc.perform(get(CLASES + "/" + codigoClase).with(comoInstructor()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Clase obtenida correctamente"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.codigo").value(codigoClase.toString()))
            .andExpect(jsonPath("$.data.asistencia").value("PRESENTE"))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void obtener_deOtroInstructor_responde404ConERR_ACA_35() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.obtener(any(), any(), any(), any())).thenThrow(
            new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA, "No encontramos esa clase"));

        mockMvc.perform(get(CLASES + "/" + codigoClase).with(comoInstructor()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ACA_35"));
    }

    @Test
    void obtener_conUnCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(get(CLASES + "/no-es-un-uuid").with(comoInstructor()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(service);
    }

    @Test
    void obtener_sinSesion_responde401() throws Exception {
        mockMvc.perform(get(CLASES + "/" + codigoClase)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- PATCH /clases/{codigo}

    @Test
    void actualizar_conUnCuerpoParcial_responde200YElServicioRecibeSoloLoEnviado() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.actualizar(eq(instructor), eq(escuela), eq(BEARER), eq(codigoClase),
            any(ActualizarClaseRequest.class))).thenReturn(clase(Asistencia.PRESENTE));

        mockMvc.perform(patch(CLASES + "/" + codigoClase).with(comoInstructor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"kmFin\":150}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Clase actualizada correctamente"))
            .andExpect(jsonPath("$.data.codigo").value(codigoClase.toString()))
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<ActualizarClaseRequest> pedido = ArgumentCaptor.forClass(ActualizarClaseRequest.class);
        verify(service).actualizar(eq(instructor), eq(escuela), eq(BEARER), eq(codigoClase), pedido.capture());
        org.assertj.core.api.Assertions.assertThat(pedido.getValue())
            .isEqualTo(new ActualizarClaseRequest(null, null, null, 150));
    }

    @Test
    void actualizar_unCuerpoVacioEsValido() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.actualizar(any(), any(), any(), any(), any())).thenReturn(clase(Asistencia.PRESENTE));

        mockMvc.perform(patch(CLASES + "/" + codigoClase).with(comoInstructor())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk());
    }

    @Test
    void actualizar_laAsistenciaSeIgnora_noEstaEnElRequest() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.actualizar(any(), any(), any(), any(), any())).thenReturn(clase(Asistencia.PRESENTE));

        mockMvc.perform(patch(CLASES + "/" + codigoClase).with(comoInstructor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"asistencia\":\"AUSENTE\",\"tema\":\"Giros\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<ActualizarClaseRequest> pedido = ArgumentCaptor.forClass(ActualizarClaseRequest.class);
        verify(service).actualizar(any(), any(), any(), any(), pedido.capture());
        org.assertj.core.api.Assertions.assertThat(pedido.getValue())
            .isEqualTo(new ActualizarClaseRequest("Giros", null, null, null));
    }

    @Test
    void actualizar_conErroresDeNegocio_responde404y400ConSuCodigo() throws Exception {
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(service.actualizar(any(), any(), any(), any(), any()))
            .thenThrow(new NotFoundException(AcademicoErrorCodes.CLASE_INSTRUCTOR_NO_ENCONTRADA, "No encontramos"))
            .thenThrow(new ValidationException(AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS, "Datos inválidos"));

        mockMvc.perform(patch(CLASES + "/" + codigoClase).with(comoInstructor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"tema\":\"x\"}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACA_35"));
        mockMvc.perform(patch(CLASES + "/" + codigoClase).with(comoInstructor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"kmFin\":1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ACA_38"));
    }

    @Test
    void actualizar_conUnCuerpoInvalido_responde400ErrSys01() throws Exception {
        String[] cuerpos = {
            "{\"tema\":\"" + "a".repeat(121) + "\"}",
            "{\"observaciones\":\"" + "b".repeat(501) + "\"}",
            "{\"kmInicio\":-5}",
            "{\"kmFin\":10000000}",
            "no es json",
        };

        for (String cuerpo : cuerpos) {
            mockMvc.perform(patch(CLASES + "/" + codigoClase).with(comoInstructor())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        mockMvc.perform(patch(CLASES + "/no-es-un-uuid").with(comoInstructor())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(service);
    }

    @Test
    void actualizar_sinSesion_responde401() throws Exception {
        mockMvc.perform(patch(CLASES + "/" + codigoClase).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
