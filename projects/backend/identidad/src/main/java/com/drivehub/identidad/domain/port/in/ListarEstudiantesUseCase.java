package com.drivehub.identidad.domain.port.in;

/**
 * CU08: el Administrador de escuela lista los estudiantes de su escuela.
 */
public interface ListarEstudiantesUseCase {

    PaginaEstudiantes listar(ListarEstudiantesComando comando);
}
