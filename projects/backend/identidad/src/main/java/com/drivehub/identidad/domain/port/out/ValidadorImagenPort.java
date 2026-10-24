package com.drivehub.identidad.domain.port.out;

import com.drivehub.identidad.domain.model.ImagenInvalidaException;

/**
 * Revisión básica de una foto antes de guardarla (CU06). No es un antivirus: comprueba que sea de
 * verdad una imagen completa, legible y del tamaño permitido.
 */
public interface ValidadorImagenPort {

    /**
     * @throws ImagenInvalidaException si el contenido no pasa la revisión, con el motivo
     */
    ImagenValidada validar(byte[] contenido);
}
