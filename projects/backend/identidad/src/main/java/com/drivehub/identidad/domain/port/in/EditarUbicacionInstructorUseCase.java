package com.drivehub.identidad.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CU09: cambiar la ubicación (coordenadas y dirección) de un instructor de la escuela del Administrador.
 */
public interface EditarUbicacionInstructorUseCase {

    /**
     * Solo cambia la ubicación: ningún otro dato de la persona ni de la cuenta.
     *
     * @throws com.drivehub.identidad.common.exception.NotFoundException `ERR_IDE_22` si no es un instructor de la escuela
     * @throws com.drivehub.identidad.common.exception.ValidationException `ERR_SYS_01` si falta alguna coordenada
     */
    PersonaInstructor editar(UUID administradorCodigo, UUID instructorCodigo, BigDecimal latitud, BigDecimal longitud);
}
