package com.drivehub.pagos.features.planes;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.assertj.core.api.Assertions;
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

/**
 * CA05: el costo en dólares es solo una referencia calculada con el tipo de cambio configurado; no se guarda.
 * Con otro tipo de cambio, el mismo plan muestra otro dólar sin tocar los datos.
 */
@SpringBootTest(properties = "app.pagos.tipo-cambio-bs-usd=10")
@AutoConfigureMockMvc
class TipoCambioPlanesIntegrationTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        vaciarTablas(jdbc);
    }

    private static JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_ESCUELA"))
            .jwt(j -> j.claim("escuela", ESCUELA.toString()));
    }

    @Test
    void ca05_elDolarSeCalculaConElTipoDeCambioConfigurado_yNoSeGuardaEnLaBase() throws Exception {
        mockMvc.perform(post("/api/v1/planes-pago").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Plan 10\",\"horas\":10,\"costoBs\":450.00}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.costoUsd").value(45.00));
        mockMvc.perform(get("/api/v1/planes-pago").with(admin()))
            .andExpect(jsonPath("$.data[0].costoBs").value(450.00))
            .andExpect(jsonPath("$.data[0].costoUsd").value(45.00));

        // * Con el tipo de cambio de prueba (11,77) serían 38,23: acá es 10 porque así se configuró
        Integer columnasDeDolar = jdbc.queryForObject("SELECT count(*) FROM information_schema.columns"
            + " WHERE table_name = 'plan_pago' AND column_name ILIKE '%usd%'", Integer.class);
        Assertions.assertThat(columnasDeDolar).isZero();
    }
}
