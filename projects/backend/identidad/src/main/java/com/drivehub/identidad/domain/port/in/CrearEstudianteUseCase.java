package com.drivehub.identidad.domain.port.in;

/**
 * CU08 (F2): el Administrador de escuela da de alta a un estudiante en su escuela, con su cuenta y
 * su perfil.
 */
public interface CrearEstudianteUseCase {

    EstudianteDetalle crear(CrearEstudianteComando comando);
}
