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

import com.drivehub.escuela.application.dto.EscuelaEstadoOutput;
import com.drivehub.escuela.application.dto.EscuelaPublicaOutput;
import com.drivehub.escuela.application.usecase.ObtenerEscuelaPorCodigoUseCase;
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

    @MockitoBean
    private ObtenerEscuelaPorCodigoUseCase obtenerEscuelaPorCodigo;

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

    @Test
    void obtenerPorCodigo_conEscuelaSuspendida_respondeActivaFalsaSinAutenticacion() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(obtenerEscuelaPorCodigo.ejecutar(codigo))
            .thenReturn(new EscuelaEstadoOutput(codigo, "escuelaexample", "Escuela Example", false));

        mockMvc.perform(get("/api/v1/escuelas/por-codigo/" + codigo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.codigo").value(codigo.toString()))
            .andExpect(jsonPath("$.data.activa").value(false))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void obtenerPorCodigo_conCodigoInexistente_responde404ConErrEsc02() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(obtenerEscuelaPorCodigo.ejecutar(codigo))
            .thenThrow(new NotFoundException(EscuelaErrorCodes.ESCUELA_NO_ENCONTRADA_POR_CODIGO, "no existe"));

        mockMvc.perform(get("/api/v1/escuelas/por-codigo/" + codigo))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ESC_02"));
    }

    @Test
    void obtenerPorCodigo_conCodigoQueNoEsUuid_responde400ConErrSys01() throws Exception {
        mockMvc.perform(get("/api/v1/escuelas/por-codigo/no-es-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }
}
