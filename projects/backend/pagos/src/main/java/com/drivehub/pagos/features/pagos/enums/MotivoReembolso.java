package com.drivehub.pagos.features.pagos.enums;

/**
 * Por qué el estudiante pide el reembolso de una compra (CU31). Lista cerrada: la app y el comprobante muestran el
 * {@link #getTexto() texto en español}; con `OTRO` el estudiante cuenta el motivo con sus palabras.
 */
public enum MotivoReembolso {
    CAMBIO_DE_PLANES("Cambié de planes"),
    MUDANZA("Me mudé de ciudad"),
    SIN_TIEMPO("No tengo tiempo para las clases"),
    COMPRA_POR_ERROR("Compré el plan por error"),
    PROBLEMAS_ECONOMICOS("Tengo problemas económicos"),
    INSATISFECHO("No quedé conforme con el servicio"),
    OTRO("Otro");

    private final String texto;

    MotivoReembolso(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }
}
