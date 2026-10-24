package com.drivehub.escuela.domain.gateway;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Lo que Escuela le pide a Identidad sobre la cuenta y el perfil de un instructor (CU09).
 *
 * * Todo se hace con el token del Administrador que opera: Identidad vuelve a validar el rol y toma
 * * la escuela de su cuenta.
 * * Un rechazo de negocio de Identidad llega como la excepción equivalente con su mismo código; un
 * * fallo de servidor, un timeout o un error de red, como IdentidadNoDisponibleException.
 */
public interface InstructorCuentaGateway {

    /**
     * Crea la cuenta (rol INSTRUCTOR) y el perfil con el `usuarioCodigo` que trae `datos`.
     *
     * @throws com.drivehub.escuela.common.exception.ConflictException `ERR_IDE_01` correo en uso, `ERR_IDE_19` CI en uso
     * @throws com.drivehub.escuela.common.exception.ValidationException `ERR_IDE_02` menor de edad
     */
    PersonaInstructor crearCuenta(DatosCuentaInstructor datos);

    /**
     * Deshace {@link #crearCuenta}. Idempotente: si la cuenta no existe, no pasa nada.
     *
     * @throws com.drivehub.escuela.common.exception.ConflictException `ERR_IDE_23` si la cuenta ya inició sesión
     */
    void revertirCuenta(UUID usuarioCodigo);

    /**
     * Las personas de esos códigos que existen en la escuela. Parte la consulta en lotes de 100; una
     * lista vacía no llama a nadie. Una persona que Identidad no devuelve simplemente no vuelve.
     */
    List<PersonaInstructor> consultar(Collection<UUID> usuarioCodigos);

    /**
     * Los códigos de las personas cuyo nombre coincide con la búsqueda. Pide páginas de 100 hasta
     * agotarlas o llegar a 5 (tope de 500 coincidencias; si se corta, se registra un WARN).
     */
    List<UUID> buscarCodigos(String busqueda);

    /**
     * Cambia la ubicación de la persona (Identidad recalcula la dirección).
     *
     * @throws com.drivehub.escuela.common.exception.NotFoundException `ERR_IDE_22` si no es un instructor de la escuela
     */
    PersonaInstructor cambiarUbicacion(UUID usuarioCodigo, BigDecimal latitud, BigDecimal longitud);
}
