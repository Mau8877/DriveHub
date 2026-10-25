package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.drivehub.pagos.common.exception.BusinessException;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.features.billetera.service.BilleteraService;
import com.drivehub.pagos.features.pagos.enums.MotivoReembolso;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.TransaccionPasarela;
import com.drivehub.pagos.features.pagos.pasarela.AvisoPasarela;
import com.drivehub.pagos.features.pagos.pasarela.DevolucionCreada;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.pasarela.SolicitudDevolucion;
import com.drivehub.pagos.features.pagos.pasarela.TipoAviso;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.TransaccionPasarelaRepository;
import com.drivehub.pagos.features.pagos.service.CompensacionesPendientesJob;
import com.drivehub.pagos.features.pagos.service.ConfirmarPagoService;
import com.drivehub.pagos.features.pagos.service.ReversionService;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

/**
 * El reembolso y la compensación (CU31) de punta a punta contra un Postgres real: la pasarela es un doble, todo lo
 * demás es el servicio de verdad (confirmar el pago, la billetera, las reversiones, el comprobante). Si no hay
 * Docker, se salta.
 */
@SpringBootTest
class ReversionIntegracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final String SESION = "cs_test_sesion";
    private static final String COBRO = "pi_test_cobro";

    @MockitoBean
    private PasarelaPago pasarela;

    @Autowired
    private ReversionService reversionService;
    @Autowired
    private ConfirmarPagoService confirmarPagoService;
    @Autowired
    private BilleteraService billeteraService;
    @Autowired
    private CompensacionesPendientesJob jobDeCompensaciones;
    @Autowired
    private PagoRepository pagos;
    @Autowired
    private TransaccionPasarelaRepository transacciones;
    @Autowired
    private PlanPagoRepository planes;
    @Autowired
    private JdbcTemplate jdbc;

    private Pago pago;

    @BeforeEach
    void preparar() {
        vaciarTablas(jdbc);
        when(pasarela.reembolsar(any(SolicitudDevolucion.class))).thenAnswer(
            i -> new DevolucionCreada("re_" + UUID.randomUUID(), "pending"));
        PlanPago plan = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan 10", (short) 10, new BigDecimal("450.00")));
        pago = pagos.saveAndFlush(new Pago(ESCUELA, ESTUDIANTE, plan, new BigDecimal("38.23"),
            new BigDecimal("11.77"), "Ana", "Escuela", "clave-1", Instant.now().plusSeconds(1800)));
        TransaccionPasarela transaccion = new TransaccionPasarela(pago, "STRIPE", "CREADA");
        transaccion.setReferenciaExterna(SESION);
        transacciones.saveAndFlush(transaccion);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        vaciarTablas(jdbc);
    }

    private static void comoEstudiante() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(ESTUDIANTE.toString(), "x", "ROLE_ESTUDIANTE"));
    }

    private static void comoInstructor() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(INSTRUCTOR.toString(), "x", "ROLE_INSTRUCTOR"));
    }

    private void cobrar(long centavos) {
        confirmarPagoService.procesar(new AvisoPasarela("evt_1", TipoAviso.COBRO_COMPLETADO, SESION,
            pago.getCodigo(), centavos, "usd", COBRO));
    }

    private String estadoDelPago() {
        return jdbc.queryForObject("SELECT estado FROM pago WHERE id = ?", String.class, pago.getId());
    }

    private int saldo() {
        return jdbc.queryForObject("SELECT coalesce(sum(horas_disponibles), 0) FROM billetera_horas", Integer.class);
    }

    private int horasRestantes() {
        return jdbc.queryForObject("SELECT horas_restantes FROM pago WHERE id = ?", Integer.class, pago.getId());
    }

    private int reversiones(String where) {
        return jdbc.queryForObject("SELECT count(*) FROM reversion_pago WHERE " + where, Integer.class);
    }

    private UUID codigoDeLaReversion() {
        return jdbc.queryForObject("SELECT codigo_reversion_pago FROM reversion_pago", UUID.class);
    }

    private AvisoPasarela avisoDeDevolucion(TipoAviso tipo) {
        String referencia = jdbc.queryForObject("SELECT referencia_externa FROM reversion_pago", String.class);
        return new AvisoPasarela("evt_9", tipo, referencia, pago.getCodigo(), 3632L, "usd", COBRO,
            codigoDeLaReversion());
    }

    // ------------------------------------------------------------------ reembolso del estudiante

    @Test
    void elReembolso_deUnaCompraIntacta_vaDeLaCompraAlPagoReembolsadoConSusDosComprobantes() {
        cobrar(3823);
        assertThat(estadoDelPago()).isEqualTo("CONFIRMADO");
        assertThat(saldo()).isEqualTo(10);
        comoEstudiante();

        var detalle = reversionService.solicitarReembolso(ESCUELA, ESTUDIANTE, pago.getCodigo(),
            MotivoReembolso.MUDANZA, null);

        assertThat(detalle.estado().name()).isEqualTo("REEMBOLSO_EN_PROCESO");
        assertThat(saldo()).isZero();
        assertThat(horasRestantes()).isZero();
        assertThat(reversiones("estado = 'SOLICITADA' AND origen = 'ESTUDIANTE' AND referencia_externa IS NOT NULL"))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT monto FROM reversion_pago", BigDecimal.class))
            .isEqualByComparingTo("36.32");
        assertThat(jdbc.queryForObject("SELECT monto_retenido FROM reversion_pago", BigDecimal.class))
            .isEqualByComparingTo("1.91");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transaccion_pasarela WHERE proveedor = 'STRIPE_REEMBOLSO'",
            Integer.class)).isEqualTo(1);

        confirmarPagoService.procesar(avisoDeDevolucion(TipoAviso.REEMBOLSO_EJECUTADO));

        assertThat(estadoDelPago()).isEqualTo("REVERTIDO");
        assertThat(reversiones("estado = 'EJECUTADA' AND ejecutado_en IS NOT NULL")).isEqualTo(1);
        assertThat(saldo()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE tipo = 'pago.revertido'",
            Integer.class)).isEqualTo(1);
        // * Los dos comprobantes quedan creados; sin MinIO su PDF queda pendiente y se reintenta
        assertThat(jdbc.queryForList("SELECT tipo FROM comprobante ORDER BY id", String.class))
            .containsExactly("PAGO", "REEMBOLSO");
    }

    @Test
    void elMismoAvisoDeDevolucion_otraVez_noCambiaNada() {
        cobrar(3823);
        comoEstudiante();
        reversionService.solicitarReembolso(ESCUELA, ESTUDIANTE, pago.getCodigo(), MotivoReembolso.SIN_TIEMPO, null);
        AvisoPasarela aviso = avisoDeDevolucion(TipoAviso.REEMBOLSO_EJECUTADO);

        confirmarPagoService.procesar(aviso);
        confirmarPagoService.procesar(aviso);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE tipo = 'pago.revertido'",
            Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM comprobante WHERE tipo = 'REEMBOLSO'", Integer.class))
            .isEqualTo(1);
    }

    @Test
    void siLaDevolucionFalla_lasHorasVuelven_yLaCompraSigueConfirmadaYReembolsable() {
        cobrar(3823);
        comoEstudiante();
        reversionService.solicitarReembolso(ESCUELA, ESTUDIANTE, pago.getCodigo(), MotivoReembolso.OTRO,
            "Perdí mi trabajo");

        confirmarPagoService.procesar(avisoDeDevolucion(TipoAviso.REEMBOLSO_FALLIDO));

        assertThat(estadoDelPago()).isEqualTo("CONFIRMADO");
        assertThat(saldo()).isEqualTo(10);
        assertThat(horasRestantes()).isEqualTo(10);
        assertThat(reversiones("estado = 'FALLIDA' AND motivo = 'OTRO' AND motivo_detalle = 'Perdí mi trabajo'"))
            .isEqualTo(1);
        // * Una fallida no estorba: se puede volver a pedir
        reversionService.solicitarReembolso(ESCUELA, ESTUDIANTE, pago.getCodigo(), MotivoReembolso.MUDANZA, null);
        assertThat(reversiones("estado = 'SOLICITADA'")).isEqualTo(1);
        assertThat(saldo()).isZero();
    }

    @Test
    void siLaPasarelaNoResponde_noQuedaNingunaReversionVigente_yElSaldoSeConserva() {
        cobrar(3823);
        comoEstudiante();
        when(pasarela.reembolsar(any(SolicitudDevolucion.class)))
            .thenThrow(new ServicioNoDisponibleException("ERR_PAG_06", "sin pasarela"));

        try {
            reversionService.solicitarReembolso(ESCUELA, ESTUDIANTE, pago.getCodigo(), MotivoReembolso.MUDANZA, null);
        } catch (ServicioNoDisponibleException esperada) {
            assertThat(esperada.getErrorCode()).isEqualTo("ERR_PAG_06");
        }

        assertThat(reversiones("estado = 'FALLIDA'")).isEqualTo(1);
        assertThat(reversiones("estado <> 'FALLIDA'")).isZero();
        assertThat(saldo()).isEqualTo(10);
        assertThat(horasRestantes()).isEqualTo(10);
    }

    @Test
    void dosPedidosSimultaneosDelMismoReembolso_dejanUnaSolaReversion() throws Exception {
        cobrar(3823);

        List<Object> resultados = enParalelo(
            () -> pedirReembolso(), () -> pedirReembolso());

        assertThat(resultados.stream().filter(r -> !(r instanceof BusinessException))).hasSize(1);
        assertThat(resultados.stream().filter(r -> r instanceof BusinessException)
            .map(r -> ((BusinessException) r).getErrorCode())).containsExactly("ERR_PAG_10");
        assertThat(reversiones("estado <> 'FALLIDA'")).isEqualTo(1);
        assertThat(saldo()).isZero();
        verify(pasarela, times(1)).reembolsar(any());
    }

    @Test
    void unConsumoYUnReembolsoSimultaneos_ganaUno_yNuncaQuedaSaldoNegativo() throws Exception {
        cobrar(3823);

        List<Object> resultados = enParalelo(
            () -> pedirReembolso(),
            () -> {
                comoInstructor();
                try {
                    return billeteraService.consumir(ESCUELA, INSTRUCTOR, ESTUDIANTE, 3, INSTRUCTOR, UUID.randomUUID(),
                        "consumo-1");
                } catch (BusinessException e) {
                    return e;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });

        assertThat(resultados.stream().filter(r -> !(r instanceof BusinessException))).hasSize(1);
        assertThat(resultados.stream().filter(r -> r instanceof BusinessException)
            .map(r -> ((BusinessException) r).getErrorCode())).containsAnyOf("ERR_PAG_11", "ERR_PAG_09");
        assertThat(saldo()).isBetween(0, 10);
        assertThat(horasRestantes()).isBetween(0, 10);
        // * Lo que queda en la billetera es exactamente lo que queda en la compra
        assertThat(saldo()).isEqualTo(horasRestantes());
    }

    // ------------------------------------------------------------------ compensación automática

    @Test
    void unCobroConUnMontoDistinto_terminaEnUnaReversionDelTotal_yElAvisoLaEjecuta() {
        cobrar(9999);

        assertThat(estadoDelPago()).isEqualTo("FALLIDO");
        assertThat(reversiones("estado = 'SOLICITADA' AND origen = 'SAGA' AND motivo = 'MONTO_DISTINTO'"
            + " AND referencia_externa IS NOT NULL")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT monto FROM reversion_pago", BigDecimal.class))
            .isEqualByComparingTo("38.23");
        assertThat(jdbc.queryForObject("SELECT monto_retenido FROM reversion_pago", BigDecimal.class))
            .isEqualByComparingTo("0");
        assertThat(saldo()).isZero();

        confirmarPagoService.procesar(avisoDeDevolucion(TipoAviso.REEMBOLSO_EJECUTADO));

        assertThat(estadoDelPago()).isEqualTo("REVERTIDO");
    }

    @Test
    void conLaPasarelaCaida_laCompensacionQuedaPendiente_yElReintentoLaCompletaUnaSolaVez() {
        when(pasarela.reembolsar(any(SolicitudDevolucion.class)))
            .thenThrow(new ServicioNoDisponibleException("ERR_PAG_06", "sin pasarela"));

        cobrar(9999);

        assertThat(reversiones("estado = 'SOLICITADA' AND origen = 'SAGA' AND referencia_externa IS NULL"))
            .isEqualTo(1);

        when(pasarela.reembolsar(any(SolicitudDevolucion.class))).thenAnswer(
            i -> new DevolucionCreada("re_" + UUID.randomUUID(), "pending"));
        jobDeCompensaciones.ejecutar();
        jobDeCompensaciones.ejecutar();

        assertThat(reversiones("estado = 'SOLICITADA' AND referencia_externa IS NOT NULL")).isEqualTo(1);
        assertThat(reversiones("1 = 1")).isEqualTo(1);
        // * La primera llamada falló, la segunda lo logró; la tercera vuelta ya no encontró nada pendiente
        verify(pasarela, times(2)).reembolsar(any());
    }

    @Test
    void elMismoCobroTardioRepetido_noCreaOtraCompensacion() {
        confirmarPagoService.procesar(new AvisoPasarela("evt_c", TipoAviso.COBRO_VENCIDO, SESION, pago.getCodigo(),
            null, null, null));
        assertThat(estadoDelPago()).isEqualTo("FALLIDO");

        cobrar(3823);
        cobrar(3823);

        assertThat(reversiones("origen = 'SAGA' AND motivo = 'COBRO_TARDIO'")).isEqualTo(1);
        verify(pasarela, times(1)).reembolsar(any());
        verify(pasarela, never()).crearSesionCobro(any());
    }

    // ------------------------------------------------------------------ utilidades

    private Object pedirReembolso() {
        comoEstudiante();
        try {
            return reversionService.solicitarReembolso(ESCUELA, ESTUDIANTE, pago.getCodigo(),
                MotivoReembolso.MUDANZA, null);
        } catch (BusinessException e) {
            return e;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** Corre las tareas a la vez (todas esperan la misma señal de largada) y devuelve lo que cada una respondió. */
    @SafeVarargs
    private static List<Object> enParalelo(Callable<Object>... tareas) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(tareas.length);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Object>> futuros = new ArrayList<>();
            for (Callable<Object> tarea : tareas) {
                futuros.add(hilos.submit(() -> {
                    largada.await();
                    return tarea.call();
                }));
            }
            largada.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }
}
