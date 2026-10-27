package com.drivehub.pagos.features.billetera;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.billetera.controller.BilleteraInternaController;
import com.drivehub.pagos.features.billetera.dto.ConsumoResultado;
import com.drivehub.pagos.features.billetera.dto.DevolucionResultado;
import com.drivehub.pagos.features.billetera.service.BilleteraService;

/**
 * El contrato HTTP de las rutas internas de la billetera (CU44): la credencial de servicio en lugar del JWT, el sobre
 * estándar y los mismos errores que las rutas públicas. El service va mockeado.
 */
@WebMvcTest(BilleteraInternaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
@TestPropertySource(properties = "app.internal.credencial-academico=credencial-de-prueba")
class BilleteraInternaControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CLASE = UUID.randomUUID();
    private static final String CONSUMO = "{\"escuelaCodigo\":\"" + ESCUELA + "\",\"estudianteCodigo\":\""
        + ESTUDIANTE + "\",\"horas\":1,\"instructorCodigo\":\"" + INSTRUCTOR + "\",\"claseCodigo\":\"" + CLASE + "\"}";
    private static final String DEVOLUCION = "{\"escuelaCodigo\":\"" + ESCUELA + "\",\"estudianteCodigo\":\""
        + ESTUDIANTE + "\",\"claseCodigo\":\"" + CLASE + "\"}";
    private static final String RUTA_CONSUMOS = "/internal/v1/billeteras-horas/consumos";
    private static final String RUTA_DEVOLUCIONES = "/internal/v1/billeteras-horas/devoluciones";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BilleteraService servicio;

    private ResultActions enviar(String ruta, String cuerpo, String clave) throws Exception {
        MockHttpServletRequestBuilder peticion = post(ruta).header("X-Service-Key", "credencial-de-prueba")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (clave != null) {
            peticion.header("Idempotency-Key", clave);
        }
        return mockMvc.perform(peticion);
    }

    @Test
    void consumir_conLaCredencial_responde201ConElSobre_yLlamaAlServiceDeSistema() throws Exception {
        when(servicio.consumirDeSistema(ESCUELA, ESTUDIANTE, 1, INSTRUCTOR, CLASE, "reserva-1"))
            .thenReturn(new ConsumoResultado(1, 9));

        enviar(RUTA_CONSUMOS, CONSUMO, "reserva-1")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.horasDescontadas").value(1))
            .andExpect(jsonPath("$.data.horasDisponibles").value(9))
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.timestamp").exists());
        verify(servicio).consumirDeSistema(ESCUELA, ESTUDIANTE, 1, INSTRUCTOR, CLASE, "reserva-1");
    }

    @Test
    void devolver_conLaCredencial_responde200ConElSobre() throws Exception {
        when(servicio.devolverDeSistema(ESCUELA, ESTUDIANTE, CLASE, "devolucion-1"))
            .thenReturn(new DevolucionResultado(1, 10));

        enviar(RUTA_DEVOLUCIONES, DEVOLUCION, "devolucion-1")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.horasDevueltas").value(1))
            .andExpect(jsonPath("$.data.horasDisponibles").value(10));
    }

    @Test
    void sinLaCredencial_responde401ErrPag19_yNoLlegaAlService() throws Exception {
        mockMvc.perform(post(RUTA_CONSUMOS).contentType(MediaType.APPLICATION_JSON).content(CONSUMO)
                .header("Idempotency-Key", "reserva-1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_PAG_19"));
        verifyNoInteractions(servicio);
    }

    @Test
    void unJwtValidoSinLaCredencial_noSirve_responde401ErrPag19() throws Exception {
        mockMvc.perform(post(RUTA_DEVOLUCIONES).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(DEVOLUCION).header("Idempotency-Key", "devolucion-1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_PAG_19"));
        verifyNoInteractions(servicio);
    }

    @Test
    void cuerpoIncompleto_responde400ErrSys01_sinLlamarAlService() throws Exception {
        enviar(RUTA_CONSUMOS, "{\"estudianteCodigo\":\"" + ESTUDIANTE + "\",\"horas\":1}", "reserva-1")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        enviar(RUTA_DEVOLUCIONES, "{}", "devolucion-1")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(servicio);
    }

    @Test
    void horasFueraDeRango_responde400() throws Exception {
        String cero = CONSUMO.replace("\"horas\":1", "\"horas\":0");
        String muchas = CONSUMO.replace("\"horas\":1", "\"horas\":501");

        enviar(RUTA_CONSUMOS, cero, "reserva-1").andExpect(status().isBadRequest());
        enviar(RUTA_CONSUMOS, muchas, "reserva-1").andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    void sinHorasSuficientes_llegaConErrPag09() throws Exception {
        when(servicio.consumirDeSistema(any(), any(), anyInt(), any(), any(), anyString()))
            .thenThrow(new ConflictException("ERR_PAG_09", "No hay horas suficientes en la billetera"));

        enviar(RUTA_CONSUMOS, CONSUMO, "reserva-1")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_09"));
    }

    @Test
    void claseSinConsumo_llegaConErrPag16() throws Exception {
        when(servicio.devolverDeSistema(any(), any(), any(), anyString()))
            .thenThrow(new NotFoundException("ERR_PAG_16", "No hay horas gastadas en esa clase para devolver"));

        enviar(RUTA_DEVOLUCIONES, DEVOLUCION, "devolucion-1")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_PAG_16"));
    }
}
