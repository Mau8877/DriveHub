package com.drivehub.pagos.features.planes;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * CU25 de punta a punta: controller, service, repositorio y Postgres reales (Testcontainers); si no hay
 * Docker, se salta. Un método por criterio de aceptación.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GestionPlanesPagoIntegrationTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA_A = UUID.randomUUID();
    private static final UUID ESCUELA_B = UUID.randomUUID();
    private static final String BASE = "/api/v1/planes-pago";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        vaciarTablas(jdbc);
    }

    private static JwtRequestPostProcessor como(String rol, UUID escuela) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol))
            .jwt(j -> {
                if (escuela != null) {
                    j.claim("escuela", escuela.toString());
                }
            });
    }

    private static JwtRequestPostProcessor admin(UUID escuela) {
        return como("ADMIN_ESCUELA", escuela);
    }

    private static String cuerpo(String nombre, int horas, String costo) {
        return "{\"nombre\":\"" + nombre + "\",\"horas\":" + horas + ",\"costoBs\":" + costo + "}";
    }

    /** Crea un plan por la API y devuelve su código público. */
    private String crear(UUID escuela, String nombre, int horas, String costo) throws Exception {
        String respuesta = mockMvc.perform(post(BASE).with(admin(escuela)).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(nombre, horas, costo)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.data.codigo");
    }

    @Test
    void ca01_elAdministradorListaSoloLosPlanesDeSuEscuela_conBsYUsd_paginadosYFiltrados() throws Exception {
        crear(ESCUELA_A, "Plan básico", 10, "450.00");
        crear(ESCUELA_A, "Plan intensivo", 40, "1600.00");
        String desactivado = crear(ESCUELA_A, "Plan viejo", 5, "250.00");
        crear(ESCUELA_B, "Plan de otra escuela", 10, "300.00");
        mockMvc.perform(post(BASE + "/" + desactivado + "/desactivar").with(admin(ESCUELA_A)))
            .andExpect(status().isOk());

        mockMvc.perform(get(BASE).with(admin(ESCUELA_A)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(3)))
            .andExpect(jsonPath("$.data[0].nombre").value("Plan viejo"))
            .andExpect(jsonPath("$.data[2].nombre").value("Plan básico"))
            .andExpect(jsonPath("$.data[2].costoBs").value(450.00))
            .andExpect(jsonPath("$.data[2].costoUsd").value(38.23))
            .andExpect(jsonPath("$.meta.totalItems").value(3))
            .andExpect(jsonPath("$.meta.page").value(1));
        mockMvc.perform(get(BASE).with(admin(ESCUELA_A)).param("search", "INTENS"))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].nombre").value("Plan intensivo"));
        mockMvc.perform(get(BASE).with(admin(ESCUELA_A)).param("filter[activo]", "false"))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].activo").value(false));
        mockMvc.perform(get(BASE).with(admin(ESCUELA_A)).param("page", "2").param("pageSize", "2"))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.meta.totalPages").value(2));
        mockMvc.perform(get(BASE).with(admin(ESCUELA_B)))
            .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void ca02_conDatosValidos_creaElPlanActivoDeLaEscuelaDelToken() throws Exception {
        mockMvc.perform(post(BASE).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("  Plan 10  ", 10, "450.50")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.nombre").value("Plan 10"))
            .andExpect(jsonPath("$.data.horas").value(10))
            .andExpect(jsonPath("$.data.costoBs").value(450.50))
            .andExpect(jsonPath("$.data.activo").value(true));

        Integer deLaEscuela = jdbc.queryForObject("SELECT count(*) FROM plan_pago WHERE escuela_codigo = ?",
            Integer.class, ESCUELA_A);
        org.assertj.core.api.Assertions.assertThat(deLaEscuela).isEqualTo(1);
    }

    @Test
    void ca03_unNombreYaUsadoEnLaEscuela_respondeErrPag01_sinCrearNada_yEnOtraEscuelaSeAcepta() throws Exception {
        String codigo = crear(ESCUELA_A, "Plan 10", 10, "450.00");

        for (String repetido : new String[] {"Plan 10", "plan 10", "PLAN 10", "  Plan 10 "}) {
            mockMvc.perform(post(BASE).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo(repetido, 20, "800.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_PAG_01"));
        }
        // * Un plan desactivado también ocupa el nombre
        mockMvc.perform(post(BASE + "/" + codigo + "/desactivar").with(admin(ESCUELA_A))).andExpect(status().isOk());
        mockMvc.perform(post(BASE).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("plan 10", 20, "800.00")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_01"));
        // * Editar a un nombre de otro plan tampoco
        String otro = crear(ESCUELA_A, "Plan 20", 20, "800.00");
        mockMvc.perform(patch(BASE + "/" + otro).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"PLAN 10\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_01"));

        mockMvc.perform(post(BASE).with(admin(ESCUELA_B)).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Plan 10", 10, "450.00")))
            .andExpect(status().isCreated());
        Integer total = jdbc.queryForObject("SELECT count(*) FROM plan_pago WHERE escuela_codigo = ?", Integer.class,
            ESCUELA_A);
        org.assertj.core.api.Assertions.assertThat(total).isEqualTo(2);
    }

    @Test
    void ca04_conDatosInvalidos_respondeErrSys01() throws Exception {
        String[] invalidos = {
            cuerpo("", 10, "450"),
            cuerpo("Plan", 0, "450"),
            cuerpo("Plan", 501, "450"),
            cuerpo("Plan", 10, "0"),
            cuerpo("Plan", 10, "-1"),
            cuerpo("Plan", 10, "10.123"),
            cuerpo("Plan", 10, "100000000"),
            cuerpo("x".repeat(81), 10, "450"),
            "{\"nombre\":\"Plan\",\"horas\":10}",
        };
        for (String invalido : invalidos) {
            mockMvc.perform(post(BASE).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        String codigo = crear(ESCUELA_A, "Plan 10", 10, "450.00");
        mockMvc.perform(patch(BASE + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        Integer total = jdbc.queryForObject("SELECT count(*) FROM plan_pago", Integer.class);
        org.assertj.core.api.Assertions.assertThat(total).isEqualTo(1);
    }

    @Test
    void ca06_editarCambiaSoloLoEnviado_yNoTocaElEstado() throws Exception {
        String codigo = crear(ESCUELA_A, "Plan 10", 10, "450.00");
        mockMvc.perform(post(BASE + "/" + codigo + "/desactivar").with(admin(ESCUELA_A))).andExpect(status().isOk());

        mockMvc.perform(patch(BASE + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"horas\":20}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.horas").value(20))
            .andExpect(jsonPath("$.data.nombre").value("Plan 10"))
            .andExpect(jsonPath("$.data.costoBs").value(450.00))
            .andExpect(jsonPath("$.data.activo").value(false));
        mockMvc.perform(patch(BASE + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Plan 20 horas\",\"costoBs\":800.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombre").value("Plan 20 horas"))
            .andExpect(jsonPath("$.data.horas").value(20))
            .andExpect(jsonPath("$.data.costoUsd").value(67.97));
        // * Renombrar a su mismo nombre (con otras mayúsculas) no choca consigo mismo
        mockMvc.perform(patch(BASE + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"PLAN 20 HORAS\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void ca07_desactivarDejaElPlanEnElListado_yReactivarLoDejaActivo() throws Exception {
        String codigo = crear(ESCUELA_A, "Plan 10", 10, "450.00");

        mockMvc.perform(post(BASE + "/" + codigo + "/desactivar").with(admin(ESCUELA_A)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.activo").value(false));
        mockMvc.perform(get(BASE).with(admin(ESCUELA_A)))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].activo").value(false));
        mockMvc.perform(post(BASE + "/" + codigo + "/activar").with(admin(ESCUELA_A)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.activo").value(true))
            .andExpect(jsonPath("$.data.nombre").value("Plan 10"));
    }

    @Test
    void ca08_unPlanQueYaEstaEnEseEstado_respondeErrPag03_sinCambiarNada() throws Exception {
        String codigo = crear(ESCUELA_A, "Plan 10", 10, "450.00");

        mockMvc.perform(post(BASE + "/" + codigo + "/activar").with(admin(ESCUELA_A)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_03"));
        mockMvc.perform(post(BASE + "/" + codigo + "/desactivar").with(admin(ESCUELA_A))).andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/" + codigo + "/desactivar").with(admin(ESCUELA_A)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_03"));
        Boolean activo = jdbc.queryForObject("SELECT activo FROM plan_pago", Boolean.class);
        org.assertj.core.api.Assertions.assertThat(activo).isFalse();
    }

    @Test
    void ca09_unPlanDeOtraEscuelaOInexistente_respondeErrPag02_aCualquierOperacion() throws Exception {
        String deB = crear(ESCUELA_B, "Plan de B", 10, "450.00");
        String inexistente = UUID.randomUUID().toString();

        for (String codigo : new String[] {deB, inexistente}) {
            mockMvc.perform(get(BASE + "/" + codigo).with(admin(ESCUELA_A)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PAG_02"));
            mockMvc.perform(patch(BASE + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"horas\":20}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PAG_02"));
            mockMvc.perform(post(BASE + "/" + codigo + "/desactivar").with(admin(ESCUELA_A)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PAG_02"));
            mockMvc.perform(post(BASE + "/" + codigo + "/activar").with(admin(ESCUELA_A)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PAG_02"));
        }
        // * El plan de B sigue intacto
        mockMvc.perform(get(BASE + "/" + deB).with(admin(ESCUELA_B)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.horas").value(10));
    }

    @Test
    void ca10_soloElAdministradorDeEscuela_ySinSesion401() throws Exception {
        String codigo = crear(ESCUELA_A, "Plan 10", 10, "450.00");

        for (String rol : new String[] {"ESTUDIANTE", "INSTRUCTOR", "SUPER_ADMIN"}) {
            mockMvc.perform(get(BASE).with(como(rol, ESCUELA_A))).andExpect(status().isForbidden());
            mockMvc.perform(post(BASE).with(como(rol, ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo("Otro", 10, "450")))
                .andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + "/" + codigo).with(como(rol, ESCUELA_A))).andExpect(status().isForbidden());
            mockMvc.perform(patch(BASE + "/" + codigo).with(como(rol, ESCUELA_A))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"horas\":20}"))
                .andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/" + codigo + "/desactivar").with(como(rol, ESCUELA_A)))
                .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(cuerpo("Otro", 10, "450")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unAdministradorSinEscuelaEnElToken_respondeErrPag04() throws Exception {
        mockMvc.perform(get(BASE).with(admin(null)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_PAG_04"));
    }
}
