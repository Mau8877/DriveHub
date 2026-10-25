package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * La migración de la reversión (CU31): el origen, la retención y las reglas que protege la base. Corre contra un
 * Postgres real (Testcontainers) con Flyway: si no hay Docker, se salta.
 */
@SpringBootTest
class ReversionMigracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();

    @Autowired
    private JdbcTemplate jdbc;

    private long pagoId;

    @BeforeEach
    void preparar() {
        limpiar();
        jdbc.update("INSERT INTO plan_pago (codigo_plan_pago, escuela_codigo, nombre, horas, costo_bs)"
            + " VALUES (?, ?, 'Plan 10', 10, 450)", UUID.randomUUID(), ESCUELA);
        long planId = jdbc.queryForObject("SELECT id FROM plan_pago WHERE escuela_codigo = ?", Long.class, ESCUELA);
        jdbc.update("INSERT INTO pago (codigo_pago, escuela_codigo, estudiante_codigo, concepto, plan_pago_id,"
            + " plan_nombre, horas, horas_restantes, monto, monto_bs, tipo_cambio, metodo, idempotency_key, estado)"
            + " VALUES (?, ?, ?, 'HORAS', ?, 'Plan 10', 10, 10, 38.23, 450, 11.7700, 'TARJETA', 'clave-rev',"
            + " 'CONFIRMADO')", UUID.randomUUID(), ESCUELA, UUID.randomUUID(), planId);
        pagoId = jdbc.queryForObject("SELECT id FROM pago WHERE escuela_codigo = ?", Long.class, ESCUELA);
    }

    @AfterEach
    void limpiar() {
        vaciarTablas(jdbc);
    }

    private void insertar(String origen, String motivo, String detalle, double monto, double retenido,
            String estado) {
        jdbc.update("INSERT INTO reversion_pago (codigo_reversion_pago, pago_id, origen, motivo, motivo_detalle,"
            + " monto, monto_retenido, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), pagoId, origen,
            motivo, detalle, monto, retenido, estado);
    }

    @Test
    void unaReversionValida_seAcepta() {
        assertThatCode(() -> insertar("ESTUDIANTE", "MUDANZA", null, 36.32, 1.91, "SOLICITADA"))
            .doesNotThrowAnyException();
    }

    @Test
    void unaSegundaReversionSolicitadaOEjecutada_esRechazada() {
        insertar("ESTUDIANTE", "MUDANZA", null, 36.32, 1.91, "SOLICITADA");

        assertThatThrownBy(() -> insertar("SAGA", "MONTO_DISTINTO", null, 38.23, 0, "SOLICITADA"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("SAGA", "MONTO_DISTINTO", null, 38.23, 0, "EJECUTADA"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void despuesDeUnaFallida_seAceptaOtra() {
        insertar("SAGA", "MONTO_DISTINTO", null, 38.23, 0, "FALLIDA");

        assertThatCode(() -> insertar("SAGA", "MONTO_DISTINTO", null, 38.23, 0, "SOLICITADA"))
            .doesNotThrowAnyException();
    }

    @Test
    void origenDesconocido_montoNoPositivoORetencionNegativa_sonRechazados() {
        assertThatThrownBy(() -> insertar("ADMIN", "MUDANZA", null, 36.32, 1.91, "SOLICITADA"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("ESTUDIANTE", "MUDANZA", null, 0, 1.91, "SOLICITADA"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("ESTUDIANTE", "MUDANZA", null, 36.32, -1, "SOLICITADA"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void otroSinDetalle_yUnMotivoDistintoDeOtroConDetalle_sonRechazados() {
        assertThatThrownBy(() -> insertar("ESTUDIANTE", "OTRO", null, 36.32, 1.91, "SOLICITADA"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("ESTUDIANTE", "MUDANZA", "texto", 36.32, 1.91, "SOLICITADA"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertar("ESTUDIANTE", "OTRO", "Perdí mi trabajo", 36.32, 1.91, "SOLICITADA"))
            .doesNotThrowAnyException();
    }
}
