package com.drivehub.academico.features.horarios.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.horarios.model.PlantillaHorario;

/**
 * Acceso a los horarios semanales. MongoDB no filtra el soft delete solo:
 * todas las consultas piden `deletedAt` nulo.
 */
public interface PlantillaHorarioRepository extends MongoRepository<PlantillaHorario, Long> {

    Optional<PlantillaHorario> findByCodigoPlantillaHorarioAndInstructorCodigoAndDeletedAtIsNull(
            UUID codigoPlantillaHorario, UUID instructorCodigo);

    Page<PlantillaHorario> findByInstructorCodigoAndDeletedAtIsNull(UUID instructorCodigo, Pageable pageable);

    Page<PlantillaHorario> findByInstructorCodigoAndActivaAndDeletedAtIsNull(
            UUID instructorCodigo, boolean activa, Pageable pageable);

    /**
     * Los horarios activos del instructor para un día de la semana: base de la comprobación de cruce.
     */
    List<PlantillaHorario> findByInstructorCodigoAndDiaSemanaAndActivaTrueAndDeletedAtIsNull(
            UUID instructorCodigo, int diaSemana);

    /**
     * Todos los horarios activos, de todos los instructores: los recorre la generación diaria.
     */
    Page<PlantillaHorario> findByActivaTrueAndDeletedAtIsNull(Pageable pageable);
}
