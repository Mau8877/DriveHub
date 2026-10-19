package com.drivehub.escuela.presentation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.escuela.application.dto.EscuelaPublicaOutput;
import com.drivehub.escuela.application.usecase.ObtenerEscuelaPorSlugUseCase;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.GlobalExceptionHandler;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.infrastructure.config.SecurityConfig;

@WebMvcTest(EscuelaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EscuelaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerEscuelaPorSlugUseCase obtenerEscuelaPorSlug;

    @Test
    void obtenerPorSlug_conEscuelaActiva_respondeEnSobreSinAutenticacion() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(obtenerEscuelaPorSlug.ejecutar("escuelaexample"))
            .thenReturn(new EscuelaPublicaOutput(codigo, "escuelaexample", "Escuela Example"));

        mockMvc.perform(get("/api/v1/escuelas/escuelaexample"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(codigo.toString()))
            .andExpect(jsonPath("$.data.slug").value("escuelaexample"))
            .andExpect(jsonPath("$.data.nombre").value("Escuela Example"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void obtenerPorSlug_conEscuelaInexistente_responde404ConErrEsc01() throws Exception {
        when(obtenerEscuelaPorSlug.ejecutar("no-existe"))
            .thenThrow(new NotFoundException(EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA, "no existe"));

        mockMvc.perform(get("/api/v1/escuelas/no-existe"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_ESC_01"));
    }
}
