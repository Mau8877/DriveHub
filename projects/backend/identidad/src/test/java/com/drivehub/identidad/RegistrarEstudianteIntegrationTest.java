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
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.sun.net.httpserver.HttpServer;

/**
 * CU01 de punta a punta: controller, servicio, adaptadores y Postgres real,
 * con un Escuela falso. Cada método nombra el criterio de aceptación (CAxx)
 * que cubre.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegistrarEstudianteIntegrationTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA_CODIGO = UUID.randomUUID();
    private static final HttpServer ESCUELA_FALSA = levantarEscuelaFalsa();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void apuntarAEscuelaFalsa(DynamicPropertyRegistry registry) {
        registry.add("app.escuela.url", () -> "http://localhost:" + ESCUELA_FALSA.getAddress().getPort());
    }

    @AfterAll
    static void apagarEscuelaFalsa() {
        ESCUELA_FALSA.stop(0);
    }

    private ResultActions registrar(String email, String password, LocalDate nacimiento, String slug, String extra)
            throws Exception {
        String json = """
            {"nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"%s","email":"%s",
             "password":"%s","escuelaSlug":"%s"%s}
            """.formatted(nacimiento, email, password, slug, extra);
        return mockMvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void ca01_ca10_registroExitoso_creaEstudianteActivoConSuEscuelaSinExponerContrasena() throws Exception {
        registrar("ca01@gmail.com", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample", "")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("ca01@gmail.com"))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT estado FROM usuario WHERE email = 'ca01@gmail.com'", String.class))
            .isEqualTo("ACTIVO");
        assertThat(jdbc.queryForObject(
            "SELECT escuela_codigo::text FROM usuario WHERE email = 'ca01@gmail.com'", String.class))
            .isEqualTo(ESCUELA_CODIGO.toString());
        assertThat(jdbc.queryForObject(
            "SELECT r.nombre FROM usuario u JOIN usuario_rol ur ON ur.usuario_id = u.id "
                + "JOIN rol r ON r.id = ur.rol_id WHERE u.email = 'ca01@gmail.com'", String.class))
            .isEqualTo("ESTUDIANTE");
        assertThat(jdbc.queryForObject(
            "SELECT password_hash FROM usuario WHERE email = 'ca01@gmail.com'", String.class))
            .startsWith("$2").isNotEqualTo("123456789");
    }

    @Test
    void ca03_correoInvalido_responde400() throws Exception {
        registrar("no-es-correo", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample", "")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void ca04_contrasenaDe8Caracteres_responde400() throws Exception {
        registrar("ca04@gmail.com", "12345678", LocalDate.of(2000, 1, 1), "escuelaexample", "")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void ca05_menorDe18_responde400ConErrIde02() throws Exception {
        registrar("ca05@gmail.com", "123456789", LocalDate.now().minusYears(18).plusDays(1), "escuelaexample", "")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_IDE_02"));
    }

    @Test
    void ca06_exactamente18Anios_acepta() throws Exception {
        registrar("ca06@gmail.com", "123456789", LocalDate.now().minusYears(18), "escuelaexample", "")
            .andExpect(status().isCreated());
    }

    @Test
    void ca07_correoRepetidoEnOtrasMayusculas_responde409ConErrIde01() throws Exception {
        registrar("ca07@gmail.com", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample", "")
            .andExpect(status().isCreated());

        registrar("CA07@Gmail.com", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample", "")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_IDE_01"));
    }

    @Test
    void ca08_escuelaInexistente_responde404ConErrIde03() throws Exception {
        registrar("ca08@gmail.com", "123456789", LocalDate.of(2000, 1, 1), "no-existe", "")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_IDE_03"));
    }

    @Test
    void ca09_rolEnLaPeticion_seIgnoraYQuedaEstudiante() throws Exception {
        registrar("ca09@gmail.com", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample",
            ",\"rol\":\"SUPER_ADMIN\"")
            .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject(
            "SELECT r.nombre FROM usuario u JOIN usuario_rol ur ON ur.usuario_id = u.id "
                + "JOIN rol r ON r.id = ur.rol_id WHERE u.email = 'ca09@gmail.com'", String.class))
            .isEqualTo("ESTUDIANTE");
    }

    private static HttpServer levantarEscuelaFalsa() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v1/escuelas/", intercambio -> {
                boolean existe = intercambio.getRequestURI().getPath().endsWith("/escuelaexample");
                String cuerpo = existe
                    ? "{\"status\":\"success\",\"data\":{\"codigo\":\"" + ESCUELA_CODIGO
                        + "\",\"slug\":\"escuelaexample\",\"nombre\":\"Escuela Example\"},\"message\":\"ok\"}"
                    : "{\"status\":\"failed\",\"data\":null,\"message\":\"no existe\",\"error\":\"ERR_ESC_01\"}";
                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(existe ? 200 : 404, bytes.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar la Escuela falsa", e);
        }
    }
}
