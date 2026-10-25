package com.drivehub.pagos.features.comprobantes.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.drivehub.pagos.features.comprobantes.dto.DatosComprobante;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

/**
 * Dibuja el PDF de un comprobante (CU30) con la plantilla Jasper `reportes/comprobante.jrxml` (diseño "Ruta", A4).
 *
 * * Un solo diseño para la compra y el reembolso: cambian las filas y el color de acento.
 * * La plantilla se compila UNA vez, al crear el componente; cada comprobante solo la llena.
 * * Las fuentes (Inter y JetBrains Mono) van dentro del proyecto: el contenedor no depende de las del sistema.
 * * Montos y fechas se formatean acá: `1.250,00`, `US$ 38,23`, `18 oct 2026 · 10:42` en hora de Bolivia.
 * * Un dato vacío se muestra como «—». Una falla de Jasper se relanza como {@link IllegalStateException}.
 * ! Nada de lo que recibe se registra: son datos personales.
 */
@Component
public class ComprobantePdfGenerador {

    static {
        // * El servicio no tiene pantalla: Java2D tiene que trabajar sin ella
        System.setProperty("java.awt.headless", "true");
    }

    private static final String PLANTILLA = "reportes/comprobante.jrxml";
    private static final String MARCA = "reportes/marca/marca-d.svg";
    private static final String VACIO = "—";
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov",
        "dic"};
    private static final DecimalFormatSymbols SIMBOLOS = simbolos();

    private final JasperReport plantilla;

    public ComprobantePdfGenerador() {
        try (InputStream entrada = new ClassPathResource(PLANTILLA).getInputStream()) {
            this.plantilla = JasperCompileManager.compileReport(entrada);
        } catch (IOException | JRException e) {
            throw new IllegalStateException("No se pudo compilar la plantilla de los comprobantes", e);
        }
    }

    /** La plantilla ya compilada, la misma en cada llamada (para comprobar que no se vuelve a compilar). */
    public JasperReport plantilla() {
        return plantilla;
    }

    public byte[] generar(DatosComprobante datos) {
        try {
            JasperPrint impresion = JasperFillManager.fillReport(plantilla, parametros(datos),
                new JREmptyDataSource(1));
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(impresion, salida);
            return salida.toByteArray();
        } catch (JRException | RuntimeException e) {
            throw new IllegalStateException("No se pudo generar el PDF del comprobante", e);
        }
    }

    private static Map<String, Object> parametros(DatosComprobante d) {
        Map<String, Object> p = new HashMap<>();
        p.put("ES_REEMBOLSO", d.esReembolso());
        p.put("MARCA_D", MARCA);
        p.put("ESCUELA", o(d.escuelaNombre()));
        p.put("ESTUDIANTE", o(d.estudianteNombre()));
        p.put("NUMERO", o(d.numero()));
        p.put("LINEA_C2", "");
        p.put("LINEA_D2", "");
        p.put("LINEA_V2", "");

        String tipoCambio = "Bs " + numero(d.tipoCambio()) + " por US$ 1";
        if (d.esReembolso()) {
            reembolso(p, d, tipoCambio);
        } else {
            compra(p, d, tipoCambio);
        }
        return p;
    }

    private static void compra(Map<String, Object> p, DatosComprobante d, String tipoCambio) {
        p.put("ETIQUETA", "DriveHub · Compra");
        p.put("TITULO", "Comprobante de pago");
        dato(p, 1, "Fecha de emisión", fecha(d.fecha()));
        dato(p, 2, "Referencia del pago", o(d.pagoReferencia()));
        dato(p, 3, "Método de pago", o(d.metodoPago()));
        dato(p, 4, "Tipo de cambio aplicado", tipoCambio);
        dato(p, 5, "Estado", "Pago confirmado");
        p.put("LINEA_C1", o(d.planNombre()));
        p.put("LINEA_D1", horas(d.horas()) + " de práctica de manejo acreditadas a tu cuenta");
        p.put("LINEA_V1", usd(d.montoUsd()));
        p.put("TOTAL_ETIQUETA", "Total pagado");
        p.put("TOTAL_NOTA", "Costo del plan: " + bs(d.montoBs()));
        p.put("TOTAL_USD", usd(d.montoUsd()));
        p.put("TOTAL_BS", bs(d.montoBs()));
    }

    private static void reembolso(Map<String, Object> p, DatosComprobante d, String tipoCambio) {
        p.put("ETIQUETA", "DriveHub · Reembolso");
        p.put("TITULO", "Comprobante de reembolso");
        dato(p, 1, "Fecha de emisión", fecha(d.fecha()));
        // * Una compra anterior a los comprobantes no tiene número: se muestra solo su fecha, sin un guion suelto
        String numeroOriginal = d.comprobanteOriginalNumero();
        String fechaCompra = fecha(d.fechaCompra(), false);
        dato(p, 2, "Compra original", numeroOriginal == null || numeroOriginal.isBlank() ? fechaCompra
            : numeroOriginal.trim() + " · " + fechaCompra);
        dato(p, 3, "Referencia del pago", o(d.pagoReferencia()));
        dato(p, 4, "Devolución", "A la tarjeta usada en la compra");
        dato(p, 5, "Motivo del reembolso", o(d.motivo()));
        p.put("LINEA_C1", "Monto original pagado");
        p.put("LINEA_D1", o(d.planNombre()) + " · " + bs(d.montoBs()) + " al tipo de cambio de Bs "
            + numero(d.tipoCambio()));
        p.put("LINEA_V1", usd(d.montoOriginal()));
        boolean hayRetencion = d.montoRetenido() != null && d.montoRetenido().signum() > 0;
        if (hayRetencion) {
            p.put("LINEA_C2", "Retención por reembolso");
            p.put("LINEA_D2", d.porcentajeRetenido() + " % del monto pagado");
            p.put("LINEA_V2", "− " + usd(d.montoRetenido()));
        }
        p.put("TOTAL_ETIQUETA", "Monto devuelto");
        p.put("TOTAL_NOTA", "Referencial en bolivianos");
        p.put("TOTAL_USD", usd(d.montoDevuelto()));
        p.put("TOTAL_BS", bs(d.montoDevueltoBs()));
    }

    private static void dato(Map<String, Object> p, int n, String clave, String valor) {
        p.put("DATO_K" + n, clave);
        p.put("DATO_V" + n, valor);
    }

    private static String o(String texto) {
        return texto == null || texto.isBlank() ? VACIO : texto.trim();
    }

    private static String horas(int horas) {
        return horas == 1 ? "1 hora" : horas + " horas";
    }

    private static String usd(BigDecimal monto) {
        return monto == null ? VACIO : "US$ " + numero(monto);
    }

    private static String bs(BigDecimal monto) {
        return monto == null ? VACIO : "Bs " + numero(monto);
    }

    /** `1.250,00`: miles con punto y dos decimales con coma, igual en cualquier servidor. */
    private static String numero(BigDecimal monto) {
        if (monto == null) {
            return VACIO;
        }
        return new DecimalFormat("#,##0.00", SIMBOLOS).format(monto);
    }

    private static String fecha(Instant instante) {
        return fecha(instante, true);
    }

    /** `18 oct 2026 · 10:42` en hora de Bolivia; sin la hora, `18 oct 2026`. */
    private static String fecha(Instant instante, boolean conHora) {
        if (instante == null) {
            return VACIO;
        }
        ZonedDateTime f = instante.atZone(ZONA);
        String dia = String.format(Locale.ROOT, "%02d %s %d", f.getDayOfMonth(), MESES[f.getMonthValue() - 1],
            f.getYear());
        return conHora ? dia + String.format(Locale.ROOT, " · %02d:%02d", f.getHour(), f.getMinute()) : dia;
    }

    private static DecimalFormatSymbols simbolos() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return s;
    }
}
