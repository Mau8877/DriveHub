package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.domain.repository.AsignacionAlumnoRepository;
import com.drivehub.escuela.domain.repository.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * Lee las asignaciones de alumno (CU29) con el filtro de escuela siempre explícito.
 */
@Repository
@RequiredArgsConstructor
public class AsignacionAlumnoRepositoryImpl implements AsignacionAlumnoRepository {

    private final AsignacionAlumnoJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Pagina<UUID> buscarEstudiantesDeInstructor(UUID escuelaCodigo, UUID instructorUsuarioCodigo, int pagina,
            int tamano) {
        Page<UUID> resultado = jpaRepository.estudiantesVigentes(escuelaCodigo, instructorUsuarioCodigo,
            PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent(), resultado.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeAsignacionActiva(UUID escuelaCodigo, UUID instructorUsuarioCodigo, UUID estudianteCodigo) {
        return jpaRepository.existeVigente(escuelaCodigo, instructorUsuarioCodigo, estudianteCodigo);
    }
}
