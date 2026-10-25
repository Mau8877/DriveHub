package com.drivehub.pagos.features.comprobantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.drivehub.pagos.PagosPropertiesPrueba;
import com.drivehub.pagos.features.comprobantes.almacen.AlmacenComprobantes;
import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;
import com.drivehub.pagos.features.comprobantes.model.Comprobante;
import com.drivehub.pagos.features.comprobantes.repository.ComprobanteRepository;
import com.drivehub.pagos.features.comprobantes.service.ComprobantePdfGenerador;
import com.drivehub.pagos.features.comprobantes.service.ComprobanteService;
import com.drivehub.pagos.features.comprobantes.service.TextoDeMotivoReversion;
import com.drivehub.pagos.features.pagos.enums.EstadoPago;
import com.drivehub.pagos.features.pagos.model.Pago;
import com.drivehub.pagos.features.pagos.model.ReversionPago;
import com.drivehub.pagos.features.pagos.repository.PagoRepository;
import com.drivehub.pagos.features.pagos.repository.ReversionPagoRepository;
import com.drivehub.pagos.features.planes.model.PlanPago;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Generar los comprobantes (CU30), con repositorios en memoria, el generador real y un almacén falso.
 */
class ComprobanteServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");
    private static ComprobantePdfGenerador generador;

    private final List<Comprobante> guardados = new ArrayList<>();
    private final Map<String, byte[]> almacenado = new HashMap<>();
    private final AtomicLong secuencia = new AtomicLong();
    private final AtomicLong ids = new AtomicLong(100);
    private Instant reloj = AHORA;
    private boolean almacenFalla;
    private Pago pago;
    private ReversionPago reversion;
    private ComprobanteService servicio;
    private ListAppender<ILoggingEvent> registros;
    private PagoRepository pagos;
    private ComprobanteRepository comprobantes;
    private Logger logger;

    @BeforeEach
    void preparar() {
        if (generador == null) {
            generador = new ComprobantePdfGenerador();
        }
        pago = unPago(EstadoPago.CONFIRMADO, 1L);
        reversion = new ReversionPago();
        ReflectionTestUtils.setField(reversion, "id", 7L);
        ReflectionTestUtils.setField(reversion, "codigo", UUID.randomUUID());
        ReflectionTestUtils.setField(reversion, "pago", pago);
        ReflectionTestUtils.setField(reversion, "motivo", "Cambié de planes");

        pagos = mock(PagoRepository.class);
        when(pagos.findByCodigo(pago.getCodigo())).thenReturn(Optional.of(pago));
        ReversionPagoRepository reversiones = mock(ReversionPagoRepository.class);
        when(reversiones.findByCodigo(reversion.getCodigo())).thenReturn(Optional.of(reversion));

        comprobantes = mock(ComprobanteRepository.class);
        when(comprobantes.siguienteNumero()).thenAnswer(i -> secuencia.incrementAndGet());
        when(comprobantes.findByPagoIdAndTipo(anyLong(), any())).thenAnswer(i -> guardados.stream()
            .filter(c -> c.getPago().getId().equals(i.getArgument(0)) && c.getTipo() == i.getArgument(1)).findFirst());
        when(comprobantes.saveAndFlush(any(Comprobante.class))).thenAnswer(i -> {
            Comprobante c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", ids.incrementAndGet());
            ReflectionTestUtils.setField(c, "codigo", UUID.randomUUID());
            ReflectionTestUtils.setField(c, "createdAt", reloj);
            guardados.add(c);
            return c;
        });
        when(comprobantes.findById(anyLong())).thenAnswer(i -> guardados.stream()
            .filter(c -> c.getId().equals(i.getArgument(0))).findFirst());
        when(comprobantes.pendientesAntesDe(any(Instant.class), any(Pageable.class))).thenAnswer(i -> {
            Instant limite = i.getArgument(0);
            Pageable pagina = i.getArgument(1);
            return guardados.stream().filter(c -> !c.tienePdf() && c.getCreatedAt().isBefore(limite))
                .limit(pagina.getPageSize()).toList();
        });

        AlmacenComprobantes almacen = new AlmacenComprobantes() {
            @Override
            public void guardar(String clave, byte[] pdf) {
                if (almacenFalla) {
                    throw new IllegalStateException("MinIO caído");
                }
                almacenado.put(clave, pdf);
            }

            @Override
            public byte[] leer(String clave) {
                return almacenado.get(clave);
            }
        };
        servicio = new ComprobanteService(pagos, reversiones, comprobantes, generador, almacen,
            new TextoDeMotivoReversion(), new Clock() {
                @Override
                public java.time.ZoneId getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return reloj;
                }
            }, mock(PlatformTransactionManager.class));

        logger = (Logger) LoggerFactory.getLogger(ComprobanteService.class);
        registros = new ListAppender<>();
        registros.start();
        logger.addAppender(registros);
    }

    @AfterEach
    void limpiar() {
        logger.detachAppender(registros);
    }

    private static Pago unPago(EstadoPago estado, long id) {
        PlanPago plan = new PlanPago(ESCUELA, "Plan 10 horas", (short) 10, new BigDecimal("450.00"));
        Pago p = new Pago(ESCUELA, UUID.randomUUID(), plan, new BigDecimal("38.23"),
            new BigDecimal(PagosPropertiesPrueba.TIPO_CAMBIO), "María José Peña Núñez", "Autoescuela Cóndor",
            "clave-" + id, AHORA);
        p.setEstado(estado);
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "codigo", UUID.fromString("0198f0a0-1234-7abc-8def-00000000" + String.format("%04x", id)));
        ReflectionTestUtils.setField(p, "createdAt", AHORA);
        return p;
    }

    // ------------------------------------------------------------------ compra

    @Test
    void generarDePago_deUnPagoConfirmado_creaElComprobanteNumeradoConSuPdf() {
        servicio.generarDePago(pago.getCodigo());

        assertThat(guardados).hasSize(1);
        Comprobante c = guardados.get(0);
        assertThat(c.getTipo()).isEqualTo(TipoComprobante.PAGO);
        assertThat(c.getNroComprobante()).isEqualTo("CMP-2026-000001");
        assertThat(c.getMonto()).isEqualByComparingTo("38.23");
        assertThat(c.tienePdf()).isTrue();
        assertThat(c.getUrlPdf()).isEqualTo("comprobantes/" + ESCUELA + "/2026/CMP-2026-000001.pdf");
        assertThat(almacenado.get(c.getUrlPdf())).startsWith(new byte[] {'%', 'P', 'D', 'F'});
    }

    @Test
    void pedirloDosVeces_dejaUnSoloComprobante_yNoVuelveAGuardar() {
        servicio.generarDePago(pago.getCodigo());
        almacenado.clear();

        servicio.generarDePago(pago.getCodigo());

        assertThat(guardados).hasSize(1);
        assertThat(almacenado).isEmpty();
    }

    @Test
    void unPagoQueNoEstaConfirmado_noGeneraComprobante_yRegistraWarn() {
        Pago pendiente = unPago(EstadoPago.PENDIENTE, 2L);
        when(pagos.findByCodigo(pendiente.getCodigo())).thenReturn(Optional.of(pendiente));

        servicio.generarDePago(pendiente.getCodigo());

        assertThat(guardados).isEmpty();
        assertThat(registros.list).anyMatch(e -> e.getLevel().toString().equals("WARN"));
    }

    @Test
    void unPagoInexistente_noGeneraNiFalla() {
        servicio.generarDePago(UUID.randomUUID());

        assertThat(guardados).isEmpty();
    }

    @Test
    void siFallaElAlmacen_elComprobanteQuedaPendiente_sinExcepcion_yElReintentoLoCompleta() {
        almacenFalla = true;

        servicio.generarDePago(pago.getCodigo());

        assertThat(guardados).hasSize(1);
        assertThat(guardados.get(0).tienePdf()).isFalse();

        almacenFalla = false;
        reloj = AHORA.plusSeconds(120);
        int completos = servicio.reintentarPendientes();

        assertThat(completos).isEqualTo(1);
        assertThat(guardados.get(0).tienePdf()).isTrue();
        assertThat(guardados).hasSize(1);
    }

    @Test
    void elReintento_noToca_losQueTienenMenosDeUnMinuto_niPierdeLosQueSiguenFallando() {
        almacenFalla = true;
        servicio.generarDePago(pago.getCodigo());

        reloj = AHORA.plusSeconds(30);
        assertThat(servicio.reintentarPendientes()).isZero();

        reloj = AHORA.plusSeconds(120);
        assertThat(servicio.reintentarPendientes()).isZero();
        assertThat(guardados.get(0).tienePdf()).isFalse();
    }

    @Test
    void siFallaElPdf_PedirloOtraVez_soloReintentaElPdf_sinNumeroNuevo() {
        almacenFalla = true;
        servicio.generarDePago(pago.getCodigo());
        almacenFalla = false;

        servicio.generarDePago(pago.getCodigo());

        assertThat(guardados).hasSize(1);
        assertThat(guardados.get(0).getNroComprobante()).isEqualTo("CMP-2026-000001");
        assertThat(guardados.get(0).tienePdf()).isTrue();
        assertThat(secuencia.get()).isEqualTo(1);
    }

    @Test
    void losNumeros_sonCrecientes_yConElFormatoEsperado() {
        Pago otro = unPago(EstadoPago.CONFIRMADO, 3L);
        when(pagos.findByCodigo(otro.getCodigo())).thenReturn(Optional.of(otro));

        servicio.generarDePago(pago.getCodigo());
        servicio.generarDePago(otro.getCodigo());

        assertThat(guardados).extracting(Comprobante::getNroComprobante)
            .containsExactly("CMP-2026-000001", "CMP-2026-000002");
        assertThat(guardados).allSatisfy(c -> assertThat(c.getNroComprobante()).matches("CMP-\\d{4}-\\d{6}")
            .hasSizeLessThanOrEqualTo(20));
    }

    @Test
    void siDosHilosChocanEnElIndiceUnico_seLeeElQueGano() {
        when(comprobantes.saveAndFlush(any(Comprobante.class)))
            .thenThrow(new DataIntegrityViolationException("uq_comprobante_pago_tipo"));

        servicio.generarDePago(pago.getCodigo());

        // * El que perdió no deja nada propio ni lanza: el ganador completa su PDF
        assertThat(guardados).isEmpty();
    }

    @Test
    void elLog_noTraeNombresNiMontos() {
        almacenFalla = true;
        servicio.generarDePago(pago.getCodigo());

        for (ILoggingEvent e : registros.list) {
            String texto = e.getFormattedMessage() + e.getArgumentArray().length;
            assertThat(texto).doesNotContain("María", "Peña", "38,23", "38.23", "Autoescuela");
            if (e.getArgumentArray() != null) {
                for (Object argumento : e.getArgumentArray()) {
                    assertThat(String.valueOf(argumento)).doesNotContain("María", "38.23", "Autoescuela");
                }
            }
        }
    }

    // ------------------------------------------------------------------ reembolso

    @Test
    void generarDeReembolso_creaElComprobanteLigadoALaReversion_conSusMontosYElMotivo() {
        servicio.generarDePago(pago.getCodigo());

        servicio.generarDeReembolso(pago.getCodigo(), reversion.getCodigo(), new BigDecimal("36.32"),
            new BigDecimal("1.91"));

        assertThat(guardados).hasSize(2);
        Comprobante reembolso = guardados.get(1);
        assertThat(reembolso.getTipo()).isEqualTo(TipoComprobante.REEMBOLSO);
        assertThat(reembolso.getNroComprobante()).isEqualTo("RMB-2026-000002");
        assertThat(reembolso.getReversionPago()).isSameAs(reversion);
        assertThat(reembolso.getMonto()).isEqualByComparingTo("36.32");
        assertThat(reembolso.tienePdf()).isTrue();
    }

    @Test
    void elPdfDelReembolso_traeElOriginal_laRetencion_elDevuelto_yElMotivo() throws Exception {
        servicio.generarDePago(pago.getCodigo());
        servicio.generarDeReembolso(pago.getCodigo(), reversion.getCodigo(), new BigDecimal("36.32"),
            new BigDecimal("1.91"));

        byte[] pdf = almacenado.get(guardados.get(1).getUrlPdf());
        try (org.apache.pdfbox.pdmodel.PDDocument documento = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String texto = new org.apache.pdfbox.text.PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
            assertThat(texto).contains("CMP-2026-000001", "US$ 38,23", "US$ 1,91", "US$ 36,32", "5 % del monto pagado",
                "Cambié de planes", "Bs 427,49");
        }
    }

    @Test
    void unReembolsoSinMotivoYSinRetencion_esUnaCompensacion() throws Exception {
        ReflectionTestUtils.setField(reversion, "motivo", null);
        servicio.generarDePago(pago.getCodigo());

        servicio.generarDeReembolso(pago.getCodigo(), reversion.getCodigo(), new BigDecimal("38.23"), BigDecimal.ZERO);

        byte[] pdf = almacenado.get(guardados.get(1).getUrlPdf());
        try (org.apache.pdfbox.pdmodel.PDDocument documento = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String texto = new org.apache.pdfbox.text.PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
            assertThat(texto).contains("La compra no se pudo completar").doesNotContain("Retención por reembolso");
        }
    }

    @Test
    void unReembolsoDeUnaReversionAjenaAlPago_noGeneraNada() {
        Pago otro = unPago(EstadoPago.CONFIRMADO, 9L);
        when(pagos.findByCodigo(otro.getCodigo())).thenReturn(Optional.of(otro));

        servicio.generarDeReembolso(otro.getCodigo(), reversion.getCodigo(), new BigDecimal("36.32"),
            new BigDecimal("1.91"));

        assertThat(guardados).isEmpty();
    }

    @Test
    void siFallaElAlmacenAlGuardar_ningunaExcepcionSale() {
        almacenFalla = true;

        servicio.generarDeReembolso(pago.getCodigo(), reversion.getCodigo(), new BigDecimal("36.32"),
            new BigDecimal("1.91"));

        assertThat(guardados).hasSize(1);
        assertThat(guardados.get(0).tienePdf()).isFalse();
    }
}
