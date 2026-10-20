package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.port.in.ConfirmarCorreoUseCase;
import com.drivehub.identidad.domain.port.in.SolicitarVerificacionCorreoUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.drivehub.identidad.infrastructure.config.WebMvcConfig;

@WebMvcTest(VerificacionCorreoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class VerificacionCorreoControllerTest {

    private static final String RUTA = "/api/v1/auth/verificar-correo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitarVerificacionCorreoUseCase solicitarVerificacion;

    @MockitoBean
    private ConfirmarCorreoUseCase confirmarCorreo;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void solicitar_conJwt_llamaAlCasoDeUsoConElSubDelToken() throws Exception {
        UUID usuario = UUID.randomUUID();

        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(usuario.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").value("Te enviamos un correo de verificación"));

        verify(solicitarVerificacion).solicitar(usuario);
    }

    @Test
    void solicitar_sinJwt_responde401ErrSys04() throws Exception {
        mockMvc.perform(post(RUTA))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));

        verify(solicitarVerificacion, never()).solicitar(any());
    }

    @Test
    void solicitar_conElCorreoYaVerificado_responde409ErrIde10() throws Exception {
        UUID usuario = UUID.randomUUID();
        doThrow(new ConflictException(IdentidadErrorCodes.CORREO_YA_VERIFICADO, "Tu correo ya está verificado"))
            .when(solicitarVerificacion).solicitar(usuario);

        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(usuario.toString()))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_IDE_10"));
    }

    @Test
    void solicitar_conUnSubQueNoEsUnUuid_responde401ErrIde09() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject("no-es-un-uuid"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_IDE_09"));
    }

    @Test
    void confirmar_esPublico_yLlamaAlCasoDeUsoConElToken() throws Exception {
        mockMvc.perform(post(RUTA + "/confirmar").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"abc123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").value("Correo verificado correctamente"));

        verify(confirmarCorreo).confirmar("abc123");
    }

    @Test
    void confirmar_conTokenVacioOFaltante_responde400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA + "/confirmar").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA + "/confirmar").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        verify(confirmarCorreo, never()).confirmar(any());
    }

    @Test
    void confirmar_conUnEnlaceInvalido_responde400ErrIde11() throws Exception {
        doThrow(new ValidationException(IdentidadErrorCodes.ENLACE_INVALIDO, "El enlace no es válido o ya venció"))
            .when(confirmarCorreo).confirmar("viejo");

        mockMvc.perform(post(RUTA + "/confirmar").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"viejo\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_IDE_11"));
    }
}
