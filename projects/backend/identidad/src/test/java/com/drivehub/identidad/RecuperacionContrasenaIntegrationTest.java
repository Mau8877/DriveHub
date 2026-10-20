package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.EnviadorCorreoPort;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * CU04 de punta a punta: recuperar y restablecer la contraseña con Postgres real. El envío
 * se reemplaza por un doble que guarda el enlace. Las cuentas tienen los roles Super admin
 * (entra por la web, sin escuela) y Estudiante (entra por la app), así que se prueban las
 * sesiones de los dos dispositivos sin una Escuela falsa. Cada método nombra el criterio (CAxx).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@ExtendWith(OutputCaptureExtension.class)
class RecuperacionContrasenaIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "clave-de-prueba-1";
    private static final String PASSWORD_NUEVA = "clave-nueva-2026";
    private static final String MENSAJE = "Si el correo está registrado y verificado, te enviamos un enlace";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepositoryPort usuarios;
    @Autowired
    private RolRepositoryPort roles;
    @Autowired
    private PasswordHasherPort hasher;

    @MockitoBean
    private EnviadorCorreoPort correo;

    // ---------- ayudantes ----------

    private Usuario crear(boolean verificado, EstadoUsuario estado) {
        String email = "recup-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        return usuarios.guardar(Usuario.builder().email(email).passwordHash(hasher.hashear(PASSWORD)).nombres("Ana")
            .apellidos("Pérez").fechaNacimiento(LocalDate.of(1995, 5, 5)).estado(estado).intentosFallidos(0)
            .correoVerificado(verificado)
            .roles(Set.of(roles.buscarPorNombre(RolNombres.SUPER_ADMIN).orElseThrow(),
                roles.buscarPorNombre(RolNombres.ESTUDIANTE).orElseThrow())).build());
    }

    private Usuario crearVerificada() {
        return crear(true, EstadoUsuario.ACTIVO);
    }

    private ResultActions login(String email, String password, String dispositivo) throws Exception {
        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"dispositivo\":\"" + dispositivo + "\"}";
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions recuperar(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions restablecer(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}"));
    }

    private static String cookieDe(ResultActions login) {
        String cabecera = login.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return cabecera.substring("refreshToken=".length(), cabecera.indexOf(';'));
    }

    private static String refreshMovilDe(ResultActions login) throws Exception {
        return JsonPath.read(login.andReturn().getResponse().getContentAsString(), "$.data.refreshToken");
    }

    private static String accessDe(ResultActions login) throws Exception {
        return JsonPath.read(login.andReturn().getResponse().getContentAsString(), "$.data.accessToken");
    }

    /** Los enlaces de recuperación enviados hasta ahora, en orden. */
    private List<String> enlacesDeRecuperacion() {
        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correo, atLeastOnce()).enviarRecuperacionContrasena(any(), any(), enlace.capture());
        return enlace.getAllValues();
    }

    private static String tokenDe(String enlace) {
        return enlace.substring(enlace.indexOf("token=") + "token=".length());
    }

    private static String sinTimestamp(ResultActions respuesta) throws Exception {
        return respuesta.andReturn().getResponse().getContentAsString().replaceAll("\"timestamp\":\"[^\"]*\"", "");
    }

    // ---------- criterios ----------

    @Test
    void ca01Ca05Ca06Ca11_recuperarYRestablecer_cambiaLaContrasenaYCierraTodasLasSesiones(CapturedOutput salida) throws Exception {
        Usuario cuenta = crearVerificada();
        String refreshWeb = cookieDe(login(cuenta.getEmail(), PASSWORD, "WEB"));
        String refreshMovil = refreshMovilDe(login(cuenta.getEmail(), PASSWORD, "MOVIL"));

        recuperar(cuenta.getEmail()).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(MENSAJE));

        verify(correo).enviarRecuperacionContrasena(eq(cuenta.getEmail()), eq("Ana"), any());
        String enlace = enlacesDeRecuperacion().get(0);
        assertThat(enlace).contains("/cuenta/restablecer?token=");
        String token = tokenDe(enlace);

        restablecer(token, PASSWORD_NUEVA).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));

        login(cuenta.getEmail(), PASSWORD_NUEVA, "WEB").andExpect(status().isOk());
        login(cuenta.getEmail(), PASSWORD, "WEB").andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_IDE_04"));
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dispositivo\":\"WEB\"}").cookie(new Cookie("refreshToken", refreshWeb)))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_09"));
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dispositivo\":\"MOVIL\",\"refreshToken\":\"" + refreshMovil + "\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_09"));
        assertThat(salida.getOut()).doesNotContain(token).doesNotContain(PASSWORD_NUEVA).doesNotContain(PASSWORD);
    }

    @Test
    void ca07_unaCuentaBloqueadaPorIntentosQuedaDesbloqueada() throws Exception {
        Usuario cuenta = crearVerificada();
        usuarios.actualizar(cuenta.toBuilder().estado(EstadoUsuario.BLOQUEADO).intentosFallidos(5)
            .bloqueadoHasta(Instant.now().plusSeconds(900)).build());
        login(cuenta.getEmail(), PASSWORD, "WEB").andExpect(status().isLocked());

        recuperar(cuenta.getEmail()).andExpect(status().isOk());
        restablecer(tokenDe(enlacesDeRecuperacion().get(0)), PASSWORD_NUEVA).andExpect(status().isOk());

        login(cuenta.getEmail(), PASSWORD_NUEVA, "WEB").andExpect(status().isOk());
        Usuario despues = usuarios.buscarPorCodigo(cuenta.getCodigo()).orElseThrow();
        assertThat(despues.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(despues.getIntentosFallidos()).isZero();
        assertThat(despues.getBloqueadoHasta()).isNull();
    }

    @Test
    void ca02_correoInexistenteInactivoOSinVerificar_respondenIgualYNoEnvianNada() throws Exception {
        Usuario valida = crearVerificada();
        Usuario inactiva = crear(true, EstadoUsuario.INACTIVO);
        Usuario sinVerificar = crear(false, EstadoUsuario.ACTIVO);

        String esperada = sinTimestamp(recuperar(valida.getEmail()).andExpect(status().isOk()));

        assertThat(sinTimestamp(recuperar("no-existe@gmail.com").andExpect(status().isOk()))).isEqualTo(esperada);
        assertThat(sinTimestamp(recuperar(inactiva.getEmail()).andExpect(status().isOk()))).isEqualTo(esperada);
        assertThat(sinTimestamp(recuperar(sinVerificar.getEmail()).andExpect(status().isOk()))).isEqualTo(esperada);
        verify(correo, never()).enviarRecuperacionContrasena(eq(inactiva.getEmail()), any(), any());
        verify(correo, never()).enviarRecuperacionContrasena(eq(sinVerificar.getEmail()), any(), any());
        verify(correo, never()).enviarRecuperacionContrasena(eq("no-existe@gmail.com"), any(), any());
    }

    @Test
    void ca03_siFallaElServidorDeCorreo_respondeIgual() throws Exception {
        Usuario cuenta = crearVerificada();
        doThrow(new IllegalStateException("SMTP caido")).when(correo).enviarRecuperacionContrasena(any(), any(), any());

        recuperar(cuenta.getEmail()).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(MENSAJE));
    }

    @Test
    void ca04_pedirOtroEnlaceInvalidaElAnterior() throws Exception {
        Usuario cuenta = crearVerificada();
        recuperar(cuenta.getEmail()).andExpect(status().isOk());
        recuperar(cuenta.getEmail()).andExpect(status().isOk());

        List<String> enlaces = enlacesDeRecuperacion();
        String primero = tokenDe(enlaces.get(enlaces.size() - 2));
        String segundo = tokenDe(enlaces.get(enlaces.size() - 1));

        restablecer(primero, PASSWORD_NUEVA).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_11"));
        restablecer(segundo, PASSWORD_NUEVA).andExpect(status().isOk());
    }

    @Test
    void ca08_reutilizarElEnlaceUnoInventadoOUnoDeVerificacion_responde400ErrIde11() throws Exception {
        Usuario cuenta = crearVerificada();
        recuperar(cuenta.getEmail()).andExpect(status().isOk());
        String token = tokenDe(enlacesDeRecuperacion().get(0));
        restablecer(token, PASSWORD_NUEVA).andExpect(status().isOk());

        restablecer(token, "otra-clave-2026").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_11"));
        restablecer("token-inventado", PASSWORD_NUEVA).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_11"));

        // * Un enlace de verificación del correo (CU04b) no sirve para restablecer la contraseña
        Usuario sinVerificar = crear(false, EstadoUsuario.ACTIVO);
        String access = accessDe(login(sinVerificar.getEmail(), PASSWORD, "WEB"));
        mockMvc.perform(post("/api/v1/auth/verificar-correo").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
            .andExpect(status().isOk());
        ArgumentCaptor<String> enlaceVerificacion = ArgumentCaptor.forClass(String.class);
        verify(correo).enviarVerificacionCorreo(eq(sinVerificar.getEmail()), any(), enlaceVerificacion.capture());
        restablecer(tokenDe(enlaceVerificacion.getValue()), PASSWORD_NUEVA).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_11"));
        login(sinVerificar.getEmail(), PASSWORD, "WEB").andExpect(status().isOk());
    }

    @Test
    void ca09_unaContrasenaInvalidaConservaElEnlace() throws Exception {
        Usuario cuenta = crearVerificada();
        recuperar(cuenta.getEmail()).andExpect(status().isOk());
        String token = tokenDe(enlacesDeRecuperacion().get(0));

        restablecer(token, "corta").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));

        restablecer(token, PASSWORD_NUEVA).andExpect(status().isOk());
        login(cuenta.getEmail(), PASSWORD_NUEVA, "WEB").andExpect(status().isOk());
    }
}
