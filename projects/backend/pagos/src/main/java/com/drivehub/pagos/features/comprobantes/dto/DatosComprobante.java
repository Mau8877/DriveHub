package com.drivehub.pagos.features.comprobantes.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;

/**
 * Todo lo que lleva el PDF de un comprobante (CU30), ya reunido: el generador no consulta nada.
 *
 * * Sale SOLO de las copias que guarda el pago al comprarse (nombres, plan, horas, montos, tipo de cambio): un
 * * cambio posterior del plan o del tipo de cambio no altera un comprobante.
 * * Los campos de reembolso son nulos en el comprobante de compra.
 * * `motivo` ya viene en español (el texto de la lista, lo que escribió el estudiante si eligió "Otro", o "La
 * * compra no se pudo completar" en una compensación).
 *
 * @param pagoReferencia los primeros 8 caracteres del código del pago, para citarlo
 * @param montoBs costo del plan en bolivianos al comprar
 * @param tipoCambio bolivianos por dólar con que se fijó el monto
 * @param porcentajeRetenido 0 en una compensación automática: el PDF no muestra la fila de retención
 */
public record DatosComprobante(
        TipoComprobante tipo,
        String numero,
        String escuelaNombre,
        String estudianteNombre,
        String planNombre,
        int horas,
        BigDecimal montoUsd,
        BigDecimal montoBs,
        BigDecimal tipoCambio,
        Instant fecha,
        String pagoReferencia,
        String metodoPago,
        String comprobanteOriginalNumero,
        Instant fechaCompra,
        BigDecimal montoOriginal,
        Integer porcentajeRetenido,
        BigDecimal montoRetenido,
        BigDecimal montoDevuelto,
        BigDecimal montoDevueltoBs,
        String motivo) {

    /** Comprobante de la compra de un plan. */
    public static DatosComprobante deCompra(String numero, String escuelaNombre, String estudianteNombre,
            String planNombre, int horas, BigDecimal montoUsd, BigDecimal montoBs, BigDecimal tipoCambio,
            Instant fecha, String pagoReferencia, String metodoPago) {
        return new DatosComprobante(TipoComprobante.PAGO, numero, escuelaNombre, estudianteNombre, planNombre,
            horas, montoUsd, montoBs, tipoCambio, fecha, pagoReferencia, metodoPago, null, null, null, null, null,
            null, null, null);
    }

    /** Comprobante del reembolso de una compra. */
    public static DatosComprobante deReembolso(String numero, String escuelaNombre, String estudianteNombre,
            String planNombre, int horas, BigDecimal montoBs, BigDecimal tipoCambio, Instant fecha,
            String pagoReferencia, String metodoPago, String comprobanteOriginalNumero, Instant fechaCompra,
            BigDecimal montoOriginal, int porcentajeRetenido, BigDecimal montoRetenido, BigDecimal montoDevuelto,
            BigDecimal montoDevueltoBs, String motivo) {
        return new DatosComprobante(TipoComprobante.REEMBOLSO, numero, escuelaNombre, estudianteNombre, planNombre,
            horas, montoDevuelto, montoBs, tipoCambio, fecha, pagoReferencia, metodoPago, comprobanteOriginalNumero,
            fechaCompra, montoOriginal, porcentajeRetenido, montoRetenido, montoDevuelto, montoDevueltoBs, motivo);
    }

    public boolean esReembolso() {
        return tipo == TipoComprobante.REEMBOLSO;
    }
}
