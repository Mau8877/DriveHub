package com.drivehub.pagos.features.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.features.pagos.dto.PagoDetalleResponse;
import com.drivehub.pagos.features.pagos.dto.PagoResumenResponse;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.enums.EstadoPagoVisible;
import com.drivehub.pagos.features.pagos.enums.EstadoReversion;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.pagos.service.ConsultaPagosService;
import com.drivehub.pagos.features.pagos.service.ReembolsoCalculadora;
import com.drivehub.pagos.features.planes.model.PlanPago;

@ExtendWith(MockitoExtension.class)
class ConsultaPagosServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final Instant CREADO = Instant.parse("2026-10-25T12:00:00Z");

    @Mock
    private PagoRepository pagos;
    @Mock
    private ReversionPagoRepository reversiones;

    private ConsultaPagosService servicio;
    private long siguienteId;

    @BeforeEach
    void preparar() {
        servicio = new ConsultaPagosService(pagos, reversiones,
            new ReembolsoCalculadora(PagosPropertiesPrueba.conRetencion(5)));
        siguienteId = 1;
    }

    private Pago pago(EstadoPago estado, int horas, int restantes) {
        PlanPago plan = new PlanPago(ESCUELA, "Plan " + horas, (short) horas, new BigDecimal("450.00"));
        Pago pago = new Pago(ESCUELA, ESTUDIANTE, plan, new BigDecimal("100.00"),
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO), "Ana", "Escuela", "clave-" + siguienteId, CREADO);
        pago.setEstado(estado);
        pago.setHorasRestantes((short) restantes);
        if (estado == EstadoPago.CONFIRMADO) {
            pago.setConfirmadoEn(CREADO.plusSeconds(60));
        }
        ReflectionTestUtils.setField(pago, "id", siguienteId++);
        ReflectionTestUtils.setField(pago, "codigo", UUID.randomUUID());
        ReflectionTestUtils.setField(pago, "createdAt", CREADO);
        return pago;
    }

    @SuppressWarnings("unchecked")
    private Pageable pedirListado(List<Pago> resultado) {
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        when(pagos.findAll(any(Specification.class), pagina.capture()))
            .thenAnswer(i -> new PageImpl<>(resultado, i.getArgument(1), resultado.size()));
        servicio.listar(ESCUELA, ESTUDIANTE, null, null, null);
        return pagina.getValue();
    }

    // ------------------------------------------------------------------ listado

    @Test
    void listar_sinParametros_pideLaPagina1DeVeinte_delMasRecientePrimero() {
        Pageable pagina = pedirListado(List.of());

        assertThat(pagina.getPageNumber()).isZero();
        assertThat(pagina.getPageSize()).isEqualTo(20);
        assertThat(pagina.getSort().toString()).isEqualTo("createdAt: DESC,id: DESC");
    }

    @SuppressWarnings("unchecked")
    @Test
    void listar_conPageSizeMayorAlTope_usaCien_yConPaginaCero_usaLaPrimera() {
        when(pagos.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);

        servicio.listar(ESCUELA, ESTUDIANTE, 0, 500, null);
        servicio.listar(ESCUELA, ESTUDIANTE, 3, 0, null);

        verify(pagos, times(2)).findAll(any(Specification.class), pagina.capture());
        assertThat(pagina.getAllValues().get(0).getPageNumber()).isZero();
        assertThat(pagina.getAllValues().get(0).getPageSize()).isEqualTo(100);
        assertThat(pagina.getAllValues().get(1).getPageNumber()).isEqualTo(2);
        assertThat(pagina.getAllValues().get(1).getPageSize()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listar_calculaElEstadoVisible_deCadaPago() {
        Pago pendiente = pago(EstadoPago.PENDIENTE, 10, 0);
        Pago confirmado = pago(EstadoPago.CONFIRMADO, 10, 10);
        Pago enProceso = pago(EstadoPago.CONFIRMADO, 10, 10);
        Pago fallido = pago(EstadoPago.FALLIDO, 10, 0);
        Pago revertido = pago(EstadoPago.REVERTIDO, 10, 0);
        when(pagos.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(pendiente, confirmado, enProceso, fallido, revertido)));
        when(reversiones.pagoIdsConReversion(any(), eq(EstadoReversion.SOLICITADA)))
            .thenReturn(List.of(enProceso.getId()));

        List<PagoResumenResponse> filas = servicio.listar(ESCUELA, ESTUDIANTE, null, null, null).getContent();

        assertThat(filas).extracting(PagoResumenResponse::estado).containsExactly(EstadoPagoVisible.PENDIENTE,
            EstadoPagoVisible.CONFIRMADO, EstadoPagoVisible.REEMBOLSO_EN_PROCESO, EstadoPagoVisible.FALLIDO,
            EstadoPagoVisible.REEMBOLSADO);
        PagoResumenResponse primera = filas.get(0);
        assertThat(primera.codigo()).isEqualTo(pendiente.getCodigo());
        assertThat(primera.planNombre()).isEqualTo("Plan 10");
        assertThat(primera.horas()).isEqualTo((short) 10);
        assertThat(primera.montoUsd()).isEqualByComparingTo("100.00");
        assertThat(primera.montoBs()).isEqualByComparingTo("450.00");
        assertThat(primera.creadoEn()).isEqualTo(CREADO);
        // * Solo se consultan las reversiones de los confirmados, y en una sola consulta
        verify(reversiones).pagoIdsConReversion(List.of(confirmado.getId(), enProceso.getId()),
            EstadoReversion.SOLICITADA);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listar_sinPagosConfirmados_noConsultaLasReversiones() {
        when(pagos.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pago(EstadoPago.PENDIENTE, 10, 0))));

        servicio.listar(ESCUELA, ESTUDIANTE, null, null, EstadoPagoVisible.PENDIENTE);

        verifyNoInteractions(reversiones);
    }

    // ------------------------------------------------------------------ detalle

    private void existe(Pago pago) {
        when(pagos.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(pago.getCodigo(), ESTUDIANTE, ESCUELA))
            .thenReturn(Optional.of(pago));
    }

    @Test
    void detalle_deUnPagoAjenoOInexistente_lanzaErrPag07() {
        UUID codigo = UUID.randomUUID();
        when(pagos.findByCodigoAndEstudianteCodigoAndEscuelaCodigo(codigo, ESTUDIANTE, ESCUELA))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.detalle(ESCUELA, ESTUDIANTE, codigo))
            .isInstanceOfSatisfying(NotFoundException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_07"));
    }

    @Test
    void detalle_deUnPagoConfirmadoEIntacto_esReembolsable_con95Y5() {
        Pago intacto = pago(EstadoPago.CONFIRMADO, 10, 10);
        existe(intacto);

        PagoDetalleResponse detalle = servicio.detalle(ESCUELA, ESTUDIANTE, intacto.getCodigo());

        assertThat(detalle.reembolsable()).isTrue();
        assertThat(detalle.estado()).isEqualTo(EstadoPagoVisible.CONFIRMADO);
        assertThat(detalle.horasRestantes()).isEqualTo((short) 10);
        assertThat(detalle.confirmadoEn()).isEqualTo(CREADO.plusSeconds(60));
        assertThat(detalle.reembolso().montoDevuelto()).isEqualByComparingTo("95.00");
        assertThat(detalle.reembolso().montoRetenido()).isEqualByComparingTo("5.00");
        assertThat(detalle.reembolso().porcentajeRetenido()).isEqualTo(5);
    }

    @Test
    void detalle_conHorasUsadas_noEsReembolsable() {
        Pago usado = pago(EstadoPago.CONFIRMADO, 10, 7);
        existe(usado);

        PagoDetalleResponse detalle = servicio.detalle(ESCUELA, ESTUDIANTE, usado.getCodigo());

        assertThat(detalle.reembolsable()).isFalse();
        assertThat(detalle.reembolso()).isNull();
        assertThat(detalle.horasRestantes()).isEqualTo((short) 7);
    }

    @Test
    void detalle_deUnPagoPendienteOFallido_noEsReembolsable() {
        Pago pendiente = pago(EstadoPago.PENDIENTE, 10, 10);
        Pago fallido = pago(EstadoPago.FALLIDO, 10, 10);
        existe(pendiente);
        existe(fallido);

        assertThat(servicio.detalle(ESCUELA, ESTUDIANTE, pendiente.getCodigo()).reembolsable()).isFalse();
        assertThat(servicio.detalle(ESCUELA, ESTUDIANTE, fallido.getCodigo()).reembolsable()).isFalse();
        verifyNoInteractions(reversiones);
    }

    @Test
    void detalle_conUnaReversionSolicitada_noEsReembolsable_yEstaEnProceso() {
        Pago pago = pago(EstadoPago.CONFIRMADO, 10, 10);
        existe(pago);
        when(reversiones.existsByPagoIdAndEstado(pago.getId(), EstadoReversion.SOLICITADA)).thenReturn(true);

        PagoDetalleResponse detalle = servicio.detalle(ESCUELA, ESTUDIANTE, pago.getCodigo());

        assertThat(detalle.reembolsable()).isFalse();
        assertThat(detalle.reembolso()).isNull();
        assertThat(detalle.estado()).isEqualTo(EstadoPagoVisible.REEMBOLSO_EN_PROCESO);
    }

    @Test
    void detalle_conUnaReversionEjecutada_noEsReembolsable() {
        Pago pago = pago(EstadoPago.CONFIRMADO, 10, 10);
        existe(pago);
        // * El service pregunta primero por la solicitada (false por defecto) y despues por la ejecutada
        lenient().when(reversiones.existsByPagoIdAndEstado(pago.getId(), EstadoReversion.EJECUTADA)).thenReturn(true);

        assertThat(servicio.detalle(ESCUELA, ESTUDIANTE, pago.getCodigo()).reembolsable()).isFalse();
    }

    @Test
    void detalle_deUnPagoRevertido_seVeReembolsado_yNoEsReembolsable() {
        Pago revertido = pago(EstadoPago.REVERTIDO, 10, 0);
        existe(revertido);

        PagoDetalleResponse detalle = servicio.detalle(ESCUELA, ESTUDIANTE, revertido.getCodigo());

        assertThat(detalle.estado()).isEqualTo(EstadoPagoVisible.REEMBOLSADO);
        assertThat(detalle.reembolsable()).isFalse();
    }
}
