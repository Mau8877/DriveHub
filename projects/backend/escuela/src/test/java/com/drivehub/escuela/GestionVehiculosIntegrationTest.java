package com.drivehub.escuela;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * CU07 en Escuela de punta a punta: controller, casos de uso con su @PreAuthorize, repositorio y
 * Postgres real. Cada método nombra el criterio de aceptación (CAxx).
 *
 * * El container se maneja a mano para poder saltear el test si Docker no está disponible
 * * (ver TESTING_BACKEND.md#testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
class GestionVehiculosIntegrationTest {

    private static final String RUTA = "/api/v1/vehiculos";
    private static final UUID ESCUELA_A = UUID.randomUUID();
    private static final UUID ESCUELA_B = UUID.randomUUID();

    private static PostgreSQLContainer<?> postgres;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void levantarPostgresSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test con base real");
        postgres = new PostgreSQLContainer<>("postgres:18-alpine");
        postgres.start();
    }

    @AfterAll
    static void detenerPostgres() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void propiedadesDeBaseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }

    private static RequestPostProcessor admin(UUID escuela) {
        return jwt().jwt(j -> j.claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_ESCUELA"));
    }

    private static String cuerpo(String placa) {
        return """
            {"placa":"%s","marca":"Toyota","modelo":"Yaris","anio":2020,"transmision":"MANUAL","kilometraje":1000}
            """.formatted(placa);
    }

    private ResultActions registrar(RequestPostProcessor sesion, String placa) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo(placa)));
    }

    private UUID registrarYObtenerCodigo(UUID escuela, String placa) throws Exception {
        String codigo = com.jayway.jsonpath.JsonPath.read(
            registrar(admin(escuela), placa).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
            "$.data.codigo");
        return UUID.fromString(codigo);
    }

    @Test
    void ca10_sinSesionResponde401YConOtroRolResponde403() throws Exception {
        UUID codigo = UUID.randomUUID();
        for (String rol : new String[] {"SUPER_ADMIN", "INSTRUCTOR", "ESTUDIANTE"}) {
            RequestPostProcessor sesion = jwt().jwt(j -> j.claim("escuela", ESCUELA_A.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
            registrar(sesion, "INT-" + rol.length()).andExpect(status().isForbidden());
            mockMvc.perform(get(RUTA).with(sesion)).andExpect(status().isForbidden());
            mockMvc.perform(post(RUTA + "/" + codigo + "/habilitar").with(sesion)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void ca02_ca03_registrarGuardaDisponibleConPlacaEnMayusculasYRechazaLaPlacaRepetida() throws Exception {
        registrar(admin(ESCUELA_A), "reg-001").andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.placa").value("REG-001"))
            .andExpect(jsonPath("$.data.estado").value("DISPONIBLE"));

        // * La placa es única en toda la plataforma: otra escuela tampoco puede repetirla
        registrar(admin(ESCUELA_B), "REG-001").andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ESC_07"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vehiculo WHERE placa = 'REG-001'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT escuela_codigo FROM vehiculo WHERE placa = 'REG-001'", UUID.class))
            .isEqualTo(ESCUELA_A);
    }

    @Test
    void ca01_ca09_cadaEscuelaVeYTocaSoloSusAutos() throws Exception {
        UUID deA = registrarYObtenerCodigo(ESCUELA_A, "AIS-001");
        registrarYObtenerCodigo(ESCUELA_B, "AIS-002");

        mockMvc.perform(get(RUTA).with(admin(ESCUELA_A)).param("search", "ais-"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].placa").value("AIS-001"))
            .andExpect(jsonPath("$.meta.totalItems").value(1));

        // * Un auto de otra escuela es, para quien lo pide, uno que no existe
        mockMvc.perform(get(RUTA + "/" + deA).with(admin(ESCUELA_B)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_08"));
        mockMvc.perform(patch(RUTA + "/" + deA).with(admin(ESCUELA_B)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"Azul\"}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post(RUTA + "/" + deA + "/dar-de-baja").with(admin(ESCUELA_B)))
            .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT estado FROM vehiculo WHERE placa = 'AIS-001'", String.class))
            .isEqualTo("DISPONIBLE");
    }

    @Test
    void ca01_elListadoFiltraPorEstadoYBuscaPorMarcaYModelo() throws Exception {
        UUID escuela = UUID.randomUUID();
        UUID uno = registrarYObtenerCodigo(escuela, "LST-001");
        registrarYObtenerCodigo(escuela, "LST-002");
        mockMvc.perform(post(RUTA + "/" + uno + "/enviar-a-mantenimiento").with(admin(escuela))).andExpect(status().isOk());

        mockMvc.perform(get(RUTA).with(admin(escuela)).param("filter[estado]", "MANTENIMIENTO"))
            .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].placa").value("LST-001"));
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("search", "YARIS"))
            .andExpect(jsonPath("$.meta.totalItems").value(2));
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("search", "100%"))
            .andExpect(jsonPath("$.meta.totalItems").value(0));
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("pageSize", "1").param("page", "2"))
            .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void ca05_editarCambiaSoloLoEnviadoYRechazaBajarElKilometraje() throws Exception {
        UUID codigo = registrarYObtenerCodigo(ESCUELA_A, "EDT-001");

        mockMvc.perform(patch(RUTA + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"Azul\",\"kilometraje\":1500}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.color").value("Azul"))
            .andExpect(jsonPath("$.data.kilometraje").value(1500))
            .andExpect(jsonPath("$.data.marca").value("Toyota"))
            .andExpect(jsonPath("$.data.placa").value("EDT-001"));
        mockMvc.perform(patch(RUTA + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kilometraje\":10}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void ca06_ca07_ca08_cicloDeEstadosBajaYReactivacion() throws Exception {
        UUID codigo = registrarYObtenerCodigo(ESCUELA_A, "EST-001");

        mockMvc.perform(post(RUTA + "/" + codigo + "/habilitar").with(admin(ESCUELA_A)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_09"));
        mockMvc.perform(post(RUTA + "/" + codigo + "/enviar-a-mantenimiento").with(admin(ESCUELA_A)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("MANTENIMIENTO"));
        mockMvc.perform(post(RUTA + "/" + codigo + "/habilitar").with(admin(ESCUELA_A)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("DISPONIBLE"));
        mockMvc.perform(post(RUTA + "/" + codigo + "/dar-de-baja").with(admin(ESCUELA_A)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("BAJA"));

        // * De baja no se edita ni va a mantenimiento, pero sigue en el listado y se puede reactivar
        mockMvc.perform(post(RUTA + "/" + codigo + "/enviar-a-mantenimiento").with(admin(ESCUELA_A)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_09"));
        mockMvc.perform(patch(RUTA + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"Azul\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_09"));
        mockMvc.perform(get(RUTA).with(admin(ESCUELA_A)).param("filter[estado]", "BAJA"))
            .andExpect(jsonPath("$.data[?(@.placa=='EST-001')]").isNotEmpty());
        mockMvc.perform(post(RUTA + "/" + codigo + "/habilitar").with(admin(ESCUELA_A)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("DISPONIBLE"));
    }
}
