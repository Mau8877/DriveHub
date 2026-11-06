package com.drivehub.identidad.domain.port.in;

/**
 * CU48: comprobar un dato del registro por WhatsApp apenas la persona lo escribe. Acá van solo las reglas que
 * dependen de los datos guardados o de la fecha (edad mínima, correo y CI sin usar); el formato lo valida la
 * entrada con las mismas anotaciones del registro normal.
 */
public interface ValidarDatoRegistroUseCase {

    void validar(DatoRegistro dato);
}
