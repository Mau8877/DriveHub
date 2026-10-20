package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.drivehub.identidad.domain.model.EstadoUsuario;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.PasswordHasherPort;
import com.drivehub.identidad.domain.port.out.RolRepositoryPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.infrastructure.config.IdentidadSeeder;
import com.sun.net.httpserver.HttpServer;

import jakarta.servlet.http.Cookie;

/**
 * CU02 y CU03 de punta a punta: login, renovación y cierre de sesión con Postgres real y un Escuela
 * falso cuyas escuelas se marcan activas o suspendidas desde el test.
 * Cada método nombra el criterio de aceptación (CAxx) que cubre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@ExtendWith(OutputCaptureExtension.class)
class AuthIntegrationTest extends PostgresIntegrationTestBase {

    private static final Map<UUID, Boolean> ESCUELAS = new ConcurrentHashMap<>();
    private static final HttpServer ESCUELA_FALSA = levantarEscuelaFalsa();
    private static final String PASSWORD = "clave-de-prueba-1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UsuarioRepositoryPort usuarios;
    @Autowired
    private RolRepositoryPort roles;
    @Autowired
    private PasswordHasherPort hasher;

    @DynamicPropertySource
    static void apuntarAEscuelaFalsa(DynamicPropertyRegistry registry) {
        registry.add("app.escuela.url", () -> "http://localhost:" + ESCUELA_FALSA.getAddress().getPort());
    }

    @AfterAll
    static void apagarEscuelaFalsa() {
        ESCUELA_FALSA.stop(0);
    }

    // ---------- ayudantes ----------

    private Usuario crear(String email, UUID escuela, boolean escuelaActiva, String... nombresRoles) {
        if (escuela != null) {
            ESCUELAS.put(escuela, escuelaActiva);
        }
        Set<Rol> rolesDeLaCuenta = new java.util.HashSet<>();
        for (String nombre : nombresRoles) {
            rolesDeLaCuenta.add(roles.buscarPorNombre(nombre).orElseThrow());
        }
        return usuarios.guardar(Usuario.builder().email(email).passwordHash(hasher.hashear(PASSWORD)).nombres("Ana")
            .apellidos("Pérez").fechaNacimiento(LocalDate.of(1995, 5, 5)).escuelaCodigo(escuela)
            .estado(EstadoUsuario.ACTIVO).intentosFallidos(0).roles(rolesDeLaCuenta).build());
    }

    private ResultActions login(String email, String password, String dispositivo) throws Exception {
        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"dispositivo\":\"" + dispositivo + "\"}";
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions refreshWeb(String refreshToken) throws Exception {
        var peticion = post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"dispositivo\":\"WEB\"}");
        if (refreshToken != null) {
            peticion.cookie(new Cookie("refreshToken", refreshToken));
        }
        return mockMvc.perform(peticion);
    }

    private ResultActions refreshMovil(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"dispositivo\":\"MOVIL\",\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private String cookieDe(ResultActions respuesta) {
        String cabecera = respuesta.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cabecera).startsWith("refreshToken=");
        return cabecera.substring("refreshToken=".length(), cabecera.indexOf(';'));
    }

    private String refreshDelCuerpo(ResultActions respuesta) throws Exception {
        String cuerpo = respuesta.andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(cuerpo, "$.data.refreshToken");
    }

    private String email(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
    }

    // ---------- criterios ----------

    @Test
    void ca01_adminEnLaWeb_recibeAccessTokenYCookieDeRefreshSinTokenEnElCuerpo() throws Exception {
        ResultActions r = login(IdentidadSeeder.EMAIL_ADMIN_EXAMPLE, "123123123", "WEB");

        r.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.expiraEnSegundos").value(900))
            .andExpect(jsonPath("$.data.usuario.roles[0]").value("ADMIN_ESCUELA"))
            .andExpect(jsonPath("$.data.usuario.escuelaCodigo").value(IdentidadSeeder.CODIGO_ESCUELA_EXAMPLE.toString()))
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
        String cabecera = r.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cabecera).contains("HttpOnly", "SameSite=Lax", "Path=/api/v1/auth");
    }

    @Test
    void ca01_estudianteEnElMovil_recibeElRefreshTokenEnElCuerpo() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("movil");
        crear(correo, escuela, true, RolNombres.ESTUDIANTE);

        login(correo, PASSWORD, "MOVIL").andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void ca04_correoInexistenteYContrasenaIncorrecta_dan_elMismoMensaje401() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c04");
        crear(correo, escuela, true, RolNombres.ESTUDIANTE);

        String sinCorreo = login("nadie-" + UUID.randomUUID() + "@gmail.com", PASSWORD, "MOVIL")
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_04"))
            .andReturn().getResponse().getContentAsString();
        String sinClave = login(correo, "otra-clave-xxxx", "MOVIL")
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_04"))
            .andReturn().getResponse().getContentAsString();

        assertThat(com.jayway.jsonpath.JsonPath.<String>read(sinCorreo, "$.message"))
            .isEqualTo(com.jayway.jsonpath.JsonPath.<String>read(sinClave, "$.message"));
    }

    @Test
    void ca06_cincoIntentosFallidosBloqueanYLaContrasenaCorrectaSigueEnBloqueo() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c06");
        crear(correo, escuela, true, RolNombres.ESTUDIANTE);

        for (int i = 0; i < 4; i++) {
            login(correo, "mal-mal-mal", "MOVIL").andExpect(status().isUnauthorized());
        }
        login(correo, "mal-mal-mal", "MOVIL").andExpect(status().isLocked()).andExpect(jsonPath("$.error").value("ERR_IDE_05"));
        login(correo, PASSWORD, "MOVIL").andExpect(status().isLocked()).andExpect(jsonPath("$.error").value("ERR_IDE_05"));
    }

    @Test
    void ca07_conElBloqueoVencido_laContrasenaCorrectaEntraYReiniciaElContador() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c07");
        crear(correo, escuela, true, RolNombres.ESTUDIANTE);
        for (int i = 0; i < 5; i++) {
            login(correo, "mal-mal-mal", "MOVIL");
        }
        jdbc.update("UPDATE usuario SET bloqueado_hasta = now() - interval '1 minute' WHERE email = ?", correo);

        login(correo, PASSWORD, "MOVIL").andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT intentos_fallidos FROM usuario WHERE email = ?", Integer.class, correo)).isZero();
        assertThat(jdbc.queryForObject("SELECT estado FROM usuario WHERE email = ?", String.class, correo)).isEqualTo("ACTIVO");
    }

    @Test
    void ca08_cuentaInactiva_responde403() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c08");
        crear(correo, escuela, true, RolNombres.ESTUDIANTE);
        jdbc.update("UPDATE usuario SET estado = 'INACTIVO' WHERE email = ?", correo);

        login(correo, PASSWORD, "MOVIL").andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_06"));
    }

    @Test
    void ca09_escuelaSuspendida_responde403PeroElSuperAdminEntraIgual() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c09");
        crear(correo, escuela, false, RolNombres.ESTUDIANTE);

        login(correo, PASSWORD, "MOVIL").andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_07"));
        login(IdentidadSeeder.EMAIL_SUPER_ADMIN, "123123123", "WEB").andExpect(status().isOk());
    }

    @Test
    void ca10_estudianteEnLaWebYAdminEnElMovil_responden403() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c10");
        crear(correo, escuela, true, RolNombres.ESTUDIANTE);

        login(correo, PASSWORD, "WEB").andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_08"));
        login(IdentidadSeeder.EMAIL_ADMIN_EXAMPLE, "123123123", "MOVIL")
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_08"));
    }

    @Test
    void ca11_unSegundoLoginDelMismoDispositivoCierraLaAnteriorYElOtroDispositivoSigue() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c11");
        crear(correo, escuela, true, RolNombres.ADMIN_ESCUELA, RolNombres.ESTUDIANTE);

        String webUno = cookieDe(login(correo, PASSWORD, "WEB").andExpect(status().isOk()));
        String movil = refreshDelCuerpo(login(correo, PASSWORD, "MOVIL").andExpect(status().isOk()));
        login(correo, PASSWORD, "WEB").andExpect(status().isOk());

        refreshWeb(webUno).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_09"));
        refreshMovil(movil).andExpect(status().isOk());
    }

    @Test
    void ca15_ca16_renovarEntregaUnTokenNuevoYElAnteriorYaNoSirve() throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c15");
        crear(correo, escuela, true, RolNombres.ADMIN_ESCUELA);
        String primero = cookieDe(login(correo, PASSWORD, "WEB").andExpect(status().isOk()));

        ResultActions renovada = refreshWeb(primero).andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isNotEmpty());
        String segundo = cookieDe(renovada);
        assertThat(segundo).isNotEqualTo(primero);

        ResultActions reutilizado = refreshWeb(primero).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_IDE_09"));
        assertThat(reutilizado.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        refreshWeb(segundo).andExpect(status().isOk());
    }

    @Test
    void ca15_renovarConTokenInexistenteOSinCookie_responde401() throws Exception {
        refreshWeb("token-que-no-existe").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_09"));
        refreshWeb(null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_09"));
        refreshMovil("tampoco-existe").andExpect(status().isUnauthorized());
    }

    @Test
    void ca17_renovarConLaCuentaInactivaOLaEscuelaSuspendida_seRechaza() throws Exception {
        UUID escuelaA = UUID.randomUUID();
        String inactiva = email("c17a");
        crear(inactiva, escuelaA, true, RolNombres.ESTUDIANTE);
        String tokenA = refreshDelCuerpo(login(inactiva, PASSWORD, "MOVIL").andExpect(status().isOk()));
        jdbc.update("UPDATE usuario SET estado = 'INACTIVO' WHERE email = ?", inactiva);

        refreshMovil(tokenA).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_06"));
        refreshMovil(tokenA).andExpect(status().isUnauthorized());

        UUID escuelaB = UUID.randomUUID();
        String suspendida = email("c17b");
        crear(suspendida, escuelaB, true, RolNombres.ESTUDIANTE);
        String tokenB = refreshDelCuerpo(login(suspendida, PASSWORD, "MOVIL").andExpect(status().isOk()));
        ESCUELAS.put(escuelaB, false);

        refreshMovil(tokenB).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_IDE_07"));
    }

    @Test
    void ca19_niLaContrasenaNiLosTokensApareceEnLosLogs(CapturedOutput salida) throws Exception {
        UUID escuela = UUID.randomUUID();
        String correo = email("c19");
        crear(correo, escuela, true, RolNombres.ESTUDIANTE);

        String refresh = refreshDelCuerpo(login(correo, PASSWORD, "MOVIL").andExpect(status().isOk()));
        login(correo, "intento-fallido-xyz", "MOVIL");
        refreshMovil(refresh).andExpect(status().isOk());

        assertThat(salida.getOut()).doesNotContain(PASSWORD).doesNotContain("intento-fallido-xyz").doesNotContain(refresh);
    }

    // ---------- Escuela falso ----------

    // ---------- CU03: cerrar sesión ----------

    private ResultActions logoutWeb(String refreshToken) throws Exception {
        var peticion = post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{\"dispositivo\":\"WEB\"}");
        if (refreshToken != null) {
            peticion.cookie(new Cookie("refreshToken", refreshToken));
        }
        return mockMvc.perform(peticion);
    }

    private ResultActions logoutMovil(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
            .content("{\"dispositivo\":\"MOVIL\",\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    @Test
    void cu03Ca01Ca02_logoutEnLaWeb_revocaLaSesionYBorraLaCookie() throws Exception {
        String correo = email("logout-web");
        crear(correo, UUID.randomUUID(), true, RolNombres.ADMIN_ESCUELA);
        String refresh = cookieDe(login(correo, PASSWORD, "WEB"));

        logoutWeb(refresh).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").doesNotExist());
        assertThat(logoutWeb(refresh).andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        refreshWeb(refresh).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_09"));
    }

    @Test
    void cu03Ca01_logoutEnElMovil_revocaLaSesionDelCuerpo() throws Exception {
        String correo = email("logout-movil");
        crear(correo, UUID.randomUUID(), true, RolNombres.ESTUDIANTE);
        String refresh = refreshDelCuerpo(login(correo, PASSWORD, "MOVIL"));

        logoutMovil(refresh).andExpect(status().isOk());

        refreshMovil(refresh).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_IDE_09"));
    }

    @Test
    void cu03Ca05_cerrarLaWebConservaLaSesionDelMovil() throws Exception {
        String correo = email("logout-dos");
        crear(correo, UUID.randomUUID(), true, RolNombres.ADMIN_ESCUELA, RolNombres.ESTUDIANTE);
        String refreshWeb = cookieDe(login(correo, PASSWORD, "WEB"));
        String refreshMovil = refreshDelCuerpo(login(correo, PASSWORD, "MOVIL"));

        logoutWeb(refreshWeb).andExpect(status().isOk());

        refreshMovil(refreshMovil).andExpect(status().isOk());
    }

    @Test
    void cu03Ca04Ca07_logoutIdempotenteYSinAccessToken() throws Exception {
        String correo = email("logout-idem");
        crear(correo, UUID.randomUUID(), true, RolNombres.ADMIN_ESCUELA);
        String refresh = cookieDe(login(correo, PASSWORD, "WEB"));

        logoutWeb(null).andExpect(status().isOk());
        logoutWeb("token-inventado").andExpect(status().isOk());
        logoutMovil("token-inventado").andExpect(status().isOk());
        logoutWeb(refresh).andExpect(status().isOk());
        logoutWeb(refresh).andExpect(status().isOk());
    }

    private static HttpServer levantarEscuelaFalsa() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v1/escuelas/por-codigo/", intercambio -> {
                String ruta = intercambio.getRequestURI().getPath();
                UUID codigo = UUID.fromString(ruta.substring(ruta.lastIndexOf('/') + 1));
                Boolean activa = codigo.equals(IdentidadSeeder.CODIGO_ESCUELA_EXAMPLE) ? Boolean.TRUE : ESCUELAS.get(codigo);
                String cuerpo = activa == null
                    ? "{\"status\":\"failed\",\"data\":null,\"message\":\"no existe\",\"error\":\"ERR_ESC_02\"}"
                    : "{\"status\":\"success\",\"data\":{\"codigo\":\"" + codigo + "\",\"slug\":\"e\",\"nombre\":\"E\",\"activa\":"
                        + activa + "},\"message\":\"ok\"}";
                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(activa == null ? 404 : 200, bytes.length);
                try (OutputStream salidaHttp = intercambio.getResponseBody()) {
                    salidaHttp.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar la Escuela falsa", e);
        }
    }
}
