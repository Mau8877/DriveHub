package com.drivehub.escuela.infrastructure.persistence.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.entity.EstadoInstructor;
import com.drivehub.escuela.domain.entity.Instructor;
import com.drivehub.escuela.domain.repository.InstructorRepository;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.infrastructure.persistence.model.CategoriaLicenciaModel;
import com.drivehub.escuela.infrastructure.persistence.model.InstructorCategoriaModel;
import com.drivehub.escuela.infrastructure.persistence.model.InstructorModel;

import lombok.RequiredArgsConstructor;

/**
 * Guarda y lee las fichas de instructores (CU09) con sus categorías.
 *
 * * Traduce entre la entidad de dominio `Instructor` y el `InstructorModel` JPA: el dominio nunca ve
 * * un Model ni el id Long.
 */
@Repository
@RequiredArgsConstructor
public class InstructorRepositoryImpl implements InstructorRepository {

    private final InstructorJpaRepository jpaRepository;
    private final InstructorCategoriaJpaRepository categoriaJpaRepository;
    private final CategoriaLicenciaJpaRepository catalogoJpaRepository;

    @Override
    @Transactional
    public Instructor guardar(Instructor instructor) {
        InstructorModel model = new InstructorModel(instructor.getCodigo(), instructor.getUsuarioCodigo(),
            instructor.getEscuelaCodigo(), instructor.getNroLicencia(), instructor.getLicenciaVence(),
            instructor.getEstado(), instructor.getFechaContratacion());
        try {
            // * saveAndFlush: el UNIQUE del número de licencia salta acá y no al confirmar la transacción
            model = jpaRepository.saveAndFlush(model);
        } catch (DataIntegrityViolationException e) {
            // * Carrera entre dos altas con el mismo número: la segunda pierde
            throw numeroEnUso();
        }
        List<InstructorCategoriaModel> asociaciones = new ArrayList<>();
        for (CategoriaLicenciaModel categoria : categoriasDe(instructor)) {
            asociaciones.add(categoriaJpaRepository.save(new InstructorCategoriaModel(model, categoria)));
        }
        return aDominio(model, asociaciones);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instructor> buscarPorCodigo(UUID escuelaCodigo, UUID codigo) {
        return jpaRepository.findByCodigoAndEscuelaCodigo(codigo, escuelaCodigo)
            .map(model -> aDominio(model, categoriaJpaRepository.deInstructores(List.of(model.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Instructor> listar(
            UUID escuelaCodigo, EstadoInstructor estado, Collection<UUID> usuarioCodigos, int pagina, int tamano) {
        if (usuarioCodigos != null && usuarioCodigos.isEmpty()) {
            return new Pagina<>(List.of(), 0);
        }
        Specification<InstructorModel> filtro = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("escuelaCodigo"), escuelaCodigo));
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (usuarioCodigos != null) {
                condiciones.add(root.get("usuarioCodigo").in(usuarioCodigos));
            }
            return cb.and(condiciones.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<InstructorModel> resultado = jpaRepository.findAll(filtro,
            PageRequest.of(pagina - 1, tamano, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        List<Long> ids = resultado.getContent().stream().map(InstructorModel::getId).toList();
        Map<Long, List<InstructorCategoriaModel>> categoriasPorInstructor = ids.isEmpty() ? Map.of()
            : categoriaJpaRepository.deInstructores(ids).stream()
                .collect(Collectors.groupingBy(ic -> ic.getInstructor().getId()));
        List<Instructor> instructores = resultado.getContent().stream()
            .map(m -> aDominio(m, categoriasPorInstructor.getOrDefault(m.getId(), List.of()))).toList();
        return new Pagina<>(instructores, resultado.getTotalElements());
    }

    @Override
    public boolean existePorNroLicencia(String nroLicencia) {
        return jpaRepository.existsByNroLicencia(nroLicencia);
    }

    @Override
    public boolean existePorNroLicenciaDeOtro(String nroLicencia, UUID codigo) {
        return jpaRepository.existsByNroLicenciaAndCodigoNot(nroLicencia, codigo);
    }

    @Override
    @Transactional
    public Instructor actualizar(Instructor instructor) {
        InstructorModel model = jpaRepository
            .findByCodigoAndEscuelaCodigo(instructor.getCodigo(), instructor.getEscuelaCodigo())
            .orElseThrow(() -> new IllegalStateException("No existe el instructor a actualizar"));
        model.setNroLicencia(instructor.getNroLicencia());
        model.setLicenciaVence(instructor.getLicenciaVence());
        model.setFechaContratacion(instructor.getFechaContratacion());
        model.setEstado(instructor.getEstado());
        try {
            model = jpaRepository.saveAndFlush(model);
        } catch (DataIntegrityViolationException e) {
            throw numeroEnUso();
        }

        List<InstructorCategoriaModel> actuales = categoriaJpaRepository.deInstructores(List.of(model.getId()));
        Set<String> queridas = instructor.getCategorias().stream()
            .map(c -> c.getSigla().toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        // * Borrado físico a propósito: UNIQUE (instructor_id, categoria_id) incluye las filas con borrado lógico
        // * y la categoría quitada podría volver a agregarse más tarde
        categoriaJpaRepository.deleteAll(actuales.stream()
            .filter(a -> !queridas.contains(a.getCategoria().getSigla().toUpperCase(Locale.ROOT))).toList());
        Set<String> yaGuardadas = actuales.stream()
            .map(a -> a.getCategoria().getSigla().toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        for (CategoriaLicenciaModel categoria : categoriasDe(instructor)) {
            if (!yaGuardadas.contains(categoria.getSigla().toUpperCase(Locale.ROOT))) {
                categoriaJpaRepository.save(new InstructorCategoriaModel(model, categoria));
            }
        }
        categoriaJpaRepository.flush();
        return aDominio(model, categoriaJpaRepository.deInstructores(List.of(model.getId())));
    }

    private List<CategoriaLicenciaModel> categoriasDe(Instructor instructor) {
        List<String> siglas = instructor.getCategorias().stream()
            .map(c -> c.getSigla().toUpperCase(Locale.ROOT)).toList();
        List<CategoriaLicenciaModel> modelos = catalogoJpaRepository.buscarPorSiglas(siglas);
        if (modelos.size() != siglas.size()) {
            throw new IllegalStateException("Alguna categoría del instructor no existe en el catálogo");
        }
        return modelos;
    }

    private ConflictException numeroEnUso() {
        return new ConflictException(EscuelaErrorCodes.NRO_LICENCIA_EN_USO,
            "El número de licencia ya pertenece a otro instructor");
    }

    private Instructor aDominio(InstructorModel model, List<InstructorCategoriaModel> asociaciones) {
        Set<CategoriaLicencia> categorias = new LinkedHashSet<>();
        for (InstructorCategoriaModel asociacion : asociaciones) {
            CategoriaLicenciaModel c = asociacion.getCategoria();
            categorias.add(CategoriaLicencia.builder().codigo(c.getCodigo()).sigla(c.getSigla())
                .descripcion(c.getDescripcion()).edadMinima(c.getEdadMinima())
                .clasesMinimas(c.getClasesMinimas()).build());
        }
        return Instructor.builder()
            .codigo(model.getCodigo())
            .usuarioCodigo(model.getUsuarioCodigo())
            .escuelaCodigo(model.getEscuelaCodigo())
            .nroLicencia(model.getNroLicencia())
            .licenciaVence(model.getLicenciaVence())
            .estado(model.getEstado())
            .fechaContratacion(model.getFechaContratacion())
            .categorias(categorias)
            .creadoEn(model.getCreatedAt())
            .build();
    }
}
