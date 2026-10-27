package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

/**
 * Las rutas internas de la billetera (CU44) contra un Postgres real: descontar y devolver sin sesión, con la misma
 * clave dos veces. Si no hay Docker, se salta.
 */
@SpringBootTest(properties = "app.internal.credencial-academico=credencial-de-integracion")
@AutoConfigureMockMvc
class BilleteraInternaIntegracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CLASE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PagoRepository pagos;
    @Autowired
    private PlanPagoRepository planes;
    @Autowired
    private BilleteraService billetera;
    @Autowired
    private PlatformTransactionManager transacciones;

    @BeforeEach
    void preparar() {
        vaciarTablas(jdbc);
        PlanPago plan = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan 10", (short) 10, new BigDecimal("450.00")));
        Pago pago = new Pago(ESCUELA, ESTUDIANTE, plan, new BigDecimal("38.23"), new BigDecimal("11.77"), "Ana",
            "Escuela", "clave-interna", Instant.now().plusSeconds(1800));
        pago.setEstado(EstadoPago.CONFIRMADO);
        Pago guardado = pagos.saveAndFlush(pago);
        new TransactionTemplate(transacciones).executeWithoutResult(estado ->
            billetera.acreditar(pagos.findById(guardado.getId()).orElseThrow()));
    }

    @AfterEach
    void limpiar() {
        vaciarTablas(jdbc);
    }

    private int saldo() {
        return jdbc.queryForObject("SELECT coalesce(sum(horas_disponibles), 0) FROM billetera_horas", Integer.class);
    }

    private ResultActions llamar(String ruta, String cuerpo, String clave) throws Exception {
        return mockMvc.perform(post(ruta).header("X-Service-Key", "credencial-de-integracion")
            .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String consumo() {
        return "{\"escuelaCodigo\":\"" + ESCUELA + "\",\"estudianteCodigo\":\"" + ESTUDIANTE
            + "\",\"horas\":1,\"instructorCodigo\":\"" + INSTRUCTOR + "\",\"claseCodigo\":\"" + CLASE + "\"}";
    }

    private static String devolucion() {
        return "{\"escuelaCodigo\":\"" + ESCUELA + "\",\"estudianteCodigo\":\"" + ESTUDIANTE
            + "\",\"claseCodigo\":\"" + CLASE + "\"}";
    }

    @Test
    void descontarDosVecesConLaMismaClave_bajaElSaldoUnaSolaVez() throws Exception {
        llamar("/internal/v1/billeteras-horas/consumos", consumo(), "reserva-1")
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.horasDisponibles").value(9));
        llamar("/internal/v1/billeteras-horas/consumos", consumo(), "reserva-1")
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.horasDisponibles").value(9));

        assertThat(saldo()).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas WHERE tipo = 'CONSUMO'",
            Integer.class)).isEqualTo(1);
    }

    @Test
    void devolver_dejaElSaldoComoAntes_yRepetirNoDevuelveDosVeces() throws Exception {
        llamar("/internal/v1/billeteras-horas/consumos", consumo(), "reserva-1").andExpect(status().isCreated());

        llamar("/internal/v1/billeteras-horas/devoluciones", devolucion(), "devolucion-1")
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.horasDevueltas").value(1));
        llamar("/internal/v1/billeteras-horas/devoluciones", devolucion(), "devolucion-1")
            .andExpect(status().isOk());

        assertThat(saldo()).isEqualTo(10);
    }

    @Test
    void devolverUnaClaseQueNoGasto_esErrPag16() throws Exception {
        llamar("/internal/v1/billeteras-horas/devoluciones", devolucion(), "devolucion-1")
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PAG_16"));
    }

    @Test
    void sinHorasSuficientes_esErrPag09_yNoCambiaElSaldo() throws Exception {
        String muchas = consumo().replace("\"horas\":1", "\"horas\":11");

        llamar("/internal/v1/billeteras-horas/consumos", muchas, "reserva-1")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_PAG_09"));

        assertThat(saldo()).isEqualTo(10);
    }

    @Test
    void sinLaCredencial_nadaPasa() throws Exception {
        mockMvc.perform(post("/internal/v1/billeteras-horas/consumos").header("Idempotency-Key", "reserva-1")
                .contentType(MediaType.APPLICATION_JSON).content(consumo()))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("ERR_PAG_19"));

        assertThat(saldo()).isEqualTo(10);
    }
}
