package com.drivehub.identidad.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.drivehub.identidad.common.exception.ForbiddenException;
import com.drivehub.identidad.common.exception.GlobalExceptionHandler;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.LockedException;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.domain.model.Dispositivo;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.IniciarSesionComando;
import com.drivehub.identidad.domain.port.in.IniciarSesionUseCase;
import com.drivehub.identidad.domain.port.in.RenovarSesionComando;
import com.drivehub.identidad.domain.port.in.RenovarSesionUseCase;
import com.drivehub.identidad.domain.port.in.SesionEmitida;
import com.drivehub.identidad.infrastructure.config.SecurityConfig;

import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, AuthExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IniciarSesionUseCase iniciarSesion;

    @MockitoBean
    private RenovarSesionUseCase renovarSesion;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private SesionEmitida sesion() {
        Usuario usuario = Usuario.builder().codigo(UUID.randomUUID()).nombres("Ana").apellidos("Pérez")
            .escuelaCodigo(UUID.randomUUID()).roles(Set.of(Rol.builder().nombre("ADMIN_ESCUELA").build())).build();
        return new SesionEmitida("jwt-de-acceso", 900, "refresh-en-claro", usuario);
    }

    private ResultActions enviar(String ruta, String json) throws Exception {
        return mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String login(String dispositivo) {
        return "{\"email\":\"ana@gmail.com\",\"password\":\"123123123\",\"dispositivo\":\"" + dispositivo + "\"}";
    }

    @Test
    void login_enLaWeb_poneElRefreshTokenEnUnaCookieHttpOnlyYNoEnElCuerpo() throws Exception {
        when(iniciarSesion.iniciar(any())).thenReturn(sesion());

        ResultActions respuesta = enviar("/api/v1/auth/login", login("WEB"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.accessToken").value("jwt-de-acceso"))
            .andExpect(jsonPath("$.data.expiraEnSegundos").value(900))
            .andExpect(jsonPath("$.data.usuario.nombres").value("Ana"))
            .andExpect(jsonPath("$.data.usuario.roles[0]").value("ADMIN_ESCUELA"))
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andExpect(jsonPath("$.data.password").doesNotExist());

        String cookie = respuesta.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("refreshToken=refresh-en-claro", "HttpOnly", "SameSite=Lax", "Path=/api/v1/auth", "Max-Age=604800");
    }

    @Test
    void login_enElMovil_devuelveElRefreshTokenEnElCuerpoYNoPoneCookie() throws Exception {
        when(iniciarSesion.iniciar(any())).thenReturn(sesion());

        enviar("/api/v1/auth/login", login("MOVIL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-en-claro"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_enviaElComandoConElDispositivo() throws Exception {
        when(iniciarSesion.iniciar(any())).thenReturn(sesion());

        enviar("/api/v1/auth/login", login("MOVIL")).andExpect(status().isOk());

        ArgumentCaptor<IniciarSesionComando> comando = ArgumentCaptor.forClass(IniciarSesionComando.class);
        verify(iniciarSesion).iniciar(comando.capture());
        assertThat(comando.getValue().dispositivo()).isEqualTo(Dispositivo.MOVIL);
        assertThat(comando.getValue().email()).isEqualTo("ana@gmail.com");
    }

    @Test
    void login_conCamposInvalidos_responde400ConErrSys01() throws Exception {
        enviar("/api/v1/auth/login", "{\"email\":\"\",\"password\":\"x\",\"dispositivo\":\"WEB\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        enviar("/api/v1/auth/login", "{\"email\":\"no-es-correo\",\"password\":\"x\",\"dispositivo\":\"WEB\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        enviar("/api/v1/auth/login", "{\"email\":\"ana@gmail.com\",\"dispositivo\":\"WEB\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        enviar("/api/v1/auth/login", login("TELEVISOR"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verify(iniciarSesion, never()).iniciar(any());
    }

    @Test
    void login_cadaExcepcionDelServicioLlegaConSuStatusYSuCodigo() throws Exception {
        when(iniciarSesion.iniciar(any()))
            .thenThrow(new UnauthorizedException(IdentidadErrorCodes.CREDENCIALES_INCORRECTAS, "x"))
            .thenThrow(new LockedException(IdentidadErrorCodes.CUENTA_BLOQUEADA, "x"))
            .thenThrow(new ForbiddenException(IdentidadErrorCodes.CUENTA_INACTIVA, "x"))
            .thenThrow(new ForbiddenException(IdentidadErrorCodes.ESCUELA_SUSPENDIDA, "x"))
            .thenThrow(new ForbiddenException(IdentidadErrorCodes.PLATAFORMA_NO_PERMITIDA, "x"));

        enviar("/api/v1/auth/login", login("WEB")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_IDE_04")).andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        enviar("/api/v1/auth/login", login("WEB")).andExpect(status().isLocked()).andExpect(jsonPath("$.error").value("ERR_IDE_05"));
        enviar("/api/v1/auth/login", login("WEB")).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_06"));
        enviar("/api/v1/auth/login", login("WEB")).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_07"));
        enviar("/api/v1/auth/login", login("WEB")).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_08"));
    }

    @Test
    void refresh_enLaWeb_tomaElTokenDeLaCookieYRenuevaLaCookie() throws Exception {
        when(renovarSesion.renovar(any())).thenReturn(sesion());

        ResultActions respuesta = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content("{\"dispositivo\":\"WEB\"}")
                .cookie(new Cookie("refreshToken", "token-de-la-cookie")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("jwt-de-acceso"))
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        ArgumentCaptor<RenovarSesionComando> comando = ArgumentCaptor.forClass(RenovarSesionComando.class);
        verify(renovarSesion).renovar(comando.capture());
        assertThat(comando.getValue().refreshToken()).isEqualTo("token-de-la-cookie");
        assertThat(respuesta.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("refreshToken=refresh-en-claro");
    }

    @Test
    void refresh_enElMovil_tomaElTokenDelCuerpo() throws Exception {
        when(renovarSesion.renovar(any())).thenReturn(sesion());

        enviar("/api/v1/auth/refresh", "{\"dispositivo\":\"MOVIL\",\"refreshToken\":\"token-del-cuerpo\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-en-claro"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        ArgumentCaptor<RenovarSesionComando> comando = ArgumentCaptor.forClass(RenovarSesionComando.class);
        verify(renovarSesion).renovar(comando.capture());
        assertThat(comando.getValue().refreshToken()).isEqualTo("token-del-cuerpo");
    }

    @Test
    void refresh_enLaWebSinCookie_mandaTokenNuloYLaSesionInvalidaBorraLaCookie() throws Exception {
        when(renovarSesion.renovar(any()))
            .thenThrow(new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó"));

        ResultActions respuesta = enviar("/api/v1/auth/refresh", "{\"dispositivo\":\"WEB\"}")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_IDE_09"));

        ArgumentCaptor<RenovarSesionComando> comando = ArgumentCaptor.forClass(RenovarSesionComando.class);
        verify(renovarSesion).renovar(comando.capture());
        assertThat(comando.getValue().refreshToken()).isNull();
        assertThat(respuesta.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("refreshToken=;", "Max-Age=0");
    }
}
