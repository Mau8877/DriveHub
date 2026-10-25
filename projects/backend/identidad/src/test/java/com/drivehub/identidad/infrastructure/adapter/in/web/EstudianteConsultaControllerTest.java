package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.domain.model.PersonaEstudiante;
import com.drivehub.identidad.domain.port.in.ConsultarEstudiantesUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.drivehub.identidad.infrastructure.config.WebMvcConfig;

@WebMvcTest(EstudianteConsultaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class EstudianteConsultaControllerTest {

    private static final String RUTA = "/api/v1/estudiantes/consulta";
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarEstudiantesUseCase consultarEstudiantes;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(INSTRUCTOR.toString()));
    }

    private static String cuerpo(UUID... codigos) {
        return "{\"codigos\":[" + List.of(codigos).stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(","))
            + "]}";
    }

    @Test
    void sinToken_responde401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo(ESTUDIANTE)))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(consultarEstudiantes);
    }

    @Test
    void consultar_respondeSoloCodigoNombresYApellidos_conElSolicitanteDeLaSesion() throws Exception {
        when(consultarEstudiantes.consultar(INSTRUCTOR, List.of(ESTUDIANTE)))
            .thenReturn(List.of(new PersonaEstudiante(ESTUDIANTE, "Ana", "Pérez")));

        mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(cuerpo(ESTUDIANTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].codigo").value(ESTUDIANTE.toString()))
            .andExpect(jsonPath("$.data[0].nombres").value("Ana"))
            .andExpect(jsonPath("$.data[0].apellidos").value("Pérez"))
            .andExpect(jsonPath("$.data[0].email").doesNotExist())
            .andExpect(jsonPath("$.data[0].ci").doesNotExist())
            .andExpect(jsonPath("$.data[0].telefono").doesNotExist())
            .andExpect(jsonPath("$.data[0].id").doesNotExist());
    }

    @Test
    void sinCodigosOConMasDeCien_responde400ErrSys01SinLlamarAlCasoDeUso() throws Exception {
        String ciento1 = IntStream.range(0, 101).mapToObj(i -> "\"" + UUID.randomUUID() + "\"")
            .collect(Collectors.joining(",", "{\"codigos\":[", "]}"));

        mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content("{\"codigos\":[]}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(ciento1))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(consultarEstudiantes);
    }

    @Test
    void unRolSinPermiso_responde403() throws Exception {
        when(consultarEstudiantes.consultar(INSTRUCTOR, List.of(ESTUDIANTE)))
            .thenThrow(new AccessDeniedException("rol"));

        mockMvc.perform(post(RUTA).with(sesion()).contentType(MediaType.APPLICATION_JSON).content(cuerpo(ESTUDIANTE)))
            .andExpect(status().isForbidden());
    }
}
