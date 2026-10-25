package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.drivehub.pagos.features.billetera.enums.TipoMovimiento;
import com.drivehub.pagos.features.billetera.model.BilleteraHoras;
import com.drivehub.pagos.features.billetera.model.MovimientoHoras;
import com.drivehub.pagos.features.billetera.repository.BilleteraRepository;
import com.drivehub.pagos.features.billetera.repository.MovimientoHorasRepository;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

/**
 * Las agregaciones de las horas de los alumnos de un instructor (CU29) contra un Postgres real, con datos de dos
 * instructores y dos escuelas. Si no hay Docker, se salta.
 */
@SpringBootTest
class HorasAlumnosRepositoryTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID OTRA_ESCUELA = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID OTRO_INSTRUCTOR = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();

    @Autowired
    private PagoRepository pagos;
    @Autowired
    private BilleteraRepository billeteras;
    @Autowired
    private MovimientoHorasRepository movimientos;
    @Autowired
    private PlanPagoRepository planes;
    @Autowired
    private JdbcTemplate jdbc;

    private PlanPago plan;
    private PlanPago planDeOtraEscuela;
    private int clave;

    @BeforeEach
    void preparar() {
        limpiar();
        plan = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan", (short) 10, new BigDecimal("450.00")));
        planDeOtraEscuela = planes.saveAndFlush(new PlanPago(OTRA_ESCUELA, "Plan", (short) 10,
            new BigDecimal("450.00")));
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM movimiento_horas");
        jdbc.update("DELETE FROM billetera_horas");
        jdbc.update("DELETE FROM reversion_pago");
        jdbc.update("DELETE FROM transaccion_pasarela");
        jdbc.update("DELETE FROM pago");
        jdbc.update("DELETE FROM plan_pago");
    }

    private Pago pago(UUID escuela, UUID estudiante, EstadoPago estado, int horas) {
        PlanPago base = escuela.equals(ESCUELA) ? plan : planDeOtraEscuela;
        Pago pago = new Pago(escuela, estudiante, base, new BigDecimal("38.23"),
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO), "Ana", "Escuela", "clave-" + clave++,
            Instant.parse("2026-10-25T12:00:00Z"));
        pago.setEstado(estado);
        Pago guardado = pagos.saveAndFlush(pago);
        // * El pago copia las horas del plan (10): se cambia la fila para probar sumas con otros valores
        jdbc.update("UPDATE pago SET horas = ? WHERE id = ?", horas, guardado.getId());
        return guardado;
    }

    private BilleteraHoras billetera(UUID escuela, UUID estudiante, int horas) {
        BilleteraHoras billetera = new BilleteraHoras(escuela, estudiante);
        billetera.setHorasDisponibles(horas);
        return billeteras.saveAndFlush(billetera);
    }

    private void movimiento(BilleteraHoras billetera, Pago pago, TipoMovimiento tipo, int horas, UUID instructor,
            UUID clase) {
        movimientos.saveAndFlush(new MovimientoHoras(billetera, pago, tipo, horas, instructor, clase,
            "mov-" + clave++));
    }

    private static Map<UUID, Long> comoMapa(List<Object[]> filas) {
        Map<UUID, Long> mapa = new HashMap<>();
        filas.forEach(f -> mapa.put((UUID) f[0], ((Number) f[1]).longValue()));
        return mapa;
    }

    @Test
    void horasCompradas_sumanSoloLosPagosConfirmados_delEstudianteYLaEscuela() {
        pago(ESCUELA, ANA, EstadoPago.CONFIRMADO, 10);
        pago(ESCUELA, ANA, EstadoPago.CONFIRMADO, 5);
        pago(ESCUELA, ANA, EstadoPago.REVERTIDO, 20);
        pago(ESCUELA, ANA, EstadoPago.PENDIENTE, 7);
        pago(ESCUELA, ANA, EstadoPago.FALLIDO, 3);
        pago(ESCUELA, BETO, EstadoPago.CONFIRMADO, 8);
        pago(OTRA_ESCUELA, ANA, EstadoPago.CONFIRMADO, 99);

        Map<UUID, Long> compradas = comoMapa(pagos.horasCompradasPor(ESCUELA, List.of(ANA, BETO, UUID.randomUUID())));

        assertThat(compradas).containsEntry(ANA, 15L).containsEntry(BETO, 8L).hasSize(2);
    }

    @Test
    void horasGastadas_sumanConsumosYRestanDevoluciones_soloDelInstructorPedido() {
        Pago pagoAna = pago(ESCUELA, ANA, EstadoPago.CONFIRMADO, 20);
        Pago pagoBeto = pago(ESCUELA, BETO, EstadoPago.CONFIRMADO, 20);
        BilleteraHoras deAna = billetera(ESCUELA, ANA, 10);
        BilleteraHoras deBeto = billetera(ESCUELA, BETO, 10);
        movimiento(deAna, pagoAna, TipoMovimiento.ACREDITACION, 20, null, null);
        movimiento(deAna, pagoAna, TipoMovimiento.CONSUMO, 6, INSTRUCTOR, UUID.randomUUID());
        movimiento(deAna, pagoAna, TipoMovimiento.CONSUMO, 4, OTRO_INSTRUCTOR, UUID.randomUUID());
        UUID claseCancelada = UUID.randomUUID();
        movimiento(deAna, pagoAna, TipoMovimiento.CONSUMO, 2, INSTRUCTOR, claseCancelada);
        movimiento(deAna, pagoAna, TipoMovimiento.DEVOLUCION, 2, INSTRUCTOR, claseCancelada);
        movimiento(deBeto, pagoBeto, TipoMovimiento.CONSUMO, 3, OTRO_INSTRUCTOR, UUID.randomUUID());

        Map<UUID, Long> conInstructor = comoMapa(
            movimientos.horasGastadasConInstructor(ESCUELA, INSTRUCTOR, List.of(ANA, BETO)));
        Map<UUID, Long> conOtro = comoMapa(
            movimientos.horasGastadasConInstructor(ESCUELA, OTRO_INSTRUCTOR, List.of(ANA, BETO)));

        assertThat(conInstructor).containsEntry(ANA, 6L).doesNotContainKey(BETO);
        assertThat(conOtro).containsEntry(ANA, 4L).containsEntry(BETO, 3L);
        assertThat(movimientos.horasGastadasConInstructor(OTRA_ESCUELA, INSTRUCTOR, List.of(ANA, BETO))).isEmpty();
    }

    @Test
    void consumosPorClase_traenLasHorasNetas_laMasRecientePrimero_sinLasClasesDevueltasPorCompleto() {
        Pago pagoAna = pago(ESCUELA, ANA, EstadoPago.CONFIRMADO, 20);
        BilleteraHoras deAna = billetera(ESCUELA, ANA, 10);
        UUID antigua = UUID.randomUUID();
        UUID reciente = UUID.randomUUID();
        UUID cancelada = UUID.randomUUID();
        UUID deOtro = UUID.randomUUID();
        movimiento(deAna, pagoAna, TipoMovimiento.CONSUMO, 3, INSTRUCTOR, antigua);
        movimiento(deAna, pagoAna, TipoMovimiento.CONSUMO, 2, INSTRUCTOR, reciente);
        movimiento(deAna, pagoAna, TipoMovimiento.DEVOLUCION, 1, INSTRUCTOR, reciente);
        movimiento(deAna, pagoAna, TipoMovimiento.CONSUMO, 2, INSTRUCTOR, cancelada);
        movimiento(deAna, pagoAna, TipoMovimiento.DEVOLUCION, 2, INSTRUCTOR, cancelada);
        movimiento(deAna, pagoAna, TipoMovimiento.CONSUMO, 4, OTRO_INSTRUCTOR, deOtro);
        jdbc.update("UPDATE movimiento_horas SET created_at = ? WHERE clase_codigo = ?",
            Timestamp.from(Instant.parse("2026-10-01T10:00:00Z")), antigua);
        jdbc.update("UPDATE movimiento_horas SET created_at = ? WHERE clase_codigo = ?",
            Timestamp.from(Instant.parse("2026-10-20T10:00:00Z")), reciente);

        List<Object[]> clases = movimientos.consumosPorClase(ESCUELA, INSTRUCTOR, ANA);

        assertThat(clases).extracting(f -> (UUID) f[0]).containsExactly(reciente, antigua);
        assertThat(clases).extracting(f -> ((Number) f[2]).intValue()).containsExactly(1, 3);
        assertThat((Instant) clases.get(1)[1]).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
        assertThat(movimientos.consumosPorClase(ESCUELA, INSTRUCTOR, BETO)).isEmpty();
        assertThat(movimientos.consumosPorClase(OTRA_ESCUELA, INSTRUCTOR, ANA)).isEmpty();
    }

    @Test
    void horasDisponibles_sonLasDeLaBilletera_delEstudianteYLaEscuela() {
        billetera(ESCUELA, ANA, 14);
        billetera(OTRA_ESCUELA, ANA, 99);

        Map<UUID, Long> disponibles = comoMapa(billeteras.horasDisponiblesDe(ESCUELA, List.of(ANA, BETO)));

        assertThat(disponibles).containsOnlyKeys(ANA).containsEntry(ANA, 14L);
    }
}
