package com.drivehub.escuela;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.domain.gateway.AdministradorCreado;
import com.drivehub.escuela.domain.gateway.AdministradorEscuelaGateway;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;

/**
 * CU05 en Escuela de punta a punta: controller, casos de uso con su @PreAuthorize, repositorio y
 * Postgres real, con Identidad simulada. Cada método nombra el criterio de aceptación (CAxx).
 *
 * * El container se maneja a mano para poder saltear el test si Docker no está disponible
 * * (ver TESTING_BACKEND.md#testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
class GestionEscuelasIntegrationTest {

    private static final String RUTA = "/api/v1/escuelas";

    private static PostgreSQLContainer<?> postgres;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private AdministradorEscuelaGateway identidad;

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

    private static RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static String cuerpoCrear(String slug, String correo) {
        return """
            {"nombre":"Escuela %s","slug":"%s","administrador":{"nombres":"Marta","apellidos":"Gómez",
             "fechaNacimiento":"1985-05-05","email":"%s","password":"123456789",
             "ci":"MG98765","sexo":"FEMENINO","latitud":-16.4897,"longitud":-68.1193}}
            """.formatted(slug, slug, correo);
    }

    private ResultActions crear(RequestPostProcessor sesion, String slug, String correo) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON)
            .content(cuerpoCrear(slug, correo)));
    }

    private void identidadCreaAlAdministrador() {
        when(identidad.crearAdministrador(any(), any()))
            .thenAnswer(inv -> new AdministradorCreado(UUID.randomUUID(), "marta@gmail.com", "Marta", "Gómez"));
    }

    private int filasConSlug(String slug) {
        return jdbc.queryForObject("SELECT count(*) FROM escuela WHERE slug = ?", Integer.class, slug);
    }

    private UUID codigoDe(String slug) {
        return jdbc.queryForObject("SELECT codigo_escuela FROM escuela WHERE slug = ?", UUID.class, slug);
    }

    @Test
    void ca12_sinSesionResponde401YConOtroRolResponde403EnCadaEndpointYNoSeCreaNada() throws Exception {
        UUID codigo = UUID.randomUUID();
        for (String rol : new String[] {"ADMIN_ESCUELA", "INSTRUCTOR", "ESTUDIANTE"}) {
            crear(como(rol), "gest-ca12-" + rol.toLowerCase().replace('_', '-'), "x@gmail.com").andExpect(status().isForbidden());
            mockMvc.perform(get(RUTA).with(como(rol))).andExpect(status().isForbidden());
            mockMvc.perform(patch(RUTA + "/" + codigo).with(como(rol)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Nuevo\"}")).andExpect(status().isForbidden());
            mockMvc.perform(delete(RUTA + "/" + codigo).with(como(rol))).andExpect(status().isForbidden());
            mockMvc.perform(post(RUTA + "/" + codigo + "/reactivar").with(como(rol))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());

        assertThat(filasConSlug("gest-ca12-admin-escuela")).isZero();
        verify(identidad, never()).crearAdministrador(any(), any());
    }

    @Test
    void ca02_ca08_ca09_ca10_flujoCompletoCrearListarEditarDesactivarYReactivar() throws Exception {
        identidadCreaAlAdministrador();

        crear(como("SUPER_ADMIN"), "gest-flujo", "flujo@gmail.com")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.slug").value("gest-flujo"))
            .andExpect(jsonPath("$.data.estado").value("ACTIVA"))
            .andExpect(jsonPath("$.data.administrador.password").doesNotExist());
        UUID codigo = codigoDe("gest-flujo");

        mockMvc.perform(get(RUTA).with(como("SUPER_ADMIN")).param("search", "gest-flujo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].estado").value("ACTIVA"))
            .andExpect(jsonPath("$.meta.totalItems").value(1));

        mockMvc.perform(patch(RUTA + "/" + codigo).with(como("SUPER_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Nombre Corregido\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombre").value("Nombre Corregido"))
            .andExpect(jsonPath("$.data.slug").value("gest-flujo"));

        // * Desactivar: la consulta que hace Identidad al iniciar sesión la ve inactiva, y el registro por slug la oculta
        mockMvc.perform(delete(RUTA + "/" + codigo).with(como("SUPER_ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get(RUTA + "/por-codigo/" + codigo))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.activa").value(false));
        mockMvc.perform(get(RUTA + "/gest-flujo"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ESC_01"));
        mockMvc.perform(get(RUTA).with(como("SUPER_ADMIN")).param("filter[estado]", "INACTIVA").param("search", "gest-flujo"))
            .andExpect(jsonPath("$.data[0].estado").value("INACTIVA"));
        mockMvc.perform(delete(RUTA + "/" + codigo).with(como("SUPER_ADMIN")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_04"));
        mockMvc.perform(patch(RUTA + "/" + codigo).with(como("SUPER_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Otro Nombre\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_06"));

        // * Reactivar: todo vuelve como estaba
        mockMvc.perform(post(RUTA + "/" + codigo + "/reactivar").with(como("SUPER_ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("ACTIVA"));
        mockMvc.perform(get(RUTA + "/por-codigo/" + codigo))
            .andExpect(jsonPath("$.data.activa").value(true)).andExpect(jsonPath("$.data.nombre").value("Nombre Corregido"));
        mockMvc.perform(post(RUTA + "/" + codigo + "/reactivar").with(como("SUPER_ADMIN")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_05"));
    }

    @Test
    void ca03_elSlugDeUnaEscuelaDesactivadaSigueReservado() throws Exception {
        identidadCreaAlAdministrador();
        crear(como("SUPER_ADMIN"), "gest-reservado", "reservado@gmail.com").andExpect(status().isCreated());
        mockMvc.perform(delete(RUTA + "/" + codigoDe("gest-reservado")).with(como("SUPER_ADMIN"))).andExpect(status().isOk());

        crear(como("SUPER_ADMIN"), "gest-reservado", "otro@gmail.com")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ESC_03"));

        assertThat(filasConSlug("gest-reservado")).isOne();
    }

    @Test
    void ca04_siIdentidadRechazaElCorreo_noQuedaLaEscuelaYElErrorEsElDeIdentidad() throws Exception {
        when(identidad.crearAdministrador(any(), any()))
            .thenThrow(new ConflictException("ERR_IDE_01", "Ya existe una cuenta con ese correo"));

        crear(como("SUPER_ADMIN"), "gest-correo-en-uso", "repetido@gmail.com")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));

        assertThat(filasConSlug("gest-correo-en-uso")).isZero();
        verify(identidad, never()).revertirAdministrador(any());
    }

    @Test
    void ca07_siIdentidadNoResponde_noQuedaLaEscuelaYSeRevierteAlAdministrador() throws Exception {
        when(identidad.crearAdministrador(any(), any())).thenThrow(new IdentidadNoDisponibleException("sin respuesta"));

        crear(como("SUPER_ADMIN"), "gest-sin-identidad", "caida@gmail.com")
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error").value("ERR_SYS_00"));

        assertThat(filasConSlug("gest-sin-identidad")).isZero();
        verify(identidad).revertirAdministrador(any(UUID.class));
    }
}
