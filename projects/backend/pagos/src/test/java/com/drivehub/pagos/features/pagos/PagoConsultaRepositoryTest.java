package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.drivehub.pagos.features.pagos.enums.ConceptoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.PagoSpecification;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

/**
 * Las consultas del historial de pagos del estudiante (CU28) contra un Postgres real. Si no hay Docker, se salta.
 */
@SpringBootTest
class PagoConsultaRepositoryTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final Sort ORDEN = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    @Autowired
    private PagoRepository pagos;
    @Autowired
    private ReversionPagoRepository reversiones;
    @Autowired
    private PlanPagoRepository planes;
    @Autowired
    private JdbcTemplate jdbc;

    private PlanPago plan;
    private int numero;

    @BeforeEach
    void preparar() {
        limpiar();
        plan = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan", (short) 10, new BigDecimal("450.00")));
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM reversion_pago");
        jdbc.update("DELETE FROM transaccion_pasarela");
        jdbc.update("DELETE FROM pago");
        jdbc.update("DELETE FROM plan_pago");
    }

    private Pago guardar(UUID escuela, UUID estudiante, EstadoPago estado) {
        Pago pago = new Pago(escuela, estudiante, plan, new BigDecimal("38.23"),
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO), "Ana", "Escuela", "clave-" + numero++,
            Instant.parse("2026-10-25T12:00:00Z"));
        pago.setEstado(estado);
        return pagos.saveAndFlush(pago);
    }

    private Pago guardar(EstadoPago estado) {
        return guardar(ESCUELA, ESTUDIANTE, estado);
    }

    private void reversion(Pago pago, EstadoReversion estado) {
        jdbc.update("INSERT INTO reversion_pago (codigo_reversion_pago, pago_id, estado) VALUES (?, ?, ?)",
            UUID.randomUUID(), pago.getId(), estado.name());
    }

    private List<Pago> consultar(EstadoPagoVisible visible) {
        return pagos.findAll(Specification.allOf(PagoSpecification.deEstudiante(ESCUELA, ESTUDIANTE),
            PagoSpecification.deConcepto(ConceptoPago.HORAS), PagoSpecification.conEstadoVisible(visible)), ORDEN)
            .stream().toList();
    }

    @Test
    void deEstudiante_traeSoloLosPagosDeEsaPersonaEnEsaEscuela() {
        Pago propio = guardar(EstadoPago.CONFIRMADO);
        guardar(ESCUELA, UUID.randomUUID(), EstadoPago.CONFIRMADO);
        guardar(UUID.randomUUID(), ESTUDIANTE, EstadoPago.CONFIRMADO);

        assertThat(consultar(null)).extracting(Pago::getId).containsExactly(propio.getId());
    }

    @Test
    void ordenaDelMasRecienteAlMasAntiguo_conElIdComoDesempate() {
        Pago primero = guardar(EstadoPago.PENDIENTE);
        Pago segundo = guardar(EstadoPago.PENDIENTE);
        Pago tercero = guardar(EstadoPago.PENDIENTE);
        jdbc.update("UPDATE pago SET created_at = ? WHERE id = ?", java.sql.Timestamp.from(Instant.parse("2026-10-20T00:00:00Z")),
            tercero.getId());

        assertThat(consultar(null)).extracting(Pago::getId).containsExactly(segundo.getId(), primero.getId(),
            tercero.getId());
    }

    @Test
    void cadaFiltroDeEstadoDevuelveLoQueCorresponde() {
        Pago pendiente = guardar(EstadoPago.PENDIENTE);
        Pago fallido = guardar(EstadoPago.FALLIDO);
        Pago confirmado = guardar(EstadoPago.CONFIRMADO);
        Pago enProceso = guardar(EstadoPago.CONFIRMADO);
        reversion(enProceso, EstadoReversion.SOLICITADA);
        Pago reembolsado = guardar(EstadoPago.REVERTIDO);
        reversion(reembolsado, EstadoReversion.EJECUTADA);

        assertThat(consultar(EstadoPagoVisible.PENDIENTE)).extracting(Pago::getId).containsExactly(pendiente.getId());
        assertThat(consultar(EstadoPagoVisible.FALLIDO)).extracting(Pago::getId).containsExactly(fallido.getId());
        assertThat(consultar(EstadoPagoVisible.CONFIRMADO)).extracting(Pago::getId).containsExactly(confirmado.getId());
        assertThat(consultar(EstadoPagoVisible.REEMBOLSO_EN_PROCESO)).extracting(Pago::getId)
            .containsExactly(enProceso.getId());
        assertThat(consultar(EstadoPagoVisible.REEMBOLSADO)).extracting(Pago::getId)
            .containsExactly(reembolsado.getId());
        assertThat(consultar(null)).hasSize(5);
    }

    @Test
    void unaReversionFallidaOEjecutadaNoPonePagoConfirmadoEnProceso() {
        Pago conFallida = guardar(EstadoPago.CONFIRMADO);
        reversion(conFallida, EstadoReversion.FALLIDA);

        assertThat(consultar(EstadoPagoVisible.REEMBOLSO_EN_PROCESO)).isEmpty();
        assertThat(consultar(EstadoPagoVisible.CONFIRMADO)).extracting(Pago::getId)
            .containsExactly(conFallida.getId());
    }

    @Test
    void paginaYCuentaElTotal() {
        for (int i = 0; i < 5; i++) {
            guardar(EstadoPago.CONFIRMADO);
        }

        Page<Pago> pagina = pagos.findAll(Specification.allOf(PagoSpecification.deEstudiante(ESCUELA, ESTUDIANTE),
            PagoSpecification.conEstadoVisible(EstadoPagoVisible.CONFIRMADO)), PageRequest.of(1, 2, ORDEN));

        assertThat(pagina.getTotalElements()).isEqualTo(5);
        assertThat(pagina.getTotalPages()).isEqualTo(3);
        assertThat(pagina.getContent()).hasSize(2);
    }

    @Test
    void unPagoConDeletedAt_noAparece() {
        Pago borrado = guardar(EstadoPago.CONFIRMADO);
        jdbc.update("UPDATE pago SET deleted_at = now() WHERE id = ?", borrado.getId());

        assertThat(consultar(null)).isEmpty();
    }

    @Test
    void reversiones_sabenSiUnPagoTieneUnaEnUnEstado() {
        Pago enProceso = guardar(EstadoPago.CONFIRMADO);
        Pago limpio = guardar(EstadoPago.CONFIRMADO);
        reversion(enProceso, EstadoReversion.SOLICITADA);

        assertThat(reversiones.existsByPagoIdAndEstado(enProceso.getId(), EstadoReversion.SOLICITADA)).isTrue();
        assertThat(reversiones.existsByPagoIdAndEstado(enProceso.getId(), EstadoReversion.EJECUTADA)).isFalse();
        assertThat(reversiones.existsByPagoIdAndEstado(limpio.getId(), EstadoReversion.SOLICITADA)).isFalse();
        assertThat(reversiones.pagoIdsConReversion(List.of(enProceso.getId(), limpio.getId()),
            EstadoReversion.SOLICITADA)).containsExactly(enProceso.getId());
    }
}
