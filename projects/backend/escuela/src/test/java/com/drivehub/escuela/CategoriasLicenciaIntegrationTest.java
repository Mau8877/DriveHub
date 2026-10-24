package com.drivehub.escuela;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import com.drivehub.escuela.domain.gateway.AdministradorEscuelaGateway;

/**
 * CU08 en Escuela: el catálogo de categorías de punta a punta (controller, casos de uso con su
 * @PreAuthorize, repositorio y Postgres real con las categorías sembradas por Flyway).
 *
 * * El container se maneja a mano para poder saltear el test si Docker no está disponible
 * * (ver TESTING_BACKEND.md#testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CategoriasLicenciaIntegrationTest {

    private static final String RUTA = "/api/v1/categorias-licencia";

    private static PostgreSQLContainer<?> postgres;

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void conOtroRolResponde403EnAmbosEndpointsYSinSesion401() throws Exception {
        for (String rol : new String[] {"SUPER_ADMIN", "INSTRUCTOR", "ESTUDIANTE"}) {
            mockMvc.perform(get(RUTA).with(como(rol))).andExpect(status().isForbidden());
            mockMvc.perform(get(RUTA + "/SIN").with(como(rol))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/SIN")).andExpect(status().isUnauthorized());
    }

    @Test
    void administrador_listaLasSeisCategoriasSembradasEnElOrdenDelCatalogo() throws Exception {
        mockMvc.perform(get(RUTA).with(como("ADMIN_ESCUELA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(6))
            .andExpect(jsonPath("$.data[0].sigla").value("P"))
            .andExpect(jsonPath("$.data[1].sigla").value("M"))
            .andExpect(jsonPath("$.data[2].sigla").value("A"))
            .andExpect(jsonPath("$.data[3].sigla").value("B"))
            .andExpect(jsonPath("$.data[4].sigla").value("C"))
            .andExpect(jsonPath("$.data[5].sigla").value("SIN"))
            .andExpect(jsonPath("$.meta.totalItems").value(6))
            .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void administrador_paginaElCatalogo() throws Exception {
        mockMvc.perform(get(RUTA).param("page", "2").param("pageSize", "4").with(como("ADMIN_ESCUELA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].sigla").value("C"))
            .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void administrador_obtieneUnaCategoriaPorSiglaSinDistinguirMayusculas() throws Exception {
        mockMvc.perform(get(RUTA + "/sin").with(como("ADMIN_ESCUELA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sigla").value("SIN"))
            .andExpect(jsonPath("$.data.descripcion").value("Sin categoría"));
    }

    @Test
    void administrador_conSiglaInexistente_recibe404ErrEsc07() throws Exception {
        mockMvc.perform(get(RUTA + "/ZZZ").with(como("ADMIN_ESCUELA")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ESC_07"));
    }
}
