package com.drivehub.academico.features.reportes.repository;

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
import org.springframework.stereotype.Repository;

import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.reservas.model.Reserva;

import lombok.RequiredArgsConstructor;

/**
 * Las clases (reservas) de una escuela, para el reporte del Administrador (CU52). Solo lectura.
 *
 * * La reserva no guarda la escuela: es la de su instructor, que se conoce por las franjas de la escuela. Por eso se
 * * buscan primero los instructores con franjas en esa escuela y después sus reservas.
 * ! MongoDB no filtra el soft delete solo: `deletedAt` se pregunta acá explícitamente.
 */
@Repository
@RequiredArgsConstructor
public class ReporteClasesRepository {

    private final MongoTemplate mongoTemplate;

    public Page<Reserva> listar(UUID escuelaCodigo, FiltroReporteClases filtro, Pageable pageable) {
        List<UUID> instructores = mongoTemplate.findDistinct(
            Query.query(Criteria.where("escuelaCodigo").is(escuelaCodigo)), "instructorCodigo", Franja.class, UUID.class);
        if (instructores.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Criteria> condiciones = new ArrayList<>();
        condiciones.add(Criteria.where("deletedAt").is(null));
        if (filtro.instructorCodigo() != null) {
            if (!instructores.contains(filtro.instructorCodigo())) {
                return Page.empty(pageable);
            }
            condiciones.add(Criteria.where("instructorCodigo").is(filtro.instructorCodigo()));
        } else {
            condiciones.add(Criteria.where("instructorCodigo").in(instructores));
        }
        if (filtro.desde() != null || filtro.hasta() != null) {
            Criteria fecha = Criteria.where("fecha");
            if (filtro.desde() != null) {
                fecha = fecha.gte(filtro.desde());
            }
            if (filtro.hasta() != null) {
                fecha = fecha.lte(filtro.hasta());
            }
            condiciones.add(fecha);
        }
        if (filtro.estado() != null) {
            condiciones.add(Criteria.where("estado").is(filtro.estado()));
        }

        Query consulta = new Query(new Criteria().andOperator(condiciones));
        List<Reserva> pagina = mongoTemplate.find(
            Query.of(consulta).with(pageable).with(Sort.by(Sort.Direction.DESC, "fecha", "horaInicio")), Reserva.class);
        return PageableExecutionUtils.getPage(pagina, pageable,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), Reserva.class));
    }
}
