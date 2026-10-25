package com.drivehub.pagos.features.planes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.pagos.PostgresIntegrationTestBase;

/**
 * La migración de `plan_pago` (CU25a): las columnas que quedan y las reglas que protege la base.
 * Corre contra un Postgres real (Testcontainers) con Flyway: si no hay Docker, se salta.
 */
@SpringBootTest
class PlanPagoMigracionTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA_A = UUID.randomUUID();
    private static final UUID ESCUELA_B = UUID.randomUUID();

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM plan_pago");
    }

    private void insertar(UUID escuela, String nombre, int horas, String costo) {
        jdbc.update("INSERT INTO plan_pago (codigo_plan_pago, escuela_codigo, nombre, horas, costo_bs)"
            + " VALUES (?, ?, ?, ?, ?)", UUID.randomUUID(), escuela, nombre, horas, new BigDecimal(costo));
    }

    @Test
    void laTablaTieneLasColumnasDelPlanDeHoras_yYaNoLasDeCuotasNiClases() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'plan_pago'", String.class);

        assertThat(columnas).contains("codigo_plan_pago", "escuela_codigo", "nombre", "horas", "costo_bs", "activo",
            "created_at", "updated_at", "deleted_at");
        assertThat(columnas).doesNotContain("categoria_sigla", "nro_clases", "nro_cuotas", "monto_total");
    }

    @Test
    void unPlanValido_seInsertaYQuedaActivoPorDefecto() {
        insertar(ESCUELA_A, "Plan de 10 horas", 10, "450.00");

        Boolean activo = jdbc.queryForObject("SELECT activo FROM plan_pago WHERE escuela_codigo = ?", Boolean.class,
            ESCUELA_A);
        assertThat(activo).isTrue();
    }

    @Test
    void lasHorasFueraDeRango_laBaseLasRechaza() {
        assertThatThrownBy(() -> insertar(ESCUELA_A, "Cero", 0, "100.00"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(ESCUELA_A, "Muchas", 501, "100.00"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertar(ESCUELA_A, "Minimo", 1, "100.00")).doesNotThrowAnyException();
        assertThatCode(() -> insertar(ESCUELA_A, "Maximo", 500, "100.00")).doesNotThrowAnyException();
    }

    @Test
    void elCostoCeroONegativo_laBaseLoRechaza() {
        assertThatThrownBy(() -> insertar(ESCUELA_A, "Gratis", 10, "0.00"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(ESCUELA_A, "Negativo", 10, "-5.00"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertar(ESCUELA_A, "Centavo", 10, "0.01")).doesNotThrowAnyException();
    }

    @Test
    void elNombreEsUnicoPorEscuelaSinDistinguirMayusculas() {
        insertar(ESCUELA_A, "Plan 10", 10, "450.00");

        assertThatThrownBy(() -> insertar(ESCUELA_A, "plan 10", 20, "800.00"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(ESCUELA_A, "PLAN 10", 20, "800.00"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elMismoNombreEnOtraEscuela_seAcepta() {
        insertar(ESCUELA_A, "Plan 10", 10, "450.00");

        assertThatCode(() -> insertar(ESCUELA_B, "Plan 10", 10, "450.00")).doesNotThrowAnyException();
    }

    @Test
    void unPlanConDeletedAt_noCuentaParaLaUnicidadDelNombre() {
        insertar(ESCUELA_A, "Plan 10", 10, "450.00");
        jdbc.update("UPDATE plan_pago SET deleted_at = now() WHERE escuela_codigo = ?", ESCUELA_A);

        assertThatCode(() -> insertar(ESCUELA_A, "Plan 10", 10, "450.00")).doesNotThrowAnyException();
    }

    @Test
    void unPlanDesactivado_siguiendoVivo_cuentaParaLaUnicidadDelNombre() {
        insertar(ESCUELA_A, "Plan 10", 10, "450.00");
        jdbc.update("UPDATE plan_pago SET activo = false WHERE escuela_codigo = ?", ESCUELA_A);

        assertThatThrownBy(() -> insertar(ESCUELA_A, "plan 10", 10, "450.00"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sinEscuela_laBaseNoDejaInsertar() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO plan_pago (codigo_plan_pago, nombre, horas, costo_bs)"
            + " VALUES (?, 'Sin escuela', 10, 100)", UUID.randomUUID()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
