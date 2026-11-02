package com.drivehub.academico.features.clases.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.clases.model.Clase;

/**
 * Acceso a las clases dictadas (CU17). En Spring Data no hay filtro automático de soft delete: cada consulta
 * descarta las borradas.
 */
public interface ClaseRepository extends MongoRepository<Clase, Long> {

    /** Una clase del instructor por su código público: nunca se revela una ajena. */
    Optional<Clase> findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(UUID codigoClase,
            UUID instructorCodigo);

    /** La clase dictada de una reserva (cerrar de nuevo un cierre interrumpido). */
    Optional<Clase> findByReservaIdAndDeletedAtIsNull(Long reservaId);

    /** Las clases de varias reservas de una vez, para la agenda. */
    List<Clase> findByReservaIdInAndDeletedAtIsNull(Collection<Long> reservaIds);
}
