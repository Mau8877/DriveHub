package com.drivehub.pagos.features.pagos.dto;

import com.drivehub.pagos.features.pagos.enums.MotivoReembolso;

import jakarta.validation.constraints.NotNull;

/**
 * Por qué el estudiante pide el reembolso (CU31). `motivoDetalle` solo se usa con `OTRO` (de 5 a 200 caracteres: lo
 * valida el service); con cualquier otro motivo se descarta.
 */
public record SolicitarReembolsoRequest(
        @NotNull(message = "Elige el motivo del reembolso") MotivoReembolso motivo,
        String motivoDetalle) {
}
