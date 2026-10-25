package com.drivehub.pagos.common.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.pagos.config.SecurityConfig;

/**
 * El handler global traduce un pedido mal formado a 400 `ERR_SYS_01` en vez de dejarlo caer en el 500.
 */
@WebMvcTest(GlobalExceptionHandlerTest.RutasDePrueba.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, GlobalExceptionHandlerTest.RutasDePrueba.class})
class GlobalExceptionHandlerTest {

    record Cuerpo(Integer horas) {
    }

    @RestController
    static class RutasDePrueba {

        @GetMapping("/api/v1/prueba/{codigo}")
        String porCodigo(@PathVariable UUID codigo) {
            return codigo.toString();
        }

        @GetMapping("/api/v1/prueba")
        String conFiltro(@RequestParam(name = "filter[activo]", required = false) Boolean activo) {
            return String.valueOf(activo);
        }

        @PostMapping("/api/v1/prueba")
        String conCuerpo(@RequestBody Cuerpo cuerpo) {
            return String.valueOf(cuerpo.horas());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unCodigoQueNoEsUuid_responde400ConErrSys01() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/abc").with(jwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void unFiltroConValorInvalido_responde400ConErrSys01() throws Exception {
        mockMvc.perform(get("/api/v1/prueba").param("filter[activo]", "quizas").with(jwt()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void unCuerpoJsonRoto_responde400ConErrSys01() throws Exception {
        mockMvc.perform(post("/api/v1/prueba").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{ no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void unNumeroConFormatoInvalido_responde400SinExponerElDetalleTecnico() throws Exception {
        mockMvc.perform(post("/api/v1/prueba").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"horas\": \"muchas\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"))
            .andExpect(jsonPath("$.message").value("La solicitud no es válida"));
    }

    @Test
    void unPedidoBienFormado_nadaCambia() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/" + UUID.randomUUID()).with(jwt()))
            .andExpect(status().isOk());
    }
}
