package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.exception.BusinessException;
import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.features.pagos.dto.ReportePagoResponse;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.pagos.service.ReportePagosService;
import com.drivehub.pagos.features.planes.model.PlanPago;

@ExtendWith(MockitoExtension.class)
class ReportePagosServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final Instant CREADO = Instant.parse("2026-10-25T12:00:00Z");

    @Mock
    private PagoRepository pagos;
    @Mock
    private ReversionPagoRepository reversiones;

    private ReportePagosService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReportePagosService(pagos, reversiones);
    }

    private static Pago pago(long id, EstadoPago estado, String estudiante) {
        PlanPago plan = new PlanPago(ESCUELA, "Plan 10", (short) 10, new BigDecimal("450.00"));
        Pago pago = new Pago(ESCUELA, UUID.randomUUID(), plan, new BigDecimal("38.23"),
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO), estudiante, "Escuela", "clave-" + id, CREADO);
        pago.setEstado(estado);
        ReflectionTestUtils.setField(pago, "id", id);
        ReflectionTestUtils.setField(pago, "codigo", UUID.randomUUID());
        ReflectionTestUtils.setField(pago, "createdAt", CREADO);
        return pago;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Pageable> devolver(List<Pago> resultado) {
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        when(pagos.findAll(any(Specification.class), pagina.capture()))
            .thenAnswer(i -> new PageImpl<>(resultado, i.getArgument(1), resultado.size()));
        return pagina;
    }

    @Test
    void listar_devuelveLosPagosDeTodaLaEscuelaConSuEstadoVisible() {
        devolver(List.of(pago(1, EstadoPago.CONFIRMADO, "Ana"), pago(2, EstadoPago.CONFIRMADO, "Luis"),
            pago(3, EstadoPago.REVERTIDO, "Eva")));
        when(reversiones.pagoIdsConReversion(anyList(), eq(EstadoReversion.SOLICITADA))).thenReturn(List.of(2L));

        Page<ReportePagoResponse> pagina = servicio.listar(ESCUELA, null, null, null, null, null);

        assertThat(pagina.getContent()).extracting(ReportePagoResponse::estudianteNombre)
            .containsExactly("Ana", "Luis", "Eva");
        assertThat(pagina.getContent()).extracting(ReportePagoResponse::estado).containsExactly(
            EstadoPagoVisible.CONFIRMADO, EstadoPagoVisible.REEMBOLSO_EN_PROCESO, EstadoPagoVisible.REEMBOLSADO);
    }

    @Test
    void listar_usaPaginasGrandesPeroAcotadas() {
        ArgumentCaptor<Pageable> pagina = devolver(List.of());

        servicio.listar(ESCUELA, null, null, null, 3, 10_000);

        assertThat(pagina.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pagina.getValue().getPageSize()).isEqualTo(500);
        verifyNoInteractions(reversiones);
    }

    @Test
    void listar_conRangoInvertido_lanzaValidationException() {
        assertThatThrownBy(() -> servicio.listar(ESCUELA, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1), null,
                null, null))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(PagosErrorCodes.RANGO_DE_FECHAS_INVALIDO);
        verifyNoInteractions(pagos);
    }
}
