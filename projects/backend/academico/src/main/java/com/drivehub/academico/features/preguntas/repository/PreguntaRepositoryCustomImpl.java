package com.drivehub.academico.features.preguntas.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import com.drivehub.academico.features.preguntas.model.Pregunta;

import lombok.RequiredArgsConstructor;

/**
 * Spring Data la une a PreguntaRepository por el sufijo `Impl`.
 *
 * ! MongoDB no filtra el soft delete solo: `deletedAt` se pregunta acá explícitamente.
 */
@RequiredArgsConstructor
public class PreguntaRepositoryCustomImpl implements PreguntaRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Pregunta> activasDelTema(UUID escuelaCodigo, java.util.Collection<String> categorias, String tema) {
        Query consulta = new Query(new Criteria().andOperator(
            Criteria.where("escuelaCodigo").is(escuelaCodigo),
            Criteria.where("deletedAt").is(null),
            Criteria.where("activa").is(true),
            Criteria.where("categoria").in(categorias),
            Criteria.where("tema").regex("^" + Pattern.quote(tema.trim()) + "$", "i")));
        return mongoTemplate.find(consulta, Pregunta.class);
    }

    @Override
    public Page<Pregunta> listar(UUID escuelaCodigo, FiltroPreguntas filtro, Pageable pageable) {
        List<Criteria> condiciones = new ArrayList<>();
        condiciones.add(Criteria.where("escuelaCodigo").is(escuelaCodigo));
        condiciones.add(Criteria.where("deletedAt").is(null));
        if (filtro.categoria() != null && !filtro.categoria().isBlank()) {
            condiciones.add(Criteria.where("categoria").is(filtro.categoria()));
        }
        if (filtro.tema() != null && !filtro.tema().isBlank()) {
            condiciones.add(Criteria.where("tema").is(filtro.tema()));
        }
        if (filtro.tipo() != null) {
            condiciones.add(Criteria.where("tipo").is(filtro.tipo()));
        }
        if (filtro.dificultad() != null) {
            condiciones.add(Criteria.where("dificultad").is(filtro.dificultad()));
        }
        if (filtro.activa() != null) {
            condiciones.add(Criteria.where("activa").is(filtro.activa()));
        }
        if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            String texto = Pattern.quote(filtro.busqueda().trim());
            condiciones.add(new Criteria().orOperator(
                Criteria.where("enunciado").regex(texto, "i"),
                Criteria.where("tema").regex(texto, "i")));
        }

        Query consulta = new Query(new Criteria().andOperator(condiciones));
        List<Pregunta> pagina = mongoTemplate.find(
            Query.of(consulta).with(pageable).with(Sort.by(Sort.Direction.DESC, "createdAt", "id")),
            Pregunta.class);
        return PageableExecutionUtils.getPage(pagina, pageable,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), Pregunta.class));
    }
}
