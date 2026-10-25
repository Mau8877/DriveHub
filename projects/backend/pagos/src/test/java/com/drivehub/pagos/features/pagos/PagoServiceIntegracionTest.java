package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
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
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.features.pagos.client.IdentidadClient;
import com.drivehub.pagos.features.pagos.pasarela.PasarelaPago;
import com.drivehub.pagos.features.pagos.pasarela.SesionCobro;
import com.drivehub.pagos.features.pagos.service.PagoService;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

/**
 * La compra (CU26) contra un Postgres real: la pasarela, Identidad y el token van falsos, todo lo demás es
 * real. Si no hay Docker, se salta.
 */
@SpringBootTest
class PagoServiceIntegracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final String URL_PAGO = "https://checkout.stripe.com/c/pay/cs_test_1";

    @Autowired
    private PagoService servicio;
    @Autowired
    private PlanPagoRepository planes;
    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private PasarelaPago pasarela;
    @MockitoBean
    private IdentidadClient identidad;
    @MockitoBean
    private BearerActual bearer;

    private UUID planCodigo;

    @BeforeEach
    void preparar() {
        vaciarTablas(jdbc);
        planCodigo = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan 10 horas", (short) 10, new BigDecimal("450.00")))
            .getCodigo();
        when(bearer.cabecera()).thenReturn("Bearer token");
        when(identidad.perfilPropio(any()))
            .thenReturn(new IdentidadClient.PerfilEstudiante("Ana Ruiz", "Escuela Example"));
        when(pasarela.crearSesionCobro(any()))
            .thenReturn(new SesionCobro("cs_test_1", URL_PAGO, Instant.now().plusSeconds(1800)));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static void iniciarSesionComoEstudiante() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("estudiante", "x", "ROLE_ESTUDIANTE"));
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    // * La base se comparte con los demas tests: sin esto, los pagos que quedan impiden borrar los planes
    @AfterEach
    void limpiarAlTerminar() {
        vaciarTablas(jdbc);
    }

    @Test
    void unaCompra_guardaElPagoPendienteConTodoCopiado_yLaSesionDeLaPasarela() {
        iniciarSesionComoEstudiante();

        PagoService.ResultadoIniciar resultado = servicio.iniciar(ESCUELA, ESTUDIANTE, planCodigo, "clave-1");

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.pago().urlPago()).isEqualTo(URL_PAGO);
        var fila = jdbc.queryForMap("SELECT * FROM pago WHERE idempotency_key = 'clave-1'");
        assertThat(fila.get("estado")).isEqualTo("PENDIENTE");
        assertThat(fila.get("concepto")).isEqualTo("HORAS");
        assertThat(fila.get("metodo")).isEqualTo("TARJETA");
        assertThat(fila.get("moneda")).isEqualTo("USD");
        assertThat(((BigDecimal) fila.get("monto"))).isEqualByComparingTo("38.23");
        assertThat(((BigDecimal) fila.get("monto_bs"))).isEqualByComparingTo("450.00");
        assertThat(fila.get("plan_nombre")).isEqualTo("Plan 10 horas");
        assertThat(fila.get("estudiante_nombre")).isEqualTo("Ana Ruiz");
        assertThat(fila.get("escuela_nombre")).isEqualTo("Escuela Example");
        assertThat(fila.get("escuela_codigo")).isEqualTo(ESCUELA);
        assertThat(contar("transaccion_pasarela")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT referencia_externa FROM transaccion_pasarela", String.class))
            .isEqualTo("cs_test_1");
    }

    @Test
    void dosComprasSimultaneasConLaMismaClave_dejanUnSoloPagoYUnaSolaSesion() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        Callable<PagoService.ResultadoIniciar> compra = () -> {
            iniciarSesionComoEstudiante();
            salida.await();
            return servicio.iniciar(ESCUELA, ESTUDIANTE, planCodigo, "clave-simultanea");
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<PagoService.ResultadoIniciar> a = hilos.submit(compra);
            Future<PagoService.ResultadoIniciar> b = hilos.submit(compra);
            salida.countDown();

            PagoService.ResultadoIniciar resultadoA = a.get();
            PagoService.ResultadoIniciar resultadoB = b.get();

            assertThat(resultadoA.pago().codigo()).isEqualTo(resultadoB.pago().codigo());
            assertThat(resultadoA.pago().urlPago()).isEqualTo(URL_PAGO);
            assertThat(resultadoB.pago().urlPago()).isEqualTo(URL_PAGO);
            assertThat(List.of(resultadoA.creado(), resultadoB.creado())).containsExactlyInAnyOrder(true, false);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(contar("pago")).isEqualTo(1);
        assertThat(contar("transaccion_pasarela")).isEqualTo(1);
    }

    @Test
    void siLaPasarelaFalla_elPagoQuedaFallido_yLaMismaClaveSePuedeReintentar() {
        iniciarSesionComoEstudiante();
        when(pasarela.crearSesionCobro(any())).thenThrow(
            new ServicioNoDisponibleException("ERR_PAG_06", "No pudimos conectar con el pago"));

        assertThatThrownBy(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, planCodigo, "clave-reintento"))
            .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(jdbc.queryForObject("SELECT estado FROM pago WHERE idempotency_key = 'clave-reintento'",
            String.class)).isEqualTo("FALLIDO");
        assertThat(contar("transaccion_pasarela")).isZero();

        doReturn(new SesionCobro("cs_test_2", URL_PAGO, Instant.now().plusSeconds(1800)))
            .when(pasarela).crearSesionCobro(any());
        PagoService.ResultadoIniciar reintento = servicio.iniciar(ESCUELA, ESTUDIANTE, planCodigo, "clave-reintento");

        assertThat(reintento.creado()).isFalse();
        assertThat(reintento.pago().urlPago()).isEqualTo(URL_PAGO);
        assertThat(jdbc.queryForObject("SELECT estado FROM pago WHERE idempotency_key = 'clave-reintento'",
            String.class)).isEqualTo("PENDIENTE");
        assertThat(contar("pago")).isEqualTo(1);
        assertThat(contar("transaccion_pasarela")).isEqualTo(1);
    }

    @Test
    void elEstadoDeUnPago_soloLoVeSuDueno() {
        iniciarSesionComoEstudiante();
        UUID codigo = servicio.iniciar(ESCUELA, ESTUDIANTE, planCodigo, "clave-estado").pago().codigo();

        assertThat(servicio.estado(ESCUELA, ESTUDIANTE, codigo).estado().name()).isEqualTo("PENDIENTE");
        assertThatThrownBy(() -> servicio.estado(ESCUELA, UUID.randomUUID(), codigo))
            .hasMessage("El pago no existe");
        assertThatThrownBy(() -> servicio.estado(UUID.randomUUID(), ESTUDIANTE, codigo))
            .hasMessage("El pago no existe");
    }

    @Test
    void sinElRolEstudiante_noSePuedeComprar() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("admin", "x", "ROLE_ADMIN_ESCUELA"));

        assertThatThrownBy(() -> servicio.iniciar(ESCUELA, ESTUDIANTE, planCodigo, "clave-rol"))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(contar("pago")).isZero();
    }
}
