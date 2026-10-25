package com.drivehub.pagos.features.billetera;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.GlobalExceptionHandler;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ValidationException;
import com.drivehub.pagos.config.SecurityConfig;
import com.drivehub.pagos.config.WebMvcConfig;
import com.drivehub.pagos.features.billetera.controller.BilleteraController;
import com.drivehub.pagos.features.billetera.dto.ConsumoResultado;
import com.drivehub.pagos.features.billetera.dto.DevolucionResultado;
import com.drivehub.pagos.features.billetera.service.BilleteraService;

/**
 * El contrato HTTP de la billetera de horas (CU27): status, forma del sobre y validación. El service va mockeado: su
 * lógica y el rol (`@PreAuthorize`) se prueban en sus tests.
 */
@WebMvcTest(BilleteraController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class BilleteraControllerTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CLASE = UUID.randomUUID();
    private static final String CONSUMO = "{\"estudianteCodigo\":\"" + ESTUDIANTE + "\",\"horas\":2,"
        + "\"instructorCodigo\":\"" + INSTRUCTOR + "\",\"claseCodigo\":\"" + CLASE + "\"}";
    private static final String DEVOLUCION = "{\"estudianteCodigo\":\"" + ESTUDIANTE + "\",\"claseCodigo\":\""
        + CLASE + "\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BilleteraService servicio;

    private static JwtRequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(USUARIO.toString()).claim("escuela", ESCUELA.toString()));
    }

    @Test
    void miSaldo_respondeLasHorasDelEstudianteDelToken() throws Exception {
        when(servicio.miSaldo(ESCUELA, USUARIO)).thenReturn(7);

        mockMvc.perform(get("/api/v1/billeteras-horas/mia").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.horasDisponibles").value(7))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void saldoDeUnEstudiante_usaLaEscuelaDelToken() throws Exception {
        when(servicio.saldoDe(ESCUELA, ESTUDIANTE)).thenReturn(3);

        mockMvc.perform(get("/api/v1/billeteras-horas/" + ESTUDIANTE + "/saldo").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.horasDisponibles").value(3));
    }

    @Test
    void sinSesion_ningunEndpointRespondeSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/billeteras-horas/mia")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/billeteras-horas/" + ESTUDIANTE + "/saldo")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/billeteras-horas/consumos").header("Idempotency-Key", "k")
            .contentType(MediaType.APPLICATION_JSON).content(CONSUMO)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/billeteras-horas/devoluciones").header("Idempotency-Key", "k")
            .contentType(MediaType.APPLICATION_JSON).content(DEVOLUCION)).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void conUnTokenSinEscuela_respondeUnError() throws Exception {
        mockMvc.perform(get("/api/v1/billeteras-horas/mia").with(jwt().jwt(j -> j.subject(USUARIO.toString()))))
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.status").value("failed"));
        verifyNoInteractions(servicio);
    }

    @Test
    void consumir_responde201ConLoDescontado() throws Exception {
        when(servicio.consumir(ESCUELA, USUARIO, ESTUDIANTE, 2, INSTRUCTOR, CLASE, "clave-1"))
            .thenReturn(new ConsumoResultado(2, 8));

        mockMvc.perform(post("/api/v1/billeteras-horas/consumos").with(sesion()).header("Idempotency-Key", "clave-1")
                .contentType(MediaType.APPLICATION_JSON).content(CONSUMO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.horasDescontadas").value(2))
            .andExpect(jsonPath("$.data.horasDisponibles").value(8));
    }

    @Test
    void consumir_sinElHeaderDeIdempotencia_pasaNuloYElServiceLoRechazaConErrSys01() throws Exception {
        when(servicio.consumir(any(), any(), any(), anyInt(), any(), any(), isNull())).thenThrow(
            new ValidationException(PagosErrorCodes.VALIDACION, "Falta la clave de idempotencia"));

        mockMvc.perform(post("/api/v1/billeteras-horas/consumos").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content(CONSUMO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void consumir_conCuerpoInvalido_responde400ConErrSys01_ySinLlamarAlService() throws Exception {
        String sinHoras = "{\"estudianteCodigo\":\"" + ESTUDIANTE + "\",\"horas\":0,\"instructorCodigo\":\""
            + INSTRUCTOR + "\",\"claseCodigo\":\"" + CLASE + "\"}";

        mockMvc.perform(post("/api/v1/billeteras-horas/consumos").with(sesion()).header("Idempotency-Key", "k")
                .contentType(MediaType.APPLICATION_JSON).content(sinHoras))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        verifyNoInteractions(servicio);
    }

    @Test
    void consumir_sinHorasSuficientes_responde409ConErrPag09() throws Exception {
        when(servicio.consumir(eq(ESCUELA), eq(USUARIO), eq(ESTUDIANTE), eq(2), eq(INSTRUCTOR), eq(CLASE), any()))
            .thenThrow(new ConflictException(PagosErrorCodes.HORAS_INSUFICIENTES, "No tiene horas suficientes"));

        mockMvc.perform(post("/api/v1/billeteras-horas/consumos").with(sesion()).header("Idempotency-Key", "k")
                .contentType(MediaType.APPLICATION_JSON).content(CONSUMO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_PAG_09"));
    }

    @Test
    void devolver_responde200ConLoDevuelto() throws Exception {
        when(servicio.devolver(ESCUELA, USUARIO, ESTUDIANTE, CLASE, "clave-2"))
            .thenReturn(new DevolucionResultado(2, 10));

        mockMvc.perform(post("/api/v1/billeteras-horas/devoluciones").with(sesion())
                .header("Idempotency-Key", "clave-2").contentType(MediaType.APPLICATION_JSON).content(DEVOLUCION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.horasDevueltas").value(2))
            .andExpect(jsonPath("$.data.horasDisponibles").value(10));
    }

    @Test
    void devolver_unaClaseSinConsumo_responde404ConErrPag16() throws Exception {
        when(servicio.devolver(any(), any(), any(), any(), any())).thenThrow(
            new NotFoundException(PagosErrorCodes.CONSUMO_NO_ENCONTRADO, "Esa clase no gastó horas"));

        mockMvc.perform(post("/api/v1/billeteras-horas/devoluciones").with(sesion()).header("Idempotency-Key", "k")
                .contentType(MediaType.APPLICATION_JSON).content(DEVOLUCION))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_PAG_16"));
    }

    @Test
    void devolver_conCuerpoInvalido_responde400() throws Exception {
        mockMvc.perform(post("/api/v1/billeteras-horas/devoluciones").with(sesion()).header("Idempotency-Key", "k")
                .contentType(MediaType.APPLICATION_JSON).content("{\"estudianteCodigo\":\"" + ESTUDIANTE + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }
}
