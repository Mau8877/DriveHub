package com.drivehub.pagos.features.comprobantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.common.exception.ConflictException;
import com.drivehub.pagos.common.exception.NotFoundException;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;
import com.drivehub.pagos.config.BearerActual;
import com.drivehub.pagos.features.comprobantes.almacen.AlmacenComprobantes;
import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;
import com.drivehub.pagos.features.comprobantes.model.Comprobante;
import com.drivehub.pagos.features.comprobantes.repository.ComprobanteRepository;
import com.drivehub.pagos.features.comprobantes.service.ComprobanteDescargaService;
import com.drivehub.pagos.features.pagos.client.EscuelaClient;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.planes.model.PlanPago;

/**
 * Quién puede descargar un comprobante (CU30). La autorización por rol NO se prueba acá (un servicio sin Spring no
 * ejecuta @PreAuthorize): ver `ComprobanteDescargaAutorizacionTest`.
 */
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceDescargaTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final String BEARER = "Bearer token-del-instructor";
    private static final byte[] PDF = {'%', 'P', 'D', 'F', 1, 2, 3};

    @Mock
    private ComprobanteRepository comprobantes;
    @Mock
    private AlmacenComprobantes almacen;
    @Mock
    private EscuelaClient escuelaClient;
    @Mock
    private BearerActual bearerActual;

    private ComprobanteDescargaService servicio;
    private Pago pago;

    @BeforeEach
    void preparar() {
        servicio = new ComprobanteDescargaService(comprobantes, almacen, escuelaClient, bearerActual);
        pago = pagoDe(ESCUELA, ESTUDIANTE);
    }

    private static Pago pagoDe(UUID escuela, UUID estudiante) {
        Pago p = new Pago(escuela, estudiante, new PlanPago(escuela, "Plan 10 horas", (short) 10,
            new BigDecimal("450.00")), new BigDecimal("38.23"), new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO),
            "Ana", "Escuela", "clave", Instant.parse("2026-10-25T12:00:00Z"));
        p.setEstado(EstadoPago.CONFIRMADO);
        return p;
    }

    private Comprobante existe(Pago deQuien, TipoComprobante tipo, String clavePdf) {
        Comprobante c = new Comprobante(deQuien, tipo, null, "CMP-2026-000184", new BigDecimal("38.23"),
            Instant.parse("2026-10-25T12:00:00Z"));
        ReflectionTestUtils.setField(c, "codigo", UUID.randomUUID());
        c.setUrlPdf(clavePdf);
        when(comprobantes.findByCodigo(c.getCodigo())).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void elEstudianteDueno_obtieneElPdf_conSuNumero() {
        Comprobante c = existe(pago, TipoComprobante.PAGO, "claves/a.pdf");
        when(almacen.leer("claves/a.pdf")).thenReturn(PDF);

        ComprobanteDescargaService.Pdf pdf = servicio.obtener(ESCUELA, ESTUDIANTE, false, c.getCodigo());

        assertThat(pdf.numero()).isEqualTo("CMP-2026-000184");
        assertThat(pdf.bytes()).isEqualTo(PDF);
        verifyNoInteractions(escuelaClient);
    }

    @Test
    void elInstructorConElAlumnoAsignado_obtieneElMismoPdf() {
        Comprobante c = existe(pago, TipoComprobante.PAGO, "claves/a.pdf");
        when(almacen.leer("claves/a.pdf")).thenReturn(PDF);
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(escuelaClient.esMiAlumno(ESTUDIANTE, BEARER)).thenReturn(true);

        assertThat(servicio.obtener(ESCUELA, INSTRUCTOR, true, c.getCodigo()).bytes()).isEqualTo(PDF);
    }

    @Test
    void elInstructorSinAsignacion_recibeErrPag13_sinLeerElAlmacen() {
        Comprobante c = existe(pago, TipoComprobante.PAGO, "claves/a.pdf");
        when(bearerActual.cabecera()).thenReturn(BEARER);
        when(escuelaClient.esMiAlumno(ESTUDIANTE, BEARER)).thenReturn(false);

        assertThatThrownBy(() -> servicio.obtener(ESCUELA, INSTRUCTOR, true, c.getCodigo()))
            .isInstanceOfSatisfying(NotFoundException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_13"));
        verify(almacen, never()).leer(any());
    }

    @Test
    void otroEstudiante_recibeErrPag13_ySinPreguntarleANadie() {
        Comprobante c = existe(pago, TipoComprobante.PAGO, "claves/a.pdf");

        assertThatThrownBy(() -> servicio.obtener(ESCUELA, UUID.randomUUID(), false, c.getCodigo()))
            .isInstanceOfSatisfying(NotFoundException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_13"));
        verifyNoInteractions(escuelaClient, almacen);
    }

    @Test
    void unComprobanteDeOtraEscuela_recibeErrPag13_aunqueSeaSuyo() {
        Pago deOtraEscuela = pagoDe(UUID.randomUUID(), ESTUDIANTE);
        Comprobante c = existe(deOtraEscuela, TipoComprobante.PAGO, "claves/a.pdf");

        assertThatThrownBy(() -> servicio.obtener(ESCUELA, ESTUDIANTE, false, c.getCodigo()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unComprobanteInexistente_recibeErrPag13() {
        UUID codigo = UUID.randomUUID();
        when(comprobantes.findByCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(ESCUELA, ESTUDIANTE, false, codigo))
            .isInstanceOfSatisfying(NotFoundException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_13"));
    }

    @Test
    void unComprobantePendiente_recibeErrPag14() {
        Comprobante c = existe(pago, TipoComprobante.PAGO, null);

        assertThatThrownBy(() -> servicio.obtener(ESCUELA, ESTUDIANTE, false, c.getCodigo()))
            .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_14"));
        verifyNoInteractions(almacen);
    }

    @Test
    void elDeReembolso_esOtroComprobanteDelMismoPago() {
        Comprobante compra = existe(pago, TipoComprobante.PAGO, "claves/compra.pdf");
        Comprobante reembolso = existe(pago, TipoComprobante.REEMBOLSO, "claves/reembolso.pdf");
        when(almacen.leer("claves/compra.pdf")).thenReturn(new byte[] {1});
        when(almacen.leer("claves/reembolso.pdf")).thenReturn(new byte[] {2});

        assertThat(servicio.obtener(ESCUELA, ESTUDIANTE, false, compra.getCodigo()).bytes()).containsExactly(1);
        assertThat(servicio.obtener(ESCUELA, ESTUDIANTE, false, reembolso.getCodigo()).bytes()).containsExactly(2);
    }

    @Test
    void siElAlmacenNoTieneElPdf_oNoResponde_esServicioNoDisponible() {
        Comprobante c = existe(pago, TipoComprobante.PAGO, "claves/a.pdf");
        when(almacen.leer("claves/a.pdf")).thenThrow(new NoSuchElementException("falta"));

        assertThatThrownBy(() -> servicio.obtener(ESCUELA, ESTUDIANTE, false, c.getCodigo()))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PAG_15"));
    }
}
