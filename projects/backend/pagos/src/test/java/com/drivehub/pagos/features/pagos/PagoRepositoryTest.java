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
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.PostgresIntegrationTestBase;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;
import com.drivehub.pagos.features.planes.repository.PlanPagoRepository;

/**
 * Las consultas de pagos (CU26) contra un Postgres real. Si no hay Docker, se salta.
 */
@SpringBootTest
class PagoRepositoryTest extends PostgresIntegrationTestBase {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");

    @Autowired
    private PagoRepository pagos;
    @Autowired
    private PlanPagoRepository planes;
    @Autowired
    private JdbcTemplate jdbc;

    private PlanPago plan;

    @BeforeEach
    void preparar() {
        vaciarTablas(jdbc);
        plan = planes.saveAndFlush(new PlanPago(ESCUELA, "Plan", (short) 10, new BigDecimal("450.00")));
    }

    private Pago guardar(String clave, EstadoPago estado, Instant expira) {
        Pago pago = new Pago(ESCUELA, UUID.randomUUID(), plan, new BigDecimal("38.23"), new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO),
            "Ana", "Escuela", clave, expira);
        pago.setEstado(estado);
        return pagos.saveAndFlush(pago);
    }

    // * La base se comparte con los demas tests: sin esto, los pagos que quedan impiden borrar los planes
    @AfterEach
    void limpiarAlTerminar() {
        vaciarTablas(jdbc);
    }

    @Test
    void idsVencidos_traeSoloLosPendientesCuyaPaginaVencioAntesDelLimite_losMasViejosPrimero() {
        Pago reciente = guardar("reciente", EstadoPago.PENDIENTE, AHORA.minusSeconds(100));
        Pago viejo = guardar("viejo", EstadoPago.PENDIENTE, AHORA.minusSeconds(5000));
        Pago masViejo = guardar("mas-viejo", EstadoPago.PENDIENTE, AHORA.minusSeconds(9000));
        guardar("vigente", EstadoPago.PENDIENTE, AHORA.plusSeconds(1800));
        guardar("confirmado", EstadoPago.CONFIRMADO, AHORA.minusSeconds(9000));
        guardar("fallido", EstadoPago.FALLIDO, AHORA.minusSeconds(9000));
        guardar("revertido", EstadoPago.REVERTIDO, AHORA.minusSeconds(9000));

        List<Long> ids = pagos.idsVencidos(EstadoPago.PENDIENTE, AHORA.minusSeconds(600), PageRequest.of(0, 100));

        assertThat(ids).containsExactly(masViejo.getId(), viejo.getId());
        assertThat(ids).doesNotContain(reciente.getId());
    }

    @Test
    void idsVencidos_respetaElTamanoDeLaPagina() {
        for (int i = 0; i < 5; i++) {
            guardar("p" + i, EstadoPago.PENDIENTE, AHORA.minusSeconds(9000L - i));
        }

        assertThat(pagos.idsVencidos(EstadoPago.PENDIENTE, AHORA, PageRequest.of(0, 3))).hasSize(3);
    }

    @Test
    void idsVencidos_unPagoSinVigenciaNoSeVence() {
        Pago sinVigencia = guardar("sin-vigencia", EstadoPago.PENDIENTE, null);

        assertThat(pagos.idsVencidos(EstadoPago.PENDIENTE, AHORA, PageRequest.of(0, 100)))
            .doesNotContain(sinVigencia.getId());
    }

    @Test
    void unPagoConDeletedAt_noApareceEnLasConsultas() {
        Pago borrado = guardar("borrado", EstadoPago.PENDIENTE, AHORA.minusSeconds(9000));
        jdbc.update("UPDATE pago SET deleted_at = now() WHERE id = ?", borrado.getId());

        assertThat(pagos.idsVencidos(EstadoPago.PENDIENTE, AHORA, PageRequest.of(0, 100))).isEmpty();
        assertThat(pagos.findByIdempotencyKey("borrado")).isEmpty();
    }

    @Test
    void findByCodigoAndEstudianteCodigoAndEscuelaCodigo_soloEncuentraAlDueno() {
        Pago pago = guardar("duenio", EstadoPago.PENDIENTE, AHORA);

        assertThat(pagos.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(pago.getCodigo(),
            pago.getEstudianteCodigo(), ESCUELA)).isPresent();
        assertThat(pagos.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(pago.getCodigo(),
            UUID.randomUUID(), ESCUELA)).isEmpty();
        assertThat(pagos.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(pago.getCodigo(),
            pago.getEstudianteCodigo(), UUID.randomUUID())).isEmpty();
    }
}
