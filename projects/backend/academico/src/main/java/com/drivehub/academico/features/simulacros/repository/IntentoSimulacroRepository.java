package com.drivehub.academico.features.simulacros.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.drivehub.academico.features.simulacros.enums.EstadoIntento;
import com.drivehub.academico.features.simulacros.model.IntentoSimulacro;

/**
 * Acceso a la colección `intentos_simulacro`, siempre dentro de un estudiante: el intento de otro estudiante es,
 * para quien consulta, uno que no existe. Sin lógica de negocio.
 */
public interface IntentoSimulacroRepository extends MongoRepository<IntentoSimulacro, Long> {

    Optional<IntentoSimulacro> findByEstudianteCodigoAndCodigoIntentoSimulacroAndDeletedAtIsNull(
        UUID estudianteCodigo, UUID codigoIntentoSimulacro);

    /** Los intentos en ese estado de un estudiante en un simulacro (como mucho uno en curso). */
    List<IntentoSimulacro> findByEstudianteCodigoAndSimulacroCodigoAndEstadoAndDeletedAtIsNull(
        UUID estudianteCodigo, UUID simulacroCodigo, EstadoIntento estado);

    /** El historial del estudiante, del más reciente al más antiguo; sin los que siguen en curso. */
    Page<IntentoSimulacro> findByEstudianteCodigoAndEstadoNotAndDeletedAtIsNullOrderByIniciadoEnDesc(
        UUID estudianteCodigo, EstadoIntento estado, Pageable pageable);
}
