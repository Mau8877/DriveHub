package com.drivehub.pagos.features.comprobantes;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.drivehub.pagos.features.comprobantes.repository.ComprobanteRepository;

/**
 * La migración de los comprobantes por tipo (CU30): lo que protege la base. Corre contra un Postgres real
 * (Testcontainers) con Flyway: si no hay Docker, se salta.
 */
@SpringBootTest
class ComprobanteMigracionTest extends PostgresIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ComprobanteRepository comprobantes;

    private long pagoId;
    private long reversionId;
    private int numero;

    @BeforeEach
    void preparar() {
        limpiar();
        UUID escuela = UUID.randomUUID();
        jdbc.update("INSERT INTO plan_pago (codigo_plan_pago, escuela_codigo, nombre, horas, costo_bs)"
            + " VALUES (?, ?, 'Plan 10', 10, 450)", UUID.randomUUID(), escuela);
        long planId = jdbc.queryForObject("SELECT id FROM plan_pago", Long.class);
        jdbc.update("INSERT INTO pago (codigo_pago, escuela_codigo, estudiante_codigo, concepto, plan_pago_id,"
            + " plan_nombre, horas, horas_restantes, monto, monto_bs, tipo_cambio, metodo, estado, idempotency_key)"
            + " VALUES (?, ?, ?, 'HORAS', ?, 'Plan 10', 10, 10, 38.23, 450, 11.7700, 'TARJETA', 'CONFIRMADO',"
            + " 'clave-comprobante')", UUID.randomUUID(), escuela, UUID.randomUUID(), planId);
        pagoId = jdbc.queryForObject("SELECT id FROM pago", Long.class);
        jdbc.update("INSERT INTO reversion_pago (codigo_reversion_pago, pago_id, origen, motivo, monto, monto_retenido)"
            + " VALUES (?, ?, 'ESTUDIANTE', 'MUDANZA', 36.32, 1.91)",
            UUID.randomUUID(), pagoId);
        reversionId = jdbc.queryForObject("SELECT id FROM reversion_pago", Long.class);
    }

    @AfterEach
    void limpiar() {
        vaciarTablas(jdbc);
    }

    private void insertar(String tipo, Long reversion) {
        jdbc.update("INSERT INTO comprobante (codigo_comprobante, pago_id, tipo, reversion_pago_id, nro_comprobante,"
            + " monto, emitido_en) VALUES (?, ?, ?, ?, ?, 38.23, now())", UUID.randomUUID(), pagoId, tipo, reversion,
            "CMP-TEST-" + numero++);
    }

    @Test
    void dosComprobantesDePagoDelMismoPago_sonRechazados() {
        insertar("PAGO", null);

        assertThatThrownBy(() -> insertar("PAGO", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unoDePagoYUnoDeReembolsoDelMismoPago_seAceptan() {
        assertThatCode(() -> {
            insertar("PAGO", null);
            insertar("REEMBOLSO", reversionId);
        }).doesNotThrowAnyException();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM comprobante", Integer.class)).isEqualTo(2);
    }

    @Test
    void unReembolsoSinReversion_esRechazado() {
        assertThatThrownBy(() -> insertar("REEMBOLSO", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unTipoDesconocido_esRechazado() {
        assertThatThrownBy(() -> insertar("OTRO", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elPdfPuedeEstarPendiente_yElNumeroDebeSerUnico() {
        insertar("PAGO", null);
        assertThat(jdbc.queryForObject("SELECT url_pdf IS NULL FROM comprobante", Boolean.class)).isTrue();

        assertThatThrownBy(() -> jdbc.update("INSERT INTO comprobante (codigo_comprobante, pago_id, tipo,"
            + " reversion_pago_id, nro_comprobante, monto, emitido_en) VALUES (?, ?, 'REEMBOLSO', ?, ?, 1, now())",
            UUID.randomUUID(), pagoId, reversionId, "CMP-TEST-0")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laSecuenciaDeNumeros_entregaValoresCrecientes() {
        long primero = comprobantes.siguienteNumero();
        long segundo = comprobantes.siguienteNumero();

        assertThat(segundo).isGreaterThan(primero);
    }
}
