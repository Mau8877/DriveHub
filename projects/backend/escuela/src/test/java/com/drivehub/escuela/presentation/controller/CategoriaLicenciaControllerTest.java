package com.drivehub.escuela.presentation.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.escuela.application.dto.CategoriaLicenciaOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.application.usecase.ListarCategoriasLicenciaUseCase;
import com.drivehub.escuela.application.usecase.ObtenerCategoriaLicenciaUseCase;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.GlobalExceptionHandler;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.infrastructure.config.SecurityConfig;

@WebMvcTest(CategoriaLicenciaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CategoriaLicenciaControllerTest {

    private static final String RUTA = "/api/v1/categorias-licencia";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarCategoriasLicenciaUseCase listarCategorias;
    @MockitoBean
    private ObtenerCategoriaLicenciaUseCase obtenerCategoria;
    // * SecurityConfig es un resource server: necesita un decodificador de JWT
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listar_sinToken_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void listar_conSesion_respondeElSobreConMeta() throws Exception {
        when(listarCategorias.ejecutar(2, 5)).thenReturn(new PaginaOutput<>(
            List.of(new CategoriaLicenciaOutput("P", "Particular")), 2, 5, 6, 2));

        mockMvc.perform(get(RUTA).param("page", "2").param("pageSize", "5").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].sigla").value("P"))
            .andExpect(jsonPath("$.data[0].descripcion").value("Particular"))
            .andExpect(jsonPath("$.meta.page").value(2))
            .andExpect(jsonPath("$.meta.pageSize").value(5))
            .andExpect(jsonPath("$.meta.totalItems").value(6))
            .andExpect(jsonPath("$.meta.totalPages").value(2));
        verify(listarCategorias).ejecutar(2, 5);
    }

    @Test
    void obtener_conSiglaExistente_respondeElObjeto() throws Exception {
        when(obtenerCategoria.ejecutar("SIN")).thenReturn(new CategoriaLicenciaOutput("SIN", "Sin categoría"));

        mockMvc.perform(get(RUTA + "/SIN").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.sigla").value("SIN"))
            .andExpect(jsonPath("$.data.descripcion").value("Sin categoría"));
    }

    @Test
    void obtener_conSiglaInexistente_responde404ConErrEsc07() throws Exception {
        when(obtenerCategoria.ejecutar("ZZZ")).thenThrow(
            new NotFoundException(EscuelaErrorCodes.CATEGORIA_NO_ENCONTRADA, "La categoría de licencia no existe"));

        mockMvc.perform(get(RUTA + "/ZZZ").with(jwt()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ESC_07"));
    }
}
