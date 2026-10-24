package com.drivehub.identidad.domain.port.in;

/**
 * CU09: buscar por nombre las personas instructor de la escuela del Administrador, de a páginas.
 */
public interface BuscarInstructoresUseCase {

    PaginaPersonasInstructor buscar(BuscarInstructoresComando comando);
}
