package com.drivehub.escuela.domain.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.Instructor;

/**
 * Lo que el negocio necesita saber de las fichas de los instructores (CU09). Todo lo que se lee está
 * aislado por escuela: nunca devuelve la ficha de otra.
 */
public interface InstructorRepository {

    /**
     * Crea la ficha y sus categorías con el `codigo` que trae. El UNIQUE del número de licencia se
     * valida en el mismo instante: una carrera entre dos altas falla acá y no al confirmar.
     *
     * @throws com.drivehub.escuela.common.exception.ConflictException `ERR_ESC_12` si el número ya está en uso
     */
    Instructor guardar(Instructor instructor);

    /**
     * La ficha con ese código dentro de esa escuela, con sus categorías. Vacío si no existe o es de
     * otra escuela: no se distinguen.
     */
    Optional<Instructor> buscarPorCodigo(UUID escuelaCodigo, UUID codigo);

    /**
     * Las fichas de la escuela de la más reciente a la más antigua (`created_at DESC, id DESC`), con
     * sus categorías, sin N+1.
     *
     * @param estado nulo trae los tres estados
     * @param usuarioCodigos nulo no filtra; vacío no devuelve nada
     * @param pagina 1-indexada
     */
    Pagina<Instructor> listar(
            UUID escuelaCodigo, EstadoInstructor estado, Collection<UUID> usuarioCodigos, int pagina, int tamano);

    /** Si algún instructor ya tiene ese número de licencia (ya normalizado). */
    boolean existePorNroLicencia(String nroLicencia);

    /** Si un instructor distinto al de ese código ya tiene ese número de licencia (ya normalizado). */
    boolean existePorNroLicenciaDeOtro(String nroLicencia, UUID codigo);

    /**
     * Guarda número de licencia, vencimiento, fecha de contratación y estado, y reemplaza el conjunto
     * de categorías (agrega las nuevas y quita las que ya no están). No toca `usuarioCodigo` ni
     * `escuelaCodigo`.
     *
     * @throws com.drivehub.escuela.common.exception.ConflictException `ERR_ESC_12` si el número ya es de otro
     */
    Instructor actualizar(Instructor instructor);
}
