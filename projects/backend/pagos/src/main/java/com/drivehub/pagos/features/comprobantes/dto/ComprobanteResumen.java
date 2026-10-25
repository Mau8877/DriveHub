package com.drivehub.pagos.features.comprobantes.dto;

import java.util.UUID;

import com.drivehub.pagos.features.comprobantes.enums.TipoComprobante;

/**
 * Un comprobante de una compra, en su detalle (CU30): para mostrarlo y pedir su PDF. `disponible` es falso mientras
 * el PDF todavía se prepara.
 */
public record ComprobanteResumen(UUID codigo, TipoComprobante tipo, String numero, boolean disponible) {
}
