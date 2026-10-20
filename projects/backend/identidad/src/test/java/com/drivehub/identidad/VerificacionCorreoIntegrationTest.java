package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * CU04b de punta a punta: pedir el correo de verificación y confirmarlo con
 * Postgres real. El envío se reemplaza por un doble que guarda el enlace.
 * Las cuentas son Super admin (no pertenecen a ninguna escuela), así que no
 * hace falta una Escuela falsa. Cada método nombra el criterio (CAxx) que cubre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@ExtendWith(OutputCaptureExtension.class)
class VerificacionCorreoIntegrationTest extends PostgresIntegrationTestBase {

    private static final String PASSWORD = "clave-de-prueba-1";
    private static final String RUTA = "/api/v1/auth/verificar-correo";

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

    private String crearSuperAdmin() {
        String email = "verif-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        usuarios.guardar(Usuario.builder().email(email).passwordHash(hasher.hashear(PASSWORD)).nombres("Ana")
            .apellidos("Pérez").fechaNacimiento(LocalDate.of(1995, 5, 5)).estado(EstadoUsuario.ACTIVO).intentosFallidos(0)
            .roles(Set.of(roles.buscarPorNombre(RolNombres.SUPER_ADMIN).orElseThrow())).build());
        return email;
    }

    private ResultActions login(String email) throws Exception {
        String json = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"dispositivo\":\"WEB\"}";
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String accessToken(ResultActions login) throws Exception {
        return JsonPath.read(login.andReturn().getResponse().getContentAsString(), "$.data.accessToken");
    }

    private ResultActions pedirCorreo(String accessToken) throws Exception {
        var peticion = post(RUTA);
        if (accessToken != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return mockMvc.perform(peticion);
    }

    private ResultActions confirmar(String token) throws Exception {
        return mockMvc.perform(post(RUTA + "/confirmar").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"));
    }

    /** Todos los enlaces enviados hasta ahora, en orden; el token es lo que va después de `token=`. */
    private List<String> enlacesEnviados() {
        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correo, atLeastOnce()).enviarVerificacionCorreo(any(), any(), enlace.capture());
        return enlace.getAllValues();
    }

    private static String tokenDe(String enlace) {
        return enlace.substring(enlace.indexOf("token=") + "token=".length());
    }

    // ---------- criterios ----------

    @Test
    void ca02Ca03Ca07_pedirYConfirmar_verificaElCorreoYLaSesionLoReflejaAlRenovar(CapturedOutput salida) throws Exception {
        String email = crearSuperAdmin();
        ResultActions primerLogin = login(email);
        primerLogin.andExpect(status().isOk()).andExpect(jsonPath("$.data.usuario.correoVerificado").value(false));
        String access = accessToken(primerLogin);

        pedirCorreo(access).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));

        verify(correo).enviarVerificacionCorreo(eq(email), eq("Ana"), any());
        String enlace = enlacesEnviados().get(0);
        assertThat(enlace).contains("/cuenta/verificar-correo?token=");
        String token = tokenDe(enlace);

        // * Confirmar es público: sin Authorization
        confirmar(token).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));

        login(email).andExpect(jsonPath("$.data.usuario.correoVerificado").value(true));
        assertThat(salida.getOut()).doesNotContain(token);
    }

    @Test
    void ca06_conElCorreoYaVerificado_pedirOtroResponde409ErrIde10() throws Exception {
        String email = crearSuperAdmin();
        String access = accessToken(login(email));
        pedirCorreo(access).andExpect(status().isOk());
        confirmar(tokenDe(enlacesEnviados().get(0))).andExpect(status().isOk());

        pedirCorreo(access).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_10"));
    }

    @Test
    void ca04_reutilizarElEnlaceOUnoInventado_responde400ErrIde11() throws Exception {
        String email = crearSuperAdmin();
        pedirCorreo(accessToken(login(email))).andExpect(status().isOk());
        String token = tokenDe(enlacesEnviados().get(0));
        confirmar(token).andExpect(status().isOk());

        confirmar(token).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_11"));
        confirmar("token-inventado").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_11"));
    }

    @Test
    void ca05_pedirOtroCorreoInvalidaElEnlaceAnterior() throws Exception {
        String email = crearSuperAdmin();
        String access = accessToken(login(email));
        pedirCorreo(access).andExpect(status().isOk());
        pedirCorreo(access).andExpect(status().isOk());

        List<String> enlaces = enlacesEnviados();
        String primero = tokenDe(enlaces.get(enlaces.size() - 2));
        String segundo = tokenDe(enlaces.get(enlaces.size() - 1));

        confirmar(primero).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_IDE_11"));
        confirmar(segundo).andExpect(status().isOk());
    }

    @Test
    void pedirElCorreoSinSesion_responde401ErrSys04() throws Exception {
        pedirCorreo(null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    @Test
    void ca08_unaCuentaSinVerificarPuedeIniciarSesionYRenovarConNormalidad() throws Exception {
        String email = crearSuperAdmin();

        ResultActions login = login(email);
        login.andExpect(status().isOk()).andExpect(jsonPath("$.data.usuario.correoVerificado").value(false));
        String cabecera = login.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String refresh = cabecera.substring("refreshToken=".length(), cabecera.indexOf(';'));

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dispositivo\":\"WEB\"}").cookie(new Cookie("refreshToken", refresh)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.usuario.correoVerificado").value(false));
    }
}
