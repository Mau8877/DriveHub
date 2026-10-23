package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
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

    // * El mapa real no se consulta en las pruebas
    @MockitoBean
    private GeocodificadorInversoPort geocodificador;

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
        return registrarConCi(email, password, nacimiento, slug, CiDePrueba.unico(), extra);
    }

    private ResultActions registrarConCi(String email, String password, LocalDate nacimiento, String slug, String ci,
            String extra) throws Exception {
        String json = """
            {"nombres":"Ana","apellidos":"Pérez","fechaNacimiento":"%s","email":"%s",
             "password":"%s","escuelaSlug":"%s","ci":"%s","sexo":"FEMENINO",
             "latitud":-16.4897,"longitud":-68.1193%s}
            """.formatted(nacimiento, email, password, slug, ci, extra);
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

    @Test
    void ca01_ca03_registroExitoso_creaElPerfilConLaDireccionCalculadaYLaCategoriaSinCategoria() throws Exception {
        when(geocodificador.direccionDe(any(BigDecimal.class), any(BigDecimal.class)))
            .thenReturn(Optional.of("Av. Arce 2345, La Paz"));

        registrarConCi("perfil01@gmail.com", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample", "ab55001", "")
            .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT p.ci FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = 'perfil01@gmail.com'", String.class)).isEqualTo("AB55001");
        assertThat(jdbc.queryForObject("SELECT p.direccion FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = 'perfil01@gmail.com'", String.class)).isEqualTo("Av. Arce 2345, La Paz");
        assertThat(jdbc.queryForObject("SELECT p.categoria_sigla FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = 'perfil01@gmail.com'", String.class)).isEqualTo("SIN");
        assertThat(jdbc.queryForObject("SELECT p.sexo FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = 'perfil01@gmail.com'", String.class)).isEqualTo("FEMENINO");
    }

    @Test
    void ca04_sinDireccionDelMapa_guardaElPuntoYLaDireccionQuedaEnBlanco() throws Exception {
        when(geocodificador.direccionDe(any(BigDecimal.class), any(BigDecimal.class))).thenReturn(Optional.empty());

        registrar("perfil04@gmail.com", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample", "")
            .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT p.direccion FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = 'perfil04@gmail.com'", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT p.latitud FROM perfil p JOIN usuario u ON u.id = p.usuario_id "
            + "WHERE u.email = 'perfil04@gmail.com'", BigDecimal.class)).isEqualByComparingTo("-16.4897");
    }

    @Test
    void ca02_ciRepetido_responde409ConErrIde19YNoCreaLaSegundaCuenta() throws Exception {
        registrarConCi("ci1@gmail.com", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample", "CI99001", "")
            .andExpect(status().isCreated());

        registrarConCi("ci2@gmail.com", "123456789", LocalDate.of(2000, 1, 1), "escuelaexample", "ci99001", "")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_IDE_19"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = 'ci2@gmail.com'", Integer.class))
            .isZero();
    }
}
