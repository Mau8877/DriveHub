package com.drivehub.academico.features.clases.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.model.PruebaClase;

/**
 * Acceso a las pruebas de clase (CU18). En Spring Data no hay filtro automático de soft delete: cada consulta descarta
 * las borradas.
 */
public interface PruebaClaseRepository extends MongoRepository<PruebaClase, Long> {

    /** La prueba de un tipo en una clase: hay a lo sumo una. */
    Optional<PruebaClase> findByClaseIdAndTipoAndDeletedAtIsNull(Long claseId, TipoPrueba tipo);

    /** Las pruebas de una clase, paginadas. */
    Page<PruebaClase> findByClaseIdAndDeletedAtIsNull(Long claseId, Pageable pageable);

    /** Las pruebas de varias clases de una vez, para la agenda. */
    List<PruebaClase> findByClaseIdInAndDeletedAtIsNull(Collection<Long> claseIds);
}
