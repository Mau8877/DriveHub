package com.drivehub.pagos.features.comprobantes;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.comprobantes.controller.ComprobanteController;
import com.drivehub.pagos.features.comprobantes.service.ComprobanteDescargaService;

/**
 * El contrato HTTP de la descarga del PDF (CU30): tipo, nombre del archivo, caché y errores con el sobre. El service
 * va mockeado: quién puede verlo se prueba en `ComprobanteServiceDescargaTest`.
 */
@WebMvcTest(ComprobanteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class ComprobanteControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID PERSONA = UUID.randomUUID();
    private static final UUID COMPROBANTE = UUID.randomUUID();
    private static final String RUTA = "/api/v1/comprobantes/" + COMPROBANTE + "/pdf";
    private static final byte[] PDF = {'%', 'P', 'D', 'F', 1, 2, 3};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComprobanteDescargaService servicio;

    private static JwtRequestPostProcessor sesion(String rol) {
        return jwt().jwt(j -> j.subject(PERSONA.toString()).claim("escuela", ESCUELA.toString()))
            .authorities(new SimpleGrantedAuthority(rol));
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void sinEscuelaEnElToken_responde403ErrPag04() throws Exception {
        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(PERSONA.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_PAG_04"));
        verifyNoInteractions(servicio);
    }

    @Test
    void elEstudiante_recibeElPdf_sinSobre_ysinCache() throws Exception {
        when(servicio.obtener(ESCUELA, PERSONA, false, COMPROBANTE))
            .thenReturn(new ComprobanteDescargaService.Pdf("CMP-2026-000184", PDF));

        mockMvc.perform(get(RUTA).with(sesion("ROLE_ESTUDIANTE")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", "inline; filename=\"CMP-2026-000184.pdf\""))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().bytes(PDF));
    }

    @Test
    void elInstructor_seIdentificaPorSuRol_alPedirElPdf() throws Exception {
        when(servicio.obtener(ESCUELA, PERSONA, true, COMPROBANTE))
            .thenReturn(new ComprobanteDescargaService.Pdf("RMB-2026-000185", PDF));

        mockMvc.perform(get(RUTA).with(sesion("ROLE_INSTRUCTOR")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "inline; filename=\"RMB-2026-000185.pdf\""))
            .andExpect(content().bytes(PDF));
    }

    @Test
    void noEsTuyoONoExiste_responde404ErrPag13_conElSobre() throws Exception {
        when(servicio.obtener(ESCUELA, PERSONA, false, COMPROBANTE))
            .thenThrow(new NotFoundException("ERR_PAG_13", "El comprobante no existe"));

        mockMvc.perform(get(RUTA).with(sesion("ROLE_ESTUDIANTE")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_PAG_13"));
    }

    @Test
    void sigueEnPreparacion_responde409ErrPag14() throws Exception {
        when(servicio.obtener(ESCUELA, PERSONA, false, COMPROBANTE))
            .thenThrow(new ConflictException("ERR_PAG_14", "Se está preparando"));

        mockMvc.perform(get(RUTA).with(sesion("ROLE_ESTUDIANTE")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_PAG_14"));
    }

    @Test
    void unCodigoQueNoEsUuid_responde400() throws Exception {
        mockMvc.perform(get("/api/v1/comprobantes/no-es-uuid/pdf").with(sesion("ROLE_ESTUDIANTE")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(servicio);
    }
}
