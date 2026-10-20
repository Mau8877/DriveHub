package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.ResultActions;

import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.port.in.RestablecerContrasenaUseCase;
import com.drivehub.identidad.domain.port.in.SolicitarRecuperacionUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;

@WebMvcTest(RecuperacionContrasenaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RecuperacionContrasenaControllerTest {

    private static final String MENSAJE_RECUPERAR = "Si el correo está registrado y verificado, te enviamos un enlace";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitarRecuperacionUseCase solicitarRecuperacion;

    @MockitoBean
    private RestablecerContrasenaUseCase restablecerContrasena;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private ResultActions enviar(String ruta, String json) throws Exception {
        return mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void recuperar_esPublico_yResponde200ConElMensajeFijo() throws Exception {
        enviar("/api/v1/auth/recuperar", "{\"email\":\"ana@gmail.com\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").value(MENSAJE_RECUPERAR));

        verify(solicitarRecuperacion).solicitar("ana@gmail.com");
    }

    @Test
    void recuperar_respondeLoMismoSeaCualSeaElResultadoDelCasoDeUso() throws Exception {
        // * Con el caso de uso mockeado "sin hacer nada" (correo inexistente) la respuesta es idéntica
        String unica = enviar("/api/v1/auth/recuperar", "{\"email\":\"nadie@gmail.com\"}")
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String otra = enviar("/api/v1/auth/recuperar", "{\"email\":\"ana@gmail.com\"}")
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // * Solo difiere el timestamp
        assertThat(unica.replaceAll("\"timestamp\":\"[^\"]*\"", "")).isEqualTo(otra.replaceAll("\"timestamp\":\"[^\"]*\"", ""));
    }

    @Test
    void recuperar_conCorreoVacioOMalFormado_responde400ErrSys01() throws Exception {
        enviar("/api/v1/auth/recuperar", "{\"email\":\"\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        enviar("/api/v1/auth/recuperar", "{\"email\":\"no-es-correo\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        enviar("/api/v1/auth/recuperar", "{}").andExpect(status().isBadRequest());

        verify(solicitarRecuperacion, never()).solicitar(any());
    }

    @Test
    void restablecer_esPublico_yLlamaAlCasoDeUsoConElTokenYLaContrasena() throws Exception {
        enviar("/api/v1/auth/restablecer", "{\"token\":\"abc123\",\"password\":\"contraseña-nueva-1\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente"));

        verify(restablecerContrasena).restablecer("abc123", "contraseña-nueva-1");
    }

    @Test
    void restablecer_conContrasenaCortaOTokenVacio_responde400ErrSys01SinLlamarAlCasoDeUso() throws Exception {
        enviar("/api/v1/auth/restablecer", "{\"token\":\"abc123\",\"password\":\"corta\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        enviar("/api/v1/auth/restablecer", "{\"token\":\" \",\"password\":\"contraseña-nueva-1\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        enviar("/api/v1/auth/restablecer", "{\"token\":\"abc123\"}").andExpect(status().isBadRequest());

        verify(restablecerContrasena, never()).restablecer(any(), any());
    }

    @Test
    void restablecer_conUnEnlaceInvalido_responde400ErrIde11SinEcoDeLaContrasena() throws Exception {
        doThrow(new ValidationException(IdentidadErrorCodes.ENLACE_INVALIDO, "El enlace no es válido o ya venció"))
            .when(restablecerContrasena).restablecer("viejo", "contraseña-nueva-1");

        String cuerpo = enviar("/api/v1/auth/restablecer", "{\"token\":\"viejo\",\"password\":\"contraseña-nueva-1\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_IDE_11"))
            .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("contraseña-nueva-1");
    }
}
