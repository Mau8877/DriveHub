package com.drivehub.identidad.domain.port.out;

/**
 * Dónde viven las fotos de perfil (CU06). Hoy es MinIO; el dominio solo conoce las claves de los objetos.
 *
 * * Un fallo del almacenamiento se propaga como excepción: el caso de uso decide qué hacer.
 */
public interface AlmacenFotosPort {

    void guardar(String clave, byte[] contenido, String contentType);

    /** Idempotente: borrar una clave que no existe no es un error. */
    void borrar(String clave);

    /** URL prefirmada para leer el objeto desde el navegador o el teléfono; vence según la configuración. */
    String urlDeLectura(String clave);
}
