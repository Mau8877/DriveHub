package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.LimiteExcedidoException;
import com.drivehub.identidad.common.exception.NotFoundException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.port.in.ConfirmarCodigoWhatsappUseCase;
import com.drivehub.identidad.domain.port.in.EmitirSesionWhatsappUseCase;
import com.drivehub.identidad.domain.port.in.SesionWhatsapp;
import com.drivehub.identidad.domain.port.in.SolicitarCodigoWhatsappUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.drivehub.identidad.infrastructure.config.WebMvcConfig;

@WebMvcTest(WhatsappController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
@TestPropertySource(properties = "app.internal.credencial-n8n=secreto-de-prueba")
class WhatsappControllerTest {

    private static final String CABECERA = "X-Service-Key";
    private static final String CREDENCIAL = "secreto-de-prueba";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitarCodigoWhatsappUseCase solicitarCodigo;

    @MockitoBean
    private ConfirmarCodigoWhatsappUseCase confirmarCodigo;

    @MockitoBean
    private EmitirSesionWhatsappUseCase emitirSesion;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void pedirCodigo_conCredencial_responde202SinDatos() throws Exception {
        mockMvc.perform(post("/internal/v1/whatsapp/codigos").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"59171234567\",\"correo\":\"ana@gmail.com\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.message").value("Si el correo tiene una cuenta, te enviamos un código"));
    }

    @Test
    void pedirCodigo_sinCredencial_responde401ErrSys04SinLlamarAlCasoDeUso() throws Exception {
        mockMvc.perform(post("/internal/v1/whatsapp/codigos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"59171234567\",\"correo\":\"ana@gmail.com\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));

        verify(solicitarCodigo, never()).solicitar(any());
    }

    @Test
    void pedirCodigo_conCredencialIncorrecta_responde401() throws Exception {
        mockMvc.perform(post("/internal/v1/whatsapp/codigos").header(CABECERA, "otra")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"59171234567\",\"correo\":\"ana@gmail.com\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void pedirCodigo_conTelefonoInvalido_responde400ErrSys01() throws Exception {
        mockMvc.perform(post("/internal/v1/whatsapp/codigos").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"abc\",\"correo\":\"ana@gmail.com\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void pedirCodigo_alSuperarElLimite_responde429ErrSys02() throws Exception {
        doThrow(new LimiteExcedidoException("Hiciste muchos pedidos")).when(solicitarCodigo).solicitar(any());

        mockMvc.perform(post("/internal/v1/whatsapp/codigos").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"59171234567\",\"correo\":\"ana@gmail.com\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("ERR_SYS_02"));
    }

    @Test
    void vincular_conCodigoCorrecto_responde201() throws Exception {
        mockMvc.perform(post("/internal/v1/whatsapp/vinculos").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"59171234567\",\"correo\":\"ana@gmail.com\",\"codigo\":\"123456\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.vinculado").value(true));
    }

    @Test
    void vincular_conCodigoInvalido_responde400Err25() throws Exception {
        doThrow(new ValidationException(IdentidadErrorCodes.CODIGO_WHATSAPP_INVALIDO, "El código no es válido"))
            .when(confirmarCodigo).confirmar(any());

        mockMvc.perform(post("/internal/v1/whatsapp/vinculos").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"59171234567\",\"correo\":\"ana@gmail.com\",\"codigo\":\"123456\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_25"));
    }

    @Test
    void vincular_conCodigoDeCincoDigitos_responde400ErrSys01() throws Exception {
        mockMvc.perform(post("/internal/v1/whatsapp/vinculos").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"59171234567\",\"correo\":\"ana@gmail.com\",\"codigo\":\"12345\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void sesion_conNumeroVinculado_devuelveElTokenYSuVigencia() throws Exception {
        when(emitirSesion.emitir("59171234567")).thenReturn(new SesionWhatsapp("jwt-corto", 300));

        mockMvc.perform(post("/internal/v1/whatsapp/sesiones").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"59171234567\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("jwt-corto"))
            .andExpect(jsonPath("$.data.expiraEn").value(300));
    }

    @Test
    void sesion_conNumeroSinVinculo_responde404Err26() throws Exception {
        when(emitirSesion.emitir("59171234567")).thenThrow(
            new NotFoundException(IdentidadErrorCodes.WHATSAPP_NO_VINCULADO, "No está vinculado"));

        mockMvc.perform(post("/internal/v1/whatsapp/sesiones").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"59171234567\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_IDE_26"));
    }
}
