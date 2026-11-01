package com.drivehub.academico.features.examenes.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import com.drivehub.academico.features.examenes.model.ExamenConduccion;

import lombok.RequiredArgsConstructor;

/**
 * Spring Data la une a ExamenConduccionRepository por el sufijo `Impl`.
 *
 * ! MongoDB no filtra el soft delete solo: `deletedAt` se pregunta acá explícitamente.
 */
@RequiredArgsConstructor
public class ExamenConduccionRepositoryCustomImpl implements ExamenConduccionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<ExamenConduccion> listar(UUID escuelaCodigo, FiltroExamenes filtro, Pageable pageable) {
        List<Criteria> condiciones = new ArrayList<>();
        condiciones.add(Criteria.where("escuelaCodigo").is(escuelaCodigo));
        condiciones.add(Criteria.where("deletedAt").is(null));
        if (filtro.categoria() != null && !filtro.categoria().isBlank()) {
            condiciones.add(Criteria.where("categoria").is(filtro.categoria()));
        }
        if (filtro.estado() != null) {
            condiciones.add(Criteria.where("estado").is(filtro.estado()));
        }
        if (filtro.estudianteCodigo() != null) {
            condiciones.add(Criteria.where("estudianteCodigo").is(filtro.estudianteCodigo()));
        }

        Query consulta = new Query(new Criteria().andOperator(condiciones));
        List<ExamenConduccion> pagina = mongoTemplate.find(
            Query.of(consulta).with(pageable).with(Sort.by(Sort.Direction.DESC, "fechaProgramada", "id")),
            ExamenConduccion.class);
        return PageableExecutionUtils.getPage(pagina, pageable,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), ExamenConduccion.class));
    }
}
