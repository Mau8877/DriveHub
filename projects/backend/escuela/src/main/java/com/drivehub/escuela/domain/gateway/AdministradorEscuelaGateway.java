package com.drivehub.escuela.domain.gateway;

import java.util.UUID;

/**
 * Lo que Escuela necesita pedirle a Identidad, el dueño de las cuentas, para crear una
 * escuela con su Administrador (CU05).
 *
 * * Es el contrato hacia OTRO servicio: vive en `domain/gateway/` y su implementación en
 * * `infrastructure/client/`, igual que los repositorios (`domain/repository/`) y sus
 * * implementaciones de persistencia.
 */
public interface AdministradorEscuelaGateway {

    /**
     * Crea la cuenta del Administrador de la escuela con ese codigo, que todavía no existe en
     * Escuela (se confirma después). Si Identidad la rechaza por una regla de negocio (correo en
     * uso, menor de edad...) lanza la excepción de negocio equivalente con su mismo código; si no
     * responde o falla, lanza {@link IdentidadNoDisponibleException}.
     */
    AdministradorCreado crearAdministrador(UUID escuelaCodigo, DatosAdministrador datos);

    /**
     * Deshace la creación del Administrador de esa escuela. Es idempotente: si no hay nada que
     * revertir no falla. Es la compensación de {@link #crearAdministrador}.
     */
    void revertirAdministrador(UUID escuelaCodigo);
}
