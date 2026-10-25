package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * Acreditar horas contra un Postgres real (CU27): idempotencia y concurrencia. Si no hay Docker, se salta.
 */
@SpringBootTest
class BilleteraIntegracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    @Autowired
    private BilleteraService billetera;
    @Autowired
    private PagoRepository pagos;
    @Autowired
    private PlanPagoRepository planes;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    private PlanPago plan;

    @BeforeEach
    void preparar() {
        limpiar();
        plan = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan 10", (short) 10, new BigDecimal("450.00")));
    }

    @AfterEach
    void limpiar() {
        vaciarTablas(jdbc);
    }

    private Long pagoConfirmado(String clave, UUID estudiante) {
        Pago pago = new Pago(ESCUELA, estudiante, plan, new BigDecimal("38.23"), new BigDecimal("11.77"), "Ana",
            "Escuela", clave, Instant.now().plusSeconds(1800));
        pago.setEstado(EstadoPago.CONFIRMADO);
        return pagos.saveAndFlush(pago).getId();
    }

    /** Acredita en su propia transacción, como lo hace la confirmación del pago. */
    private boolean acreditar(Long pagoId) {
        return new TransactionTemplate(transacciones).execute(estado -> billetera.acreditar(pagos.findById(pagoId).orElseThrow()));
    }

    private int saldoEnBase() {
        return jdbc.queryForObject("SELECT coalesce(sum(horas_disponibles), 0) FROM billetera_horas", Integer.class);
    }

    @Test
    void laPrimeraCompra_creaLaBilleteraYElMovimiento_yDejaLasHorasRestantesEnElPago() {
        Long pagoId = pagoConfirmado("p1", ESTUDIANTE);

        assertThat(acreditar(pagoId)).isTrue();

        assertThat(billetera.saldo(ESCUELA, ESTUDIANTE)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas WHERE tipo = 'ACREDITACION'",
            Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT horas_restantes FROM pago WHERE id = ?", Integer.class, pagoId))
            .isEqualTo(10);
    }

    @Test
    void elMismoPagoDosVeces_acreditaUnaSolaVez() {
        Long pagoId = pagoConfirmado("p1", ESTUDIANTE);

        assertThat(acreditar(pagoId)).isTrue();
        assertThat(acreditar(pagoId)).isFalse();

        assertThat(billetera.saldo(ESCUELA, ESTUDIANTE)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas", Integer.class)).isEqualTo(1);
    }

    @Test
    void dosCompras_sumanElSaldo() {
        acreditar(pagoConfirmado("p1", ESTUDIANTE));
        acreditar(pagoConfirmado("p2", ESTUDIANTE));

        assertThat(billetera.saldo(ESCUELA, ESTUDIANTE)).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM billetera_horas", Integer.class)).isEqualTo(1);
    }

    @Test
    void estudiantesDistintos_tienenBilleterasDistintas() {
        UUID otro = UUID.randomUUID();
        acreditar(pagoConfirmado("p1", ESTUDIANTE));
        acreditar(pagoConfirmado("p2", otro));

        assertThat(billetera.saldo(ESCUELA, ESTUDIANTE)).isEqualTo(10);
        assertThat(billetera.saldo(ESCUELA, otro)).isEqualTo(10);
        assertThat(billetera.saldo(UUID.randomUUID(), ESTUDIANTE)).isZero();
    }

    @Test
    void sinBilletera_elSaldoEsCero() {
        assertThat(billetera.saldo(ESCUELA, UUID.randomUUID())).isZero();
    }

    @Test
    void dosAcreditacionesSimultaneasDePagosDistintosDelMismoEstudiante_dejanLaSumaCorrectaYUnaSolaBilletera()
            throws Exception {
        Long a = pagoConfirmado("simultanea-a", ESTUDIANTE);
        Long b = pagoConfirmado("simultanea-b", ESTUDIANTE);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> acreditarA = () -> {
                salida.await();
                return acreditar(a);
            };
            Callable<Boolean> acreditarB = () -> {
                salida.await();
                return acreditar(b);
            };
            Future<Boolean> primero = hilos.submit(acreditarA);
            Future<Boolean> segundo = hilos.submit(acreditarB);
            salida.countDown();

            assertThat(primero.get()).isTrue();
            assertThat(segundo.get()).isTrue();
        } finally {
            hilos.shutdownNow();
        }

        assertThat(saldoEnBase()).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM billetera_horas", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas", Integer.class)).isEqualTo(2);
    }

    @Test
    void dosAcreditacionesSimultaneasDelMismoPago_acreditanUnaSolaVez() throws Exception {
        Long pagoId = pagoConfirmado("mismo-pago", ESTUDIANTE);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> acreditarPago = () -> {
                salida.await();
                return acreditar(pagoId);
            };
            Future<Boolean> primero = hilos.submit(acreditarPago);
            Future<Boolean> segundo = hilos.submit(acreditarPago);
            salida.countDown();

            assertThat(java.util.List.of(primero.get(), segundo.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(saldoEnBase()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas", Integer.class)).isEqualTo(1);
    }
}
