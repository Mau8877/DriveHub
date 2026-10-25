package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.drivehub.pagos.common.exception.BusinessException;
import com.drivehub.pagos.features.billetera.dto.ConsumoResultado;
import com.drivehub.pagos.features.billetera.dto.DevolucionResultado;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

/**
 * Gastar y devolver horas contra un Postgres real (CU27): el orden de las compras, la idempotencia y la
 * concurrencia. Si no hay Docker, se salta.
 */
@SpringBootTest
class BilleteraConsumoIntegracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID OTRO_INSTRUCTOR = UUID.randomUUID();

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
        plan = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan 5", (short) 5, new BigDecimal("275.00")));
        entrarComoAdministrador();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM movimiento_horas");
        jdbc.update("DELETE FROM billetera_horas");
        jdbc.update("DELETE FROM transaccion_pasarela");
        jdbc.update("DELETE FROM pago");
        jdbc.update("DELETE FROM plan_pago");
    }

    private static void entrarComoAdministrador() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN_ESCUELA"));
    }

    /** Una compra confirmada y acreditada de `horas` horas (la confirmación de CU27, simulada). */
    private Long compraAcreditada(String clave, int horas, Instant confirmadoEn) {
        PlanPago planDeCompra = horas == plan.getHoras() ? plan
            : planes.saveAndFlush(new PlanPago(ESCUELA, "Plan " + horas + " " + clave, (short) horas,
                new BigDecimal("100.00")));
        Pago pago = new Pago(ESCUELA, ESTUDIANTE, planDeCompra, new BigDecimal("23.34"), new BigDecimal("11.77"),
            "Ana", "Escuela", clave, confirmadoEn.plusSeconds(1800));
        pago.setEstado(EstadoPago.CONFIRMADO);
        pago.setConfirmadoEn(confirmadoEn);
        Long id = pagos.saveAndFlush(pago).getId();
        new TransactionTemplate(transacciones).executeWithoutResult(
            estado -> billetera.acreditar(pagos.findById(id).orElseThrow()));
        return id;
    }

    private ConsumoResultado consumir(int horas, UUID clase, String clave) {
        return billetera.consumir(ESCUELA, ESTUDIANTE, ESTUDIANTE, horas, INSTRUCTOR, clase, clave);
    }

    private int restantesDe(Long pagoId) {
        return jdbc.queryForObject("SELECT horas_restantes FROM pago WHERE id = ?", Integer.class, pagoId);
    }

    private int disponibles() {
        return jdbc.queryForObject("SELECT horas_disponibles FROM billetera_horas", Integer.class);
    }

    private int sumaDeRestantes() {
        return jdbc.queryForObject("SELECT coalesce(sum(horas_restantes), 0) FROM pago WHERE estado = 'CONFIRMADO'",
            Integer.class);
    }

    @Test
    void consumir_cruzandoDosCompras_gastaLaMasAntiguaPrimeroYGuardaDeQueCompraSaleCadaHora() {
        Long antigua = compraAcreditada("antigua", 5, Instant.parse("2026-10-01T10:00:00Z"));
        Long reciente = compraAcreditada("reciente", 5, Instant.parse("2026-10-02T10:00:00Z"));
        UUID clase = UUID.randomUUID();

        ConsumoResultado resultado = consumir(7, clase, "consumo-1");

        assertThat(resultado.horasDescontadas()).isEqualTo(7);
        assertThat(resultado.horasDisponibles()).isEqualTo(3);
        assertThat(restantesDe(antigua)).isZero();
        assertThat(restantesDe(reciente)).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT horas FROM movimiento_horas WHERE tipo = 'CONSUMO' ORDER BY horas DESC",
            Integer.class)).containsExactly(5, 2);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT instructor_codigo) FROM movimiento_horas"
            + " WHERE tipo = 'CONSUMO'", Integer.class)).isEqualTo(1);
        assertThat(sumaDeRestantes()).isEqualTo(disponibles());
    }

    @Test
    void consumir_unaCompraMasNuevaConfirmadaAntes_noAlteraElOrden() {
        Long confirmadaDespues = compraAcreditada("a", 5, Instant.parse("2026-10-05T10:00:00Z"));
        Long confirmadaAntes = compraAcreditada("b", 5, Instant.parse("2026-10-01T10:00:00Z"));

        consumir(3, UUID.randomUUID(), "consumo-1");

        assertThat(restantesDe(confirmadaAntes)).isEqualTo(2);
        assertThat(restantesDe(confirmadaDespues)).isEqualTo(5);
    }

    @Test
    void consumir_masDeLoQueHay_esErrPag09_yNoCambiaNada() {
        Long compra = compraAcreditada("a", 5, Instant.parse("2026-10-01T10:00:00Z"));

        assertThatThrownBy(() -> consumir(6, UUID.randomUUID(), "consumo-1"))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_09"));

        assertThat(disponibles()).isEqualTo(5);
        assertThat(restantesDe(compra)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas WHERE tipo = 'CONSUMO'", Integer.class))
            .isZero();
    }

    @Test
    void consumir_conLaMismaClave_noDescuentaDeNuevo() {
        compraAcreditada("a", 5, Instant.parse("2026-10-01T10:00:00Z"));
        compraAcreditada("b", 5, Instant.parse("2026-10-02T10:00:00Z"));
        UUID clase = UUID.randomUUID();

        ConsumoResultado primero = consumir(7, clase, "misma-clave");
        ConsumoResultado repetido = consumir(7, clase, "misma-clave");

        assertThat(repetido.horasDescontadas()).isEqualTo(7);
        assertThat(repetido.horasDisponibles()).isEqualTo(primero.horasDisponibles()).isEqualTo(3);
        assertThat(disponibles()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas WHERE tipo = 'CONSUMO'", Integer.class))
            .isEqualTo(2);
    }

    @Test
    void dosConsumosSimultaneosQueJuntosSuperanElSaldo_unoSeAceptaYElOtroSeRechaza_sinSaldoNegativo()
            throws Exception {
        compraAcreditada("a", 5, Instant.parse("2026-10-01T10:00:00Z"));
        compraAcreditada("b", 5, Instant.parse("2026-10-02T10:00:00Z"));
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<ConsumoResultado>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                String clave = "simultaneo-" + i;
                Callable<ConsumoResultado> consumo = () -> {
                    entrarComoAdministrador();
                    salida.await();
                    return consumir(7, UUID.randomUUID(), clave);
                };
                resultados.add(hilos.submit(consumo));
            }
            salida.countDown();

            int aceptados = 0;
            int rechazados = 0;
            for (Future<ConsumoResultado> futuro : resultados) {
                try {
                    futuro.get();
                    aceptados++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOfSatisfying(BusinessException.class,
                        b -> assertThat(b.getErrorCode()).isEqualTo("ERR_PAG_09"));
                    rechazados++;
                }
            }
            assertThat(aceptados).isEqualTo(1);
            assertThat(rechazados).isEqualTo(1);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(disponibles()).isEqualTo(3);
        assertThat(sumaDeRestantes()).isEqualTo(3);
    }

    @Test
    void devolver_restituyeALasMismasComprasYElSaldo_ySeVeConElInstructorCorrecto() {
        Long antigua = compraAcreditada("a", 5, Instant.parse("2026-10-01T10:00:00Z"));
        Long reciente = compraAcreditada("b", 5, Instant.parse("2026-10-02T10:00:00Z"));
        UUID clase = UUID.randomUUID();
        consumir(7, clase, "consumo-1");

        DevolucionResultado resultado = billetera.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, clase, "devolucion-1");

        assertThat(resultado.horasDevueltas()).isEqualTo(7);
        assertThat(resultado.horasDisponibles()).isEqualTo(10);
        assertThat(restantesDe(antigua)).isEqualTo(5);
        assertThat(restantesDe(reciente)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_horas WHERE tipo = 'DEVOLUCION'"
            + " AND instructor_codigo = ? AND clase_codigo = ?", Integer.class, INSTRUCTOR, clase)).isEqualTo(2);
        assertThat(sumaDeRestantes()).isEqualTo(disponibles());
    }

    @Test
    void devolver_dosVeces_conLaMismaClaveNoDevuelveDeMas_yConOtraClaveEsErrPag16() {
        compraAcreditada("a", 5, Instant.parse("2026-10-01T10:00:00Z"));
        UUID clase = UUID.randomUUID();
        consumir(4, clase, "consumo-1");

        billetera.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, clase, "devolucion-1");
        DevolucionResultado repetida = billetera.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, clase, "devolucion-1");

        assertThat(repetida.horasDevueltas()).isEqualTo(4);
        assertThat(disponibles()).isEqualTo(5);
        assertThatThrownBy(() -> billetera.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, clase, "devolucion-2"))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_16"));
        assertThat(disponibles()).isEqualTo(5);
    }

    @Test
    void devolver_unaClaseQueNuncaGasto_esErrPag16() {
        compraAcreditada("a", 5, Instant.parse("2026-10-01T10:00:00Z"));

        assertThatThrownBy(() -> billetera.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, UUID.randomUUID(), "d"))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_16"));
    }

    @Test
    void lasHorasGastadasConCadaInstructor_seCuentanPorSuCodigo() {
        compraAcreditada("a", 5, Instant.parse("2026-10-01T10:00:00Z"));
        UUID claseUno = UUID.randomUUID();
        billetera.consumir(ESCUELA, ESTUDIANTE, ESTUDIANTE, 2, INSTRUCTOR, claseUno, "c1");
        billetera.consumir(ESCUELA, ESTUDIANTE, ESTUDIANTE, 1, OTRO_INSTRUCTOR, UUID.randomUUID(), "c2");
        billetera.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, claseUno, "d1");

        Integer conElPrimero = jdbc.queryForObject("SELECT coalesce(sum(CASE tipo WHEN 'CONSUMO' THEN horas"
            + " WHEN 'DEVOLUCION' THEN -horas END), 0) FROM movimiento_horas WHERE instructor_codigo = ?",
            Integer.class, INSTRUCTOR);
        Integer conElOtro = jdbc.queryForObject("SELECT coalesce(sum(CASE tipo WHEN 'CONSUMO' THEN horas"
            + " WHEN 'DEVOLUCION' THEN -horas END), 0) FROM movimiento_horas WHERE instructor_codigo = ?",
            Integer.class, OTRO_INSTRUCTOR);

        assertThat(conElPrimero).isZero();
        assertThat(conElOtro).isEqualTo(1);
    }

    @Test
    void elSaldoSiempreEsLaSumaDeLasHorasRestantesDeLasCompras_trasCadaOperacion() {
        compraAcreditada("a", 5, Instant.parse("2026-10-01T10:00:00Z"));
        compraAcreditada("b", 5, Instant.parse("2026-10-02T10:00:00Z"));
        UUID clase = UUID.randomUUID();

        consumir(3, clase, "c1");
        assertThat(sumaDeRestantes()).isEqualTo(disponibles());
        consumir(4, UUID.randomUUID(), "c2");
        assertThat(sumaDeRestantes()).isEqualTo(disponibles());
        billetera.devolver(ESCUELA, ESTUDIANTE, ESTUDIANTE, clase, "d1");
        assertThat(sumaDeRestantes()).isEqualTo(disponibles());
        assertThat(disponibles()).isEqualTo(6);
    }
}
