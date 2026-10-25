package com.drivehub.escuela.presentation.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.ConfirmarMiAlumnoUseCase;
import com.drivehub.escuela.application.usecase.ListarMisAlumnosUseCase;
import com.drivehub.escuela.common.exception.GlobalExceptionHandler;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.infrastructure.config.SecurityConfig;
import com.drivehub.escuela.infrastructure.config.WebMvcConfig;

@WebMvcTest(MisAlumnosController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class MisAlumnosControllerTest {

    private static final String RUTA = "/api/v1/asignaciones-alumno/mis-alumnos";
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID ALUMNO = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarMisAlumnosUseCase listar;
    @MockitoBean
    private ConfirmarMiAlumnoUseCase confirmar;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(INSTRUCTOR.toString()).claim("escuela", ESCUELA.toString()));
    }

    @Test
    void sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/" + ALUMNO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(listar, confirmar);
    }

    @Test
    void sinEscuelaEnElToken_responde403SinLlamarAlCasoDeUso() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(INSTRUCTOR.toString()))))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        verifyNoInteractions(listar, confirmar);
    }

    @Test
    void listar_respondeElSobreConMeta_yElInstructorYLaEscuelaSalenDelToken() throws Exception {
        when(listar.ejecutar(ESCUELA, INSTRUCTOR, 2, 5))
            .thenReturn(new PaginaOutput<>(List.of(ALUMNO), 2, 5, 6, 2));

        mockMvc.perform(get(RUTA).param("page", "2").param("pageSize", "5").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].estudianteCodigo").value(ALUMNO.toString()))
            .andExpect(jsonPath("$.data[0].id").doesNotExist())
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(5))
            .andExpect(jsonPath("$.meta.totalItems").value(6))
            .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void confirmar_deUnAlumnoPropio_responde200ConSuCodigo() throws Exception {
        when(confirmar.ejecutar(ESCUELA, INSTRUCTOR, ALUMNO)).thenReturn(ALUMNO);

        mockMvc.perform(get(RUTA + "/" + ALUMNO).with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.estudianteCodigo").value(ALUMNO.toString()));
    }

    @Test
    void confirmar_deUnAlumnoAjeno_responde404ErrEsc15() throws Exception {
        when(confirmar.ejecutar(ESCUELA, INSTRUCTOR, ALUMNO))
            .thenThrow(new NotFoundException("ERR_ESC_15", "El alumno no está asignado a este instructor"));

        mockMvc.perform(get(RUTA + "/" + ALUMNO).with(sesion()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ESC_15"));
    }

    @Test
    void otroRol_responde403() throws Exception {
        when(listar.ejecutar(ESCUELA, INSTRUCTOR, null, null)).thenThrow(new AccessDeniedException("rol"));

        mockMvc.perform(get(RUTA).with(sesion())).andExpect(status().isForbidden());
    }

    @Test
    void confirmar_conUnCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(get(RUTA + "/no-es-uuid").with(sesion()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }
}
