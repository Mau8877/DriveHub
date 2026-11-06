package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.port.in.CompletarRegistroWhatsappUseCase;
import com.drivehub.identidad.domain.port.in.CrearSolicitudRegistroWhatsappUseCase;
import com.drivehub.identidad.domain.port.in.DatoRegistro;
import com.drivehub.identidad.domain.port.in.EnlaceRegistro;
import com.drivehub.identidad.domain.port.in.ValidarDatoRegistroUseCase;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;
import com.drivehub.identidad.infrastructure.config.WebMvcConfig;

@WebMvcTest({RegistroWhatsappInternoController.class, RegistroWhatsappController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class, ValidadorDatoRegistro.class})
@TestPropertySource(properties = "app.internal.credencial-n8n=secreto-de-prueba")
class RegistroWhatsappControllersTest {

    private static final String CABECERA = "X-Service-Key";
    private static final String CREDENCIAL = "secreto-de-prueba";
    private static final String VALIDACIONES = "/internal/v1/whatsapp/registros/validaciones";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValidarDatoRegistroUseCase validarDato;
    @MockitoBean
    private CrearSolicitudRegistroWhatsappUseCase crearSolicitud;
    @MockitoBean
    private CompletarRegistroWhatsappUseCase completarRegistro;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private org.springframework.test.web.servlet.ResultActions validar(String campo, String valor) throws Exception {
        return mockMvc.perform(post(VALIDACIONES).header(CABECERA, CREDENCIAL)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"campo\":\"" + campo + "\",\"valor\":\"" + valor + "\"}"));
    }

    @Test
    void validar_sinCredencial_responde401() throws Exception {
        mockMvc.perform(post(VALIDACIONES).contentType(MediaType.APPLICATION_JSON)
                .content("{\"campo\":\"nombres\",\"valor\":\"Ana\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validar_nombresValidos_responde200() throws Exception {
        validar("nombres", "Ana").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void validar_nombresDemasiadoLargos_usaElMensajeDelRegistroNormal() throws Exception {
        validar("nombres", "a".repeat(81)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
            .andExpect(jsonPath("$.message").value("Los nombres no pueden superar 80 caracteres"));
    }

    @Test
    void validar_ciConSimbolos_responde400() throws Exception {
        validar("ci", "12 34*").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(validarDato, never()).validar(any());
    }

    @Test
    void validar_ciValidoPeroYaRegistrado_responde409Err19() throws Exception {
        doThrow(new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI"))
            .when(validarDato).validar(new DatoRegistro(DatoRegistro.Tipo.CI, "AB123"));

        validar("ci", "AB123").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_19"));
    }

    @Test
    void validar_fechaConFormatoMalo_responde400() throws Exception {
        validar("fechaNacimiento", "05/11/2000").andExpect(status().isBadRequest());
    }

    @Test
    void validar_fechaDeMenorDeEdad_responde400Err02() throws Exception {
        doThrow(new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD, "Debes tener al menos 18 años"))
            .when(validarDato).validar(new DatoRegistro(DatoRegistro.Tipo.FECHA_NACIMIENTO, "2015-01-01"));

        validar("fechaNacimiento", "2015-01-01").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_02"));
    }

    @Test
    void validar_correoConFormatoMalo_responde400() throws Exception {
        validar("correo", "no-es-correo").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("El correo no es válido"));
    }

    @Test
    void validar_sexoInvalido_responde400() throws Exception {
        validar("sexo", "otro").andExpect(status().isBadRequest());
        validar("sexo", "femenino").andExpect(status().isOk());
    }

    @Test
    void validar_ubicacionFueraDeRango_responde400() throws Exception {
        validar("ubicacion", "-17.78,-63.18").andExpect(status().isOk());
        validar("ubicacion", "95,10").andExpect(status().isBadRequest());
        validar("ubicacion", "solo-uno").andExpect(status().isBadRequest());
    }

    @Test
    void validar_campoDesconocido_responde400() throws Exception {
        validar("rol", "ADMIN").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void crear_conDatosValidos_responde201ConElEnlace() throws Exception {
        when(crearSolicitud.crear(any())).thenReturn(new EnlaceRegistro("http://web/account/set-password?token=abc",
            Instant.parse("2026-11-05T12:30:00Z")));

        mockMvc.perform(post("/internal/v1/whatsapp/registros").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"telefono":"59171234567","nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"2000-01-01",
                     "sexo":"FEMENINO","ci":"AB123","correo":"ana@gmail.com","latitud":-17.78,"longitud":-63.18,
                     "rol":"ADMIN_ESCUELA","password":"123456789"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.enlace").value("http://web/account/set-password?token=abc"));
    }

    @Test
    void crear_sinUbicacion_responde400() throws Exception {
        mockMvc.perform(post("/internal/v1/whatsapp/registros").header(CABECERA, CREDENCIAL)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"telefono":"59171234567","nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"2000-01-01",
                     "sexo":"FEMENINO","ci":"AB123","correo":"ana@gmail.com"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("La ubicación es obligatoria"));
    }

    @Test
    void completar_esPublicaYConUnaContrasenaValidaResponde201() throws Exception {
        when(completarRegistro.completar("abc", "123456789")).thenReturn(true);

        mockMvc.perform(post("/api/v1/registros-whatsapp/completar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"password\":\"123456789\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.vinculado").value(true));
    }

    @Test
    void completar_conContrasenaCorta_responde400SinLlamarAlCasoDeUso() throws Exception {
        mockMvc.perform(post("/api/v1/registros-whatsapp/completar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"password\":\"12345678\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("La contraseña debe tener entre 9 y 64 caracteres"));

        verify(completarRegistro, never()).completar(any(), any());
    }

    @Test
    void completar_conEnlaceInvalido_responde400Err28() throws Exception {
        when(completarRegistro.completar("abc", "123456789")).thenThrow(
            new ValidationException(IdentidadErrorCodes.ENLACE_REGISTRO_INVALIDO, "El enlace no es válido"));

        mockMvc.perform(post("/api/v1/registros-whatsapp/completar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"password\":\"123456789\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_28"));
    }
}
