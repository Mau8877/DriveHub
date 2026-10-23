package com.drivehub.academico.features.horarios.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;

/**
 * Acceso a los horarios semanales. MongoDB no filtra el soft delete solo:
 * todas las consultas piden `deletedAt` nulo.
 */
public interface PlantillaHorarioRepository extends MongoRepository<PlantillaHorario, Long> {

    Optional<PlantillaHorario> findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            UUID codigoPlantillaHorario, UUID instructorCodigo);

    /**
     * Los horarios del instructor en alguno de esos estados. Se trae la lista completa (un instructor tiene
     * pocos) porque el orden por estado lo hace el service.
     */
    List<PlantillaHorario> findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(
            UUID instructorCodigo, Collection<EstadoPlantilla> estados);

    /**
     * Los horarios del instructor en un estado: con `ACTIVA` es la base de la comprobación de cruce.
     */
    List<PlantillaHorario> findByInstructorCodigoAndEstadoAndDeletedAtIsNull(
            UUID instructorCodigo, EstadoPlantilla estado);

    /**
     * Los horarios de todos los instructores en un estado: con `ACTIVA` los recorre la generación diaria.
     */
    Page<PlantillaHorario> findByEstadoAndDeletedAtIsNull(EstadoPlantilla estado, Pageable pageable);
}
