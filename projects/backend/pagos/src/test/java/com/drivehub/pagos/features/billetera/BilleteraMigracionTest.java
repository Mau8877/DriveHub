package com.drivehub.pagos.features.billetera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.pagos.PostgresIntegrationTestBase;

/**
 * La migración de la billetera de horas (CU27): las tablas, y las reglas que protege la base. Corre contra un
 * Postgres real (Testcontainers) con Flyway: si no hay Docker, se salta.
 */
@SpringBootTest
class BilleteraMigracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA_A = UUID.randomUUID();
    private static final UUID ESCUELA_B = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    @Autowired
    private JdbcTemplate jdbc;

    private long pagoId;

    @BeforeEach
    void preparar() {
        limpiar();
        jdbc.update("INSERT INTO plan_pago (codigo_plan_pago, escuela_codigo, nombre, horas, costo_bs)"
            + " VALUES (?, ?, 'Plan 10', 10, 450)", UUID.randomUUID(), ESCUELA_A);
        long planId = jdbc.queryForObject("SELECT id FROM plan_pago", Long.class);
        jdbc.update("INSERT INTO pago (codigo_pago, escuela_codigo, estudiante_codigo, concepto, plan_pago_id,"
            + " plan_nombre, horas, horas_restantes, monto, monto_bs, tipo_cambio, metodo, idempotency_key)"
            + " VALUES (?, ?, ?, 'HORAS', ?, 'Plan 10', 10, 10, 38.23, 450, 11.7700, 'TARJETA', 'clave-migracion')",
            UUID.randomUUID(), ESCUELA_A, ESTUDIANTE, planId);
        pagoId = jdbc.queryForObject("SELECT id FROM pago", Long.class);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM movimiento_horas");
        jdbc.update("DELETE FROM billetera_horas");
        jdbc.update("DELETE FROM transaccion_pasarela");
        jdbc.update("DELETE FROM pago");
        jdbc.update("DELETE FROM plan_pago");
    }

    private long insertarBilletera(UUID escuela, UUID estudiante, int saldo) {
        jdbc.update("INSERT INTO billetera_horas (codigo_billetera_horas, escuela_codigo, estudiante_codigo,"
            + " horas_disponibles) VALUES (?, ?, ?, ?)", UUID.randomUUID(), escuela, estudiante, saldo);
        return jdbc.queryForObject("SELECT id FROM billetera_horas WHERE escuela_codigo = ? AND estudiante_codigo = ?",
            Long.class, escuela, estudiante);
    }

    private void insertarMovimiento(long billeteraId, String tipo, int horas, String clave) {
        jdbc.update("INSERT INTO movimiento_horas (codigo_movimiento_horas, billetera_horas_id, pago_id, tipo,"
            + " horas, idempotency_key) VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID(), billeteraId, pagoId, tipo,
            horas, clave);
    }

    @Test
    void lasDosTablasExisten_conSusColumnas() {
        List<String> billetera = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'billetera_horas'", String.class);
        List<String> movimiento = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'movimiento_horas'", String.class);

        assertThat(billetera).contains("codigo_billetera_horas", "escuela_codigo", "estudiante_codigo",
            "horas_disponibles", "version", "created_at", "updated_at", "deleted_at");
        assertThat(movimiento).contains("codigo_movimiento_horas", "billetera_horas_id", "pago_id", "tipo", "horas",
            "instructor_codigo", "clase_codigo", "idempotency_key", "created_at", "updated_at", "deleted_at");
    }

    @Test
    void unaBilleteraVacia_seInserta_yUnSaldoNegativoLoRechazaLaBase() {
        assertThatCode(() -> insertarBilletera(ESCUELA_A, ESTUDIANTE, 0)).doesNotThrowAnyException();

        assertThatThrownBy(() -> insertarBilletera(ESCUELA_A, UUID.randomUUID(), -1))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unaSegundaBilleteraDelMismoEstudianteEnLaMismaEscuela_laBaseLaRechaza_yEnOtraEscuelaSeAcepta() {
        insertarBilletera(ESCUELA_A, ESTUDIANTE, 5);

        assertThatThrownBy(() -> insertarBilletera(ESCUELA_A, ESTUDIANTE, 5))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertarBilletera(ESCUELA_B, ESTUDIANTE, 5)).doesNotThrowAnyException();
    }

    @Test
    void laVersionArrancaEnCero() {
        insertarBilletera(ESCUELA_A, ESTUDIANTE, 0);

        assertThat(jdbc.queryForObject("SELECT version FROM billetera_horas", Long.class)).isZero();
    }

    @Test
    void unMovimientoValido_seInserta_conCadaTipo() {
        long billetera = insertarBilletera(ESCUELA_A, ESTUDIANTE, 10);

        for (String tipo : List.of("ACREDITACION", "CONSUMO", "DEVOLUCION", "REEMBOLSO")) {
            assertThatCode(() -> insertarMovimiento(billetera, tipo, 1, "clave-" + tipo)).doesNotThrowAnyException();
        }
    }

    @Test
    void unMovimientoConHorasCeroONegativas_oConUnTipoDesconocido_laBaseLoRechaza() {
        long billetera = insertarBilletera(ESCUELA_A, ESTUDIANTE, 10);

        assertThatThrownBy(() -> insertarMovimiento(billetera, "CONSUMO", 0, "cero"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarMovimiento(billetera, "CONSUMO", -2, "negativas"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarMovimiento(billetera, "REGALO", 1, "tipo"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elMismoMovimientoDosVeces_conLaMismaClaveYElMismoPago_laBaseLoRechaza() {
        long billetera = insertarBilletera(ESCUELA_A, ESTUDIANTE, 10);
        insertarMovimiento(billetera, "CONSUMO", 1, "misma-clave");

        assertThatThrownBy(() -> insertarMovimiento(billetera, "CONSUMO", 1, "misma-clave"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertarMovimiento(billetera, "CONSUMO", 1, "otra-clave")).doesNotThrowAnyException();
    }

    @Test
    void unMovimientoSinBilleteraOSinPagoQueExista_laBaseLoRechaza() {
        long billetera = insertarBilletera(ESCUELA_A, ESTUDIANTE, 10);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO movimiento_horas (codigo_movimiento_horas,"
            + " billetera_horas_id, pago_id, tipo, horas, idempotency_key) VALUES (?, ?, 999999, 'CONSUMO', 1, 'x')",
            UUID.randomUUID(), billetera)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO movimiento_horas (codigo_movimiento_horas,"
            + " billetera_horas_id, pago_id, tipo, horas, idempotency_key) VALUES (?, 999999, ?, 'CONSUMO', 1, 'y')",
            UUID.randomUUID(), pagoId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sinEscuelaOSinEstudiante_laBaseNoDejaInsertarLaBilletera() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO billetera_horas (codigo_billetera_horas,"
            + " estudiante_codigo) VALUES (?, ?)", UUID.randomUUID(), ESTUDIANTE))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO billetera_horas (codigo_billetera_horas,"
            + " escuela_codigo) VALUES (?, ?)", UUID.randomUUID(), ESCUELA_A))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
