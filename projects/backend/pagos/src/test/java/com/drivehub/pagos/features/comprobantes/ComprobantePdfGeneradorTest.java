package com.drivehub.pagos.features.comprobantes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.drivehub.pagos.features.comprobantes.dto.DatosComprobante;
import com.drivehub.pagos.features.comprobantes.service.ComprobantePdfGenerador;

/**
 * El PDF de los comprobantes (CU30), leído con PDFBox. La plantilla se compila una sola vez para toda la clase.
 */
class ComprobantePdfGeneradorTest {

    private static final Instant COMPRA = Instant.parse("2026-10-18T14:42:00Z");
    private static final Instant REEMBOLSO = Instant.parse("2026-10-25T20:08:00Z");

    private static ComprobantePdfGenerador generador;

    @BeforeAll
    static void compilar() {
        generador = new ComprobantePdfGenerador();
    }

    private static DatosComprobante compra(String estudiante) {
        return DatosComprobante.deCompra("CMP-2026-000184", "Autoescuela Cóndor", estudiante, "Plan 10 horas", 10,
            new BigDecimal("38.23"), new BigDecimal("450.00"), new BigDecimal("11.77"), COMPRA, "PAG-0198F0A0",
            "Tarjeta · procesado por Stripe");
    }

    private static DatosComprobante reembolso(int retencion, String motivo) {
        BigDecimal retenido = retencion == 0 ? BigDecimal.ZERO : new BigDecimal("1.91");
        BigDecimal devuelto = retencion == 0 ? new BigDecimal("38.23") : new BigDecimal("36.32");
        return DatosComprobante.deReembolso("RMB-2026-000185", "Autoescuela Cóndor", "María José Peña Núñez",
            "Plan 10 horas", 10, new BigDecimal("450.00"), new BigDecimal("11.77"), REEMBOLSO, "PAG-0198F0A0",
            "Tarjeta · procesado por Stripe", "CMP-2026-000184", COMPRA, new BigDecimal("38.23"), retencion,
            retenido, devuelto, new BigDecimal("427.49"), motivo);
    }

    private static String texto(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
        }
    }

    private static PDDocument abrir(byte[] pdf) throws IOException {
        return Loader.loadPDF(pdf);
    }

    @Test
    void elPdfDeCompra_empiezaConPdf_yTraeLosDatosYLosTresMontos() throws IOException {
        byte[] pdf = generador.generar(compra("María José Peña Núñez"));

        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        String texto = texto(pdf);
        assertThat(texto).contains("Comprobante de pago", "CMP-2026-000184", "Autoescuela Cóndor",
            "María José Peña Núñez", "Plan 10 horas", "10 horas", "US$ 38,23", "Bs 450,00", "Bs 11,77 por US$ 1",
            "18 oct 2026 · 10:42", "PAG-0198F0A0", "Tarjeta · procesado por Stripe");
        assertThat(texto).doesNotContain("Retención", "MONTO DEVUELTO", "Motivo");
    }

    @Test
    void elPdfDeReembolso_sumaOriginalRetencionYDevuelto_ElMotivoYLaCompraOriginal() throws IOException {
        String texto = texto(generador.generar(reembolso(5, "No tengo tiempo para las clases")));

        assertThat(texto).contains("Comprobante de reembolso", "RMB-2026-000185", "Monto original pagado",
            "US$ 38,23", "Retención por reembolso", "5 % del monto pagado", "US$ 1,91", "MONTO DEVUELTO",
            "US$ 36,32", "Bs 427,49", "No tengo tiempo para las clases", "CMP-2026-000184 · 18 oct 2026");
    }

    @Test
    void unMotivoOtro_muestraElTextoEscrito() throws IOException {
        assertThat(texto(generador.generar(reembolso(5, "Me mudé a otra ciudad")))).contains("Me mudé a otra ciudad");
    }

    @Test
    void unaCompensacionSinRetencion_noMuestraLaFilaDeRetencion() throws IOException {
        String texto = texto(generador.generar(reembolso(0, "La compra no se pudo completar")));

        assertThat(texto).doesNotContain("Retención por reembolso");
        assertThat(texto).contains("US$ 38,23", "La compra no se pudo completar");
    }

    @Test
    void laLeyendaLegalCompleta_estaYNoHayDespedidaNiFirma() throws IOException {
        for (byte[] pdf : new byte[][] {generador.generar(compra("Ana Pérez")),
            generador.generar(reembolso(5, "Otro"))}) {
            String texto = texto(pdf);
            assertThat(texto).contains("Documento sin validez fiscal.", "no es una factura, no lleva NIT",
                "no sustituye ningún documento tributario", "datos fijados al momento de la operación");
            assertThat(texto).doesNotContain("Buen camino", "equipo DriveHub");
        }
    }

    @Test
    void tildesYEnie_salenBien() throws IOException {
        String texto = texto(generador.generar(compra("Peña Núñez, Educación Ñandú")));

        assertThat(texto).contains("Peña Núñez, Educación Ñandú");
    }

    @Test
    void unNombreDe160Caracteres_yUnMotivoDe200_noRompenElDiseno() throws IOException {
        String largo = "Nombre ".repeat(23).substring(0, 160);
        String motivoLargo = ("Motivo muy largo " + "palabra ".repeat(40)).substring(0, 200);

        byte[] compra = generador.generar(compra(largo));
        byte[] reembolso = generador.generar(reembolso(5, motivoLargo));

        for (byte[] pdf : new byte[][] {compra, reembolso}) {
            try (PDDocument documento = abrir(pdf)) {
                assertThat(documento.getNumberOfPages()).isEqualTo(1);
            }
        }
        assertThat(texto(compra)).contains("Nombre Nombre");
        assertThat(texto(reembolso)).contains("Motivo muy largo");
    }

    @Test
    void esA4DeUnaPagina() throws IOException {
        try (PDDocument documento = abrir(generador.generar(compra("Ana Pérez")))) {
            assertThat(documento.getNumberOfPages()).isEqualTo(1);
            PDRectangle caja = documento.getPage(0).getMediaBox();
            assertThat(caja.getWidth()).isEqualTo(595f);
            assertThat(caja.getHeight()).isEqualTo(842f);
        }
    }

    @Test
    void unDatoVacio_seMuestraComoRaya() throws IOException {
        DatosComprobante sinMetodo = DatosComprobante.deCompra("CMP-2026-000184", "Autoescuela Cóndor", "Ana Pérez",
            "Plan 10 horas", 1, new BigDecimal("3.82"), new BigDecimal("45.00"), new BigDecimal("11.77"), COMPRA,
            null, " ");

        String texto = texto(generador.generar(sinMetodo));

        assertThat(texto).contains("Referencia del pago —", "Método de pago —", "1 hora de práctica");
    }

    @Test
    void dosLlamadasSeguidas_usanLaMismaPlantillaCompilada() {
        Object primera = generador.plantilla();
        generador.generar(compra("Ana Pérez"));
        generador.generar(reembolso(5, "Otro"));

        assertThat(generador.plantilla()).isSameAs(primera);
    }
}
