package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.port.in.ConfirmarCambioCorreoUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.drivehub.identidad.infrastructure.config.WebMvcConfig;

@WebMvcTest(CambioCorreoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class CambioCorreoControllerTest {

    private static final String RUTA = "/api/v1/auth/cambiar-correo/confirmar";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfirmarCambioCorreoUseCase confirmarCambioCorreo;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void confirmar_esPublico_yLlamaAlCasoDeUsoConElToken() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"abc123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").value("Correo cambiado correctamente"));

        verify(confirmarCambioCorreo).confirmar("abc123");
    }

    @Test
    void confirmar_conTokenVacioOFaltante_responde400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        verify(confirmarCambioCorreo, never()).confirmar(any());
    }

    @Test
    void confirmar_conUnEnlaceInvalido_responde400ErrIde11() throws Exception {
        doThrow(new ValidationException(IdentidadErrorCodes.ENLACE_INVALIDO, "El enlace no es válido o ya venció"))
            .when(confirmarCambioCorreo).confirmar("viejo");

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"viejo\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_IDE_11"));
    }
}
