package com.drivehub.escuela.domain.repository;

import java.util.UUID;

/**
 * Lectura de las asignaciones de alumno a instructor (CU29). Las asignaciones las crea CU10: acá solo se leen.
 *
 * * Una asignación está vigente mientras no tiene `fechaFin`.
 * * El instructor se identifica por el código de su cuenta (`usuarioCodigo`, el `sub` del token): es lo que
 * * el instructor tiene a mano, no el código de su ficha.
 */
public interface AsignacionAlumnoRepository {

    /**
     * Los códigos de los estudiantes con asignación vigente a ese instructor de esa escuela, en el orden en que
     * se asignaron (primero el más antiguo).
     *
     * @param pagina 1-indexada
     */
    Pagina<UUID> buscarEstudiantesDeInstructor(UUID escuelaCodigo, UUID instructorUsuarioCodigo, int pagina,
            int tamano);

    /** Si ese estudiante tiene una asignación vigente a ese instructor de esa escuela. */
    boolean existeAsignacionActiva(UUID escuelaCodigo, UUID instructorUsuarioCodigo, UUID estudianteCodigo);
}
