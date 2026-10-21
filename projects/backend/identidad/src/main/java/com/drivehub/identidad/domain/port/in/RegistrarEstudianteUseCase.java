package com.drivehub.identidad.domain.port.in;

import com.drivehub.identidad.domain.model.Usuario;

/**
 * Caso de uso CU01: un estudiante crea su propia cuenta, asociada a una escuela.
 */
public interface RegistrarEstudianteUseCase {

    Usuario registrar(RegistrarEstudianteComando comando);
}
