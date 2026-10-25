package com.drivehub.pagos.features.planes;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;
import com.drivehub.pagos.features.planes.repository.PlanPagoSpecification;

/**
 * Las consultas de planes (CU25), siempre aisladas por escuela. Corre contra un Postgres real
 * (Testcontainers): si no hay Docker, se salta.
 */
@SpringBootTest
class PlanPagoRepositoryTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA_A = UUID.randomUUID();
    private static final UUID ESCUELA_B = UUID.randomUUID();
    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    @Autowired
    private PlanPagoRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM plan_pago");
    }

    /** Guarda un plan y fija su fecha de alta, para que el orden de los tests no dependa del reloj. */
    private PlanPago guardar(UUID escuela, String nombre, boolean activo, String creado) {
        PlanPago plan = new PlanPago(escuela, nombre, (short) 10, new BigDecimal("450.00"));
        plan.setActivo(activo);
        plan = repositorio.saveAndFlush(plan);
        jdbc.update("UPDATE plan_pago SET created_at = ? WHERE id = ?",
            java.sql.Timestamp.from(Instant.parse(creado)), plan.getId());
        return plan;
    }

    private Page<PlanPago> listar(UUID escuela, String busqueda, Boolean activo) {
        Specification<PlanPago> filtro = Specification.allOf(PlanPagoSpecification.deLaEscuela(escuela),
            PlanPagoSpecification.nombreContiene(busqueda), PlanPagoSpecification.conEstado(activo));
        return repositorio.findAll(filtro, PageRequest.of(0, 20, MAS_RECIENTE_PRIMERO));
    }

    private static List<String> nombres(Page<PlanPago> pagina) {
        return pagina.getContent().stream().map(PlanPago::getNombre).toList();
    }

    @Test
    void listar_traeSoloLosPlanesDeLaEscuelaPedida() {
        guardar(ESCUELA_A, "Plan A1", true, "2026-10-01T10:00:00Z");
        guardar(ESCUELA_B, "Plan B1", true, "2026-10-01T10:00:00Z");

        assertThat(nombres(listar(ESCUELA_A, null, null))).containsExactly("Plan A1");
        assertThat(nombres(listar(ESCUELA_B, null, null))).containsExactly("Plan B1");
    }

    @Test
    void listar_buscaPorParteDelNombreSinImportarMayusculas() {
        guardar(ESCUELA_A, "Plan Intensivo", true, "2026-10-01T10:00:00Z");
        guardar(ESCUELA_A, "Plan Basico", true, "2026-10-02T10:00:00Z");

        assertThat(nombres(listar(ESCUELA_A, "INTENS", null))).containsExactly("Plan Intensivo");
        assertThat(nombres(listar(ESCUELA_A, "plan", null))).containsExactlyInAnyOrder("Plan Intensivo", "Plan Basico");
        assertThat(nombres(listar(ESCUELA_A, "   ", null))).hasSize(2);
        assertThat(nombres(listar(ESCUELA_A, "inexistente", null))).isEmpty();
    }

    @Test
    void listar_tomaElPorcientoYElGuionBajoComoLetras() {
        guardar(ESCUELA_A, "Plan 100% teorico", true, "2026-10-01T10:00:00Z");
        guardar(ESCUELA_A, "Plan 1000 practico", true, "2026-10-02T10:00:00Z");
        guardar(ESCUELA_A, "Plan_extra", true, "2026-10-03T10:00:00Z");
        guardar(ESCUELA_A, "Plan extra", true, "2026-10-04T10:00:00Z");

        assertThat(nombres(listar(ESCUELA_A, "100%", null))).containsExactly("Plan 100% teorico");
        assertThat(nombres(listar(ESCUELA_A, "_extra", null))).containsExactly("Plan_extra");
        assertThat(nombres(listar(ESCUELA_A, "%", null))).containsExactly("Plan 100% teorico");
    }

    @Test
    void listar_filtraPorEstado_ySinFiltroVuelvenLosDos() {
        guardar(ESCUELA_A, "Activo", true, "2026-10-01T10:00:00Z");
        guardar(ESCUELA_A, "Desactivado", false, "2026-10-02T10:00:00Z");

        assertThat(nombres(listar(ESCUELA_A, null, true))).containsExactly("Activo");
        assertThat(nombres(listar(ESCUELA_A, null, false))).containsExactly("Desactivado");
        assertThat(nombres(listar(ESCUELA_A, null, null))).containsExactlyInAnyOrder("Activo", "Desactivado");
    }

    @Test
    void listar_vaDelMasRecienteAlMasAntiguo() {
        guardar(ESCUELA_A, "Primero", true, "2026-10-01T10:00:00Z");
        guardar(ESCUELA_A, "Tercero", true, "2026-10-03T10:00:00Z");
        guardar(ESCUELA_A, "Segundo", true, "2026-10-02T10:00:00Z");

        assertThat(nombres(listar(ESCUELA_A, null, null))).containsExactly("Tercero", "Segundo", "Primero");
    }

    @Test
    void listar_paginaYCuentaElTotal() {
        for (int i = 1; i <= 5; i++) {
            guardar(ESCUELA_A, "Plan " + i, true, "2026-10-0" + i + "T10:00:00Z");
        }
        Specification<PlanPago> filtro = PlanPagoSpecification.deLaEscuela(ESCUELA_A);

        Page<PlanPago> segunda = repositorio.findAll(filtro, PageRequest.of(1, 2, MAS_RECIENTE_PRIMERO));

        assertThat(segunda.getTotalElements()).isEqualTo(5);
        assertThat(segunda.getTotalPages()).isEqualTo(3);
        assertThat(nombres(segunda)).containsExactly("Plan 3", "Plan 2");
    }

    @Test
    void findByCodigoAndEscuelaCodigo_devuelveElPlanDeSuEscuela_yVacioParaLaDeOtra() {
        PlanPago plan = guardar(ESCUELA_A, "Plan A1", true, "2026-10-01T10:00:00Z");

        assertThat(repositorio.findByCodigoAndEscuelaCodigo(plan.getCodigo(), ESCUELA_A)).isPresent();
        assertThat(repositorio.findByCodigoAndEscuelaCodigo(plan.getCodigo(), ESCUELA_B)).isEmpty();
        assertThat(repositorio.findByCodigoAndEscuelaCodigo(UUID.randomUUID(), ESCUELA_A)).isEmpty();
    }

    @Test
    void existePorNombre_ignoraMayusculas_cuentaLosDesactivados_yEsPorEscuela() {
        guardar(ESCUELA_A, "Plan 10", false, "2026-10-01T10:00:00Z");

        assertThat(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(ESCUELA_A, "plan 10")).isTrue();
        assertThat(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(ESCUELA_A, "PLAN 10")).isTrue();
        assertThat(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(ESCUELA_A, "Plan 20")).isFalse();
        assertThat(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(ESCUELA_B, "Plan 10")).isFalse();
    }

    @Test
    void existePorNombreDeOtro_excluyeAlPropioPlan() {
        PlanPago propio = guardar(ESCUELA_A, "Plan 10", true, "2026-10-01T10:00:00Z");
        PlanPago otro = guardar(ESCUELA_A, "Plan 20", true, "2026-10-02T10:00:00Z");

        assertThat(repositorio.existsByEscuelaCodigoAndNombreIgnoreCaseAndCodigoNot(
            ESCUELA_A, "plan 10", propio.getCodigo())).isFalse();
        assertThat(repositorio.existsByEscuelaCodigoAndNombreIgnoreCaseAndCodigoNot(
            ESCUELA_A, "plan 10", otro.getCodigo())).isTrue();
    }

    @Test
    void unPlanConDeletedAt_noApareceEnNingunaConsulta() {
        PlanPago plan = guardar(ESCUELA_A, "Plan borrado", true, "2026-10-01T10:00:00Z");
        jdbc.update("UPDATE plan_pago SET deleted_at = now() WHERE id = ?", plan.getId());

        assertThat(nombres(listar(ESCUELA_A, null, null))).isEmpty();
        assertThat(repositorio.findByCodigoAndEscuelaCodigo(plan.getCodigo(), ESCUELA_A)).isEmpty();
        assertThat(repositorio.existsByEscuelaCodigoAndNombreIgnoreCase(ESCUELA_A, "Plan borrado")).isFalse();
    }
}
