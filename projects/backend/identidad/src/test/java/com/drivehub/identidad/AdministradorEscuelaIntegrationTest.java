package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * CU05 en Identidad de punta a punta: controller, servicios con su @PreAuthorize, adaptadores
 * y Postgres real. Cada método nombra el criterio de aceptación (CAxx) que cubre.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdministradorEscuelaIntegrationTest extends PostgresIntegrationTestBase {

    private static final String RUTA = "/api/v1/usuarios/administradores-escuela";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private static RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private ResultActions crear(RequestPostProcessor sesion, UUID escuela, String email) throws Exception {
        String json = """
            {"nombres":"Marta","apellidos":"Gómez","fechaNacimiento":"1985-05-05","email":"%s",
             "password":"123456789","escuelaCodigo":"%s"}
            """.formatted(email, escuela);
        return mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions revertir(RequestPostProcessor sesion, UUID escuela) throws Exception {
        return mockMvc.perform(delete(RUTA).with(sesion).param("escuelaCodigo", escuela.toString()));
    }

    @Test
    void ca12_sinSesionResponde401YConOtroRolResponde403() throws Exception {
        UUID escuela = UUID.randomUUID();

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        crear(como("ADMIN_ESCUELA"), escuela, "adm-ca12a@gmail.com").andExpect(status().isForbidden());
        crear(como("ESTUDIANTE"), escuela, "adm-ca12b@gmail.com").andExpect(status().isForbidden());
        revertir(como("ADMIN_ESCUELA"), escuela).andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email LIKE 'adm-ca12%'", Integer.class)).isZero();
    }

    @Test
    void ca02_elSuperAdminCreaUnAdministradorActivoDeEsaEscuelaSinVerificarSuCorreo() throws Exception {
        UUID escuela = UUID.randomUUID();

        crear(como("SUPER_ADMIN"), escuela, "adm-ca02@gmail.com")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.escuelaCodigo").value(escuela.toString()))
            .andExpect(jsonPath("$.data.password").doesNotExist());

        assertThat(jdbc.queryForObject(
            "SELECT estado || '/' || correo_verificado FROM usuario WHERE email = 'adm-ca02@gmail.com'", String.class))
            .isEqualTo("ACTIVO/false");
        assertThat(jdbc.queryForObject(
            "SELECT r.nombre FROM usuario_rol ur JOIN usuario u ON u.id = ur.usuario_id JOIN rol r ON r.id = ur.rol_id"
                + " WHERE u.email = 'adm-ca02@gmail.com'", String.class)).isEqualTo("ADMIN_ESCUELA");
    }

    @Test
    void ca04_correoEnUsoResponde409ErrIde01() throws Exception {
        crear(como("SUPER_ADMIN"), UUID.randomUUID(), "adm-ca04@gmail.com").andExpect(status().isCreated());

        crear(como("SUPER_ADMIN"), UUID.randomUUID(), "ADM-CA04@gmail.com")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_01"));
    }

    @Test
    void unaEscuelaTieneUnSoloAdministrador_errIde12() throws Exception {
        UUID escuela = UUID.randomUUID();
        crear(como("SUPER_ADMIN"), escuela, "adm-unico1@gmail.com").andExpect(status().isCreated());

        crear(como("SUPER_ADMIN"), escuela, "adm-unico2@gmail.com")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_12"));
    }

    @Test
    void ca07_revertirBorraAlAdministradorSinUsoYLiberaSuCorreo_yEsIdempotente() throws Exception {
        UUID escuela = UUID.randomUUID();
        crear(como("SUPER_ADMIN"), escuela, "adm-ca07@gmail.com").andExpect(status().isCreated());

        revertir(como("SUPER_ADMIN"), escuela).andExpect(status().isOk());
        revertir(como("SUPER_ADMIN"), escuela).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = 'adm-ca07@gmail.com'", Integer.class)).isZero();
        var respuesta = crear(como("SUPER_ADMIN"), escuela, "adm-ca07@gmail.com").andReturn().getResponse();
        assertThat(respuesta.getStatus()).as(respuesta.getContentAsString()).isEqualTo(201);
    }

    @Test
    void revertirUnAdministradorQueYaInicioSesion_responde409ErrIde13YNoLoBorra() throws Exception {
        UUID escuela = UUID.randomUUID();
        crear(como("SUPER_ADMIN"), escuela, "adm-usado@gmail.com").andExpect(status().isCreated());
        jdbc.update("UPDATE usuario SET ultimo_login = now() WHERE email = 'adm-usado@gmail.com'");

        revertir(como("SUPER_ADMIN"), escuela)
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_IDE_13"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = 'adm-usado@gmail.com'", Integer.class)).isOne();
    }
}
