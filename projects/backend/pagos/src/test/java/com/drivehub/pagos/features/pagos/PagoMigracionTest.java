package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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
 * La migración de la compra de horas (CU26): las columnas que quedan en `pago` y las reglas que protege la
 * base. Corre contra un Postgres real (Testcontainers) con Flyway: si no hay Docker, se salta.
 */
@SpringBootTest
class PagoMigracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    @Autowired
    private JdbcTemplate jdbc;

    private long planId;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM transaccion_pasarela");
        jdbc.update("DELETE FROM pago");
        jdbc.update("DELETE FROM plan_pago");
        jdbc.update("INSERT INTO plan_pago (codigo_plan_pago, escuela_codigo, nombre, horas, costo_bs)"
            + " VALUES (?, ?, 'Plan 10', 10, 450)", UUID.randomUUID(), ESCUELA);
        planId = jdbc.queryForObject("SELECT id FROM plan_pago WHERE escuela_codigo = ?", Long.class, ESCUELA);
    }

    private void insertarHoras(String clave, Integer horas, Integer horasRestantes, Object plan, String metodo) {
        jdbc.update("INSERT INTO pago (codigo_pago, escuela_codigo, estudiante_codigo, concepto, plan_pago_id,"
            + " plan_nombre, horas, horas_restantes, monto, monto_bs, tipo_cambio, metodo, idempotency_key)"
            + " VALUES (?, ?, ?, 'HORAS', ?, 'Plan 10', ?, ?, 38.23, 450, 11.7700, ?, ?)",
            UUID.randomUUID(), ESCUELA, ESTUDIANTE, plan, horas, horasRestantes, metodo, clave);
    }

    // * La base se comparte con los demas tests: sin esto, los pagos que quedan impiden borrar los planes
    @AfterEach
    void limpiarAlTerminar() {
        jdbc.update("DELETE FROM transaccion_pasarela");
        jdbc.update("DELETE FROM pago");
        jdbc.update("DELETE FROM plan_pago");
    }

    @Test
    void laTablaTieneLasColumnasDeLaCompra_yYaNoLasDeCuotas() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'pago'", String.class);

        assertThat(columnas).contains("escuela_codigo", "plan_pago_id", "plan_nombre", "horas", "horas_restantes",
            "monto", "moneda", "monto_bs", "tipo_cambio", "estudiante_nombre", "escuela_nombre", "expira_en");
        assertThat(columnas).doesNotContain("cuota_id");
    }

    @Test
    void lasTablasDeCuotasEInscripcionesYaNoExisten() {
        Integer tablas = jdbc.queryForObject("SELECT count(*) FROM information_schema.tables"
            + " WHERE table_name IN ('cuota', 'inscripcion_plan')", Integer.class);
        assertThat(tablas).isZero();
    }

    @Test
    void laTransaccionGuardaLaUrlDePago_yYaNoElQr() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'transaccion_pasarela'",
            String.class);

        assertThat(columnas).contains("url_pago");
        assertThat(columnas).doesNotContain("qr_payload");
    }

    @Test
    void unPagoDeHorasCompleto_seInserta_yLaMonedaPorDefectoEsUsd() {
        assertThatCode(() -> insertarHoras("clave-1", 10, 10, planId, "TARJETA")).doesNotThrowAnyException();

        String moneda = jdbc.queryForObject("SELECT moneda FROM pago WHERE idempotency_key = 'clave-1'",
            String.class);
        assertThat(moneda).isEqualTo("USD");
    }

    @Test
    void unPagoDeHorasSinPlanOSinHoras_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertarHoras("sin-plan", 10, 10, null, "TARJETA"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarHoras("sin-horas", null, null, planId, "TARJETA"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lasHorasFueraDeRango_laBaseLasRechaza() {
        assertThatThrownBy(() -> insertarHoras("cero", 0, 0, planId, "TARJETA"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarHoras("muchas", 501, 501, planId, "TARJETA"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lasHorasRestantesMayoresQueLasCompradas_laBaseLasRechaza() {
        assertThatThrownBy(() -> insertarHoras("de-mas", 10, 11, planId, "TARJETA"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertarHoras("agotadas", 10, 0, planId, "TARJETA")).doesNotThrowAnyException();
    }

    @Test
    void laClaveDeIdempotenciaDuplicada_laBaseLaRechaza() {
        insertarHoras("misma", 10, 10, planId, "TARJETA");

        assertThatThrownBy(() -> insertarHoras("misma", 10, 10, planId, "TARJETA"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unMetodoQueNoEsTarjeta_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertarHoras("qr", 10, 10, planId, "QR"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unConceptoQueNoExiste_laBaseLoRechaza_yClaseSeAceptaSinPlan() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO pago (codigo_pago, escuela_codigo, estudiante_codigo,"
            + " concepto, monto, metodo, idempotency_key) VALUES (?, ?, ?, 'CUOTA', 10, 'TARJETA', 'cuota')",
            UUID.randomUUID(), ESCUELA, ESTUDIANTE)).isInstanceOf(DataIntegrityViolationException.class);

        assertThatCode(() -> jdbc.update("INSERT INTO pago (codigo_pago, escuela_codigo, estudiante_codigo,"
            + " concepto, monto, metodo, idempotency_key) VALUES (?, ?, ?, 'CLASE', ?, 'TARJETA', 'clase')",
            UUID.randomUUID(), ESCUELA, ESTUDIANTE, new BigDecimal("10.00"))).doesNotThrowAnyException();
    }

    @Test
    void sinEscuela_laBaseNoDejaInsertar() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO pago (codigo_pago, estudiante_codigo, concepto, monto,"
            + " metodo, idempotency_key) VALUES (?, ?, 'CLASE', 10, 'TARJETA', 'sin-escuela')",
            UUID.randomUUID(), ESTUDIANTE)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
