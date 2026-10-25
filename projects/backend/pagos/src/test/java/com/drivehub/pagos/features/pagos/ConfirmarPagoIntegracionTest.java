package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;
import com.drivehub.pagos.features.pagos.pasarela.AvisoPasarela;
import com.drivehub.pagos.features.pagos.pasarela.TipoAviso;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.TransaccionPasarelaRepository;
import com.drivehub.pagos.features.pagos.service.ConfirmarPagoService;
import com.drivehub.pagos.features.pagos.service.evento.CompensacionRequerida;
import com.drivehub.pagos.features.pagos.service.evento.MotivoCompensacion;
import com.drivehub.pagos.features.pagos.service.evento.PagoConfirmado;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

/**
 * Confirmar el pago con los avisos de la pasarela contra un Postgres real (CU27). Si no hay Docker, se salta.
 */
@SpringBootTest
class ConfirmarPagoIntegracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final String SESION = "cs_test_sesion";
    private static final String COBRO = "pi_test_cobro";

    @TestConfiguration
    static class Escucha {
        final List<Object> recibidos = new ArrayList<>();

        @EventListener
        void alRecibir(PagoConfirmado evento) {
            recibidos.add(evento);
        }

        @EventListener
        void alRecibir(CompensacionRequerida evento) {
            recibidos.add(evento);
        }
    }

    @Autowired
    private ConfirmarPagoService servicio;
    @Autowired
    private PagoRepository pagos;
    @Autowired
    private TransaccionPasarelaRepository transacciones;
    @Autowired
    private PlanPagoRepository planes;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Escucha escucha;

    private Pago pago;

    @BeforeEach
    void preparar() {
        limpiar();
        escucha.recibidos.clear();
        PlanPago plan = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan 10", (short) 10, new BigDecimal("450.00")));
        pago = pagos.saveAndFlush(new Pago(ESCUELA, ESTUDIANTE, plan, new BigDecimal("38.23"),
            new BigDecimal("11.77"), "Ana", "Escuela", "clave-1", Instant.now().plusSeconds(1800)));
        TransaccionPasarela transaccion = new TransaccionPasarela(pago, "STRIPE", "CREADA");
        transaccion.setReferenciaExterna(SESION);
        transacciones.saveAndFlush(transaccion);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM movimiento_horas");
        jdbc.update("DELETE FROM billetera_horas");
        jdbc.update("DELETE FROM outbox_evento");
        jdbc.update("DELETE FROM transaccion_pasarela");
        jdbc.update("DELETE FROM pago");
        jdbc.update("DELETE FROM plan_pago");
    }

    private AvisoPasarela cobro(long centavos, String moneda) {
        return new AvisoPasarela("evt_1", TipoAviso.COBRO_COMPLETADO, SESION, pago.getCodigo(), centavos, moneda,
            COBRO);
    }

    private String estadoDelPago() {
        return jdbc.queryForObject("SELECT estado FROM pago WHERE id = ?", String.class, pago.getId());
    }

    private int saldo() {
        return jdbc.queryForObject("SELECT coalesce(sum(horas_disponibles), 0) FROM billetera_horas", Integer.class);
    }

    @Test
    void cobroCompletado_confirmaElPago_acreditaLasHoras_yEscribeElOutbox() {
        servicio.procesar(cobro(3823, "usd"));

        assertThat(estadoDelPago()).isEqualTo("CONFIRMADO");
        assertThat(saldo()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT horas_restantes FROM pago WHERE id = ?", Integer.class, pago.getId()))
            .isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT estado FROM transaccion_pasarela", String.class))
            .isEqualTo("COMPLETADA");
        assertThat(jdbc.queryForObject("SELECT payload->>'pagoCodigo' FROM outbox_evento WHERE tipo = 'pago.confirmado'",
            String.class)).isEqualTo(pago.getCodigo().toString());
        assertThat(escucha.recibidos).hasSize(1).first().isInstanceOf(PagoConfirmado.class);
    }

    @Test
    void elMismoAvisoDosVeces_acreditaUnaSolaVez() {
        servicio.procesar(cobro(3823, "usd"));
        servicio.procesar(cobro(3823, "usd"));

        assertThat(saldo()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas", Integer.class)).isEqualTo(1);
    }

    @Test
    void dosAvisosSimultaneos_acreditanUnaSolaVez() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> trabajos = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                trabajos.add(hilos.submit(() -> {
                    salida.await();
                    servicio.procesar(cobro(3823, "usd"));
                    return null;
                }));
            }
            salida.countDown();
            for (Future<?> trabajo : trabajos) {
                trabajo.get();
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(saldo()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas", Integer.class)).isEqualTo(1);
        assertThat(estadoDelPago()).isEqualTo("CONFIRMADO");
    }

    @Test
    void montoDistinto_dejaElPagoFallido_noAcredita_ypideDevolverElCobro() {
        servicio.procesar(cobro(100, "usd"));

        assertThat(estadoDelPago()).isEqualTo("FALLIDO");
        assertThat(saldo()).isZero();
        assertThat(escucha.recibidos).hasSize(1);
        CompensacionRequerida pedido = (CompensacionRequerida) escucha.recibidos.get(0);
        assertThat(pedido.motivo()).isEqualTo(MotivoCompensacion.MONTO_DISTINTO);
        assertThat(pedido.cobroReferencia()).isEqualTo(COBRO);
        assertThat(jdbc.queryForObject("SELECT respuesta->>'cobroReferencia' FROM transaccion_pasarela", String.class))
            .isEqualTo(COBRO);
    }

    @Test
    void monedaDistinta_seTrataComoMontoDistinto() {
        servicio.procesar(cobro(3823, "eur"));

        assertThat(estadoDelPago()).isEqualTo("FALLIDO");
        assertThat(saldo()).isZero();
    }

    @Test
    void cobroDeUnPagoYaFallido_pideDevolverElCobro_sinAcreditar() {
        jdbc.update("UPDATE pago SET estado = 'FALLIDO' WHERE id = ?", pago.getId());

        servicio.procesar(cobro(3823, "usd"));

        assertThat(estadoDelPago()).isEqualTo("FALLIDO");
        assertThat(saldo()).isZero();
        assertThat(((CompensacionRequerida) escucha.recibidos.get(0)).motivo())
            .isEqualTo(MotivoCompensacion.COBRO_TARDIO);
    }

    @Test
    void avisoVencido_dejaElPagoFallido_ySiYaEstabaConfirmadoNoLoToca() {
        AvisoPasarela vencido = new AvisoPasarela("evt_2", TipoAviso.COBRO_VENCIDO, SESION, pago.getCodigo(), null,
            null, null);
        servicio.procesar(vencido);
        assertThat(estadoDelPago()).isEqualTo("FALLIDO");
        assertThat(jdbc.queryForObject("SELECT estado FROM transaccion_pasarela", String.class)).isEqualTo("VENCIDA");

        jdbc.update("UPDATE pago SET estado = 'CONFIRMADO' WHERE id = ?", pago.getId());
        servicio.procesar(vencido);
        assertThat(estadoDelPago()).isEqualTo("CONFIRMADO");
    }

    @Test
    void cobroDeUnaSesionDesconocida_seIgnora() {
        servicio.procesar(new AvisoPasarela("evt_3", TipoAviso.COBRO_COMPLETADO, "cs_otra", null, 3823L, "usd", COBRO));

        assertThat(estadoDelPago()).isEqualTo("PENDIENTE");
        assertThat(escucha.recibidos).isEmpty();
    }

    @Test
    void avisoDeOtroTipo_seIgnora() {
        servicio.procesar(AvisoPasarela.otro("evt_4"));

        assertThat(estadoDelPago()).isEqualTo("PENDIENTE");
    }

    @Test
    void siNoSePuedenAcreditarLasHoras_elPagoNoQuedaConfirmado_ySePideDevolverElCobro() {
        // * Una billetera con el tope de horas lleno fuerza el fallo de la acreditación sin tocar el código
        jdbc.update("""
            INSERT INTO billetera_horas (codigo_billetera_horas, escuela_codigo, estudiante_codigo, horas_disponibles)
            VALUES (?, ?, ?, 0)""", UUID.randomUUID(), ESCUELA, ESTUDIANTE);
        jdbc.execute("ALTER TABLE movimiento_horas ADD CONSTRAINT tmp_falla CHECK (tipo <> 'ACREDITACION')");
        try {
            servicio.procesar(cobro(3823, "usd"));
        } finally {
            jdbc.execute("ALTER TABLE movimiento_horas DROP CONSTRAINT tmp_falla");
        }

        assertThat(estadoDelPago()).isEqualTo("FALLIDO");
        assertThat(saldo()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento", Integer.class)).isZero();
        assertThat(escucha.recibidos).hasSize(1);
        assertThat(((CompensacionRequerida) escucha.recibidos.get(0)).motivo())
            .isEqualTo(MotivoCompensacion.ACREDITACION_FALLIDA);
    }
}
