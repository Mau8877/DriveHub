package com.drivehub.academico.features.reservas.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

/**
 * Acceso a las reservas (CU14). Las consultas con filtros por fecha y hora, el cambio de estado atómico y la
 * búsqueda de solapes están en {@link ReservaRepositoryCustom}.
 */
public interface ReservaRepository extends MongoRepository<Reserva, Long>, ReservaRepositoryCustom {

    Optional<Reserva> findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(UUID codigoReserva,
            UUID estudianteCodigo);

    /**
     * La reserva que el estudiante ya tiene sobre esa franja en ese estado: hace inocuo repetir la confirmación.
     */
    Optional<Reserva> findByFranjaIdAndEstudianteCodigoAndEstadoAndDeletedAtIsNull(Long franjaId,
            UUID estudianteCodigo, EstadoReserva estado);
}
