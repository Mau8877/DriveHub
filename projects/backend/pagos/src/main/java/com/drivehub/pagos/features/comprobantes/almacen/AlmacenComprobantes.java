package com.drivehub.pagos.features.comprobantes.almacen;

/**
 * Dónde viven los PDF de los comprobantes (CU30): un almacenamiento de objetos PRIVADO. El PDF solo sale por el
 * endpoint autenticado de Pagos; nunca hay direcciones públicas ni prefirmadas.
 */
public interface AlmacenComprobantes {

    /**
     * Guarda (o reemplaza) el PDF bajo esa clave.
     *
     * @throws com.drivehub.pagos.common.exception.ServicioNoDisponibleException `ERR_PAG_15` si el almacén no responde
     */
    void guardar(String clave, byte[] pdf);

    /**
     * Lee el PDF de esa clave.
     *
     * @throws java.util.NoSuchElementException si no hay nada guardado bajo esa clave
     * @throws com.drivehub.pagos.common.exception.ServicioNoDisponibleException `ERR_PAG_15` si el almacén no responde
     */
    byte[] leer(String clave);
}
