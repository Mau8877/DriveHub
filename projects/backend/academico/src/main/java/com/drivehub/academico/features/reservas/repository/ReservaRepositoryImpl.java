package com.drivehub.academico.features.reservas.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;

import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.HistorialEstado;
import com.drivehub.academico.features.reservas.model.Reserva;

import lombok.RequiredArgsConstructor;

/**
 * Implementación con `MongoTemplate` de las consultas de reservas. Sin lógica de negocio.
 */
@RequiredArgsConstructor
public class ReservaRepositoryImpl implements ReservaRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public boolean existeSolape(UUID estudianteCodigo, LocalDate fecha, String horaInicio) {
        Query consulta = Query.query(Criteria.where("estudianteCodigo").is(estudianteCodigo)
            .and("fecha").is(fecha)
            .and("horaInicio").is(horaInicio)
            .and("estado").in(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA)
            .and("deletedAt").is(null));
        return mongoTemplate.exists(consulta, Reserva.class);
    }

    @Override
    public Page<Reserva> proximas(UUID estudianteCodigo, LocalDate hoy, String ahora, Pageable pageable) {
        Criteria criterio = new Criteria().andOperator(
            delEstudiante(estudianteCodigo),
            Criteria.where("estado").is(EstadoReserva.CONFIRMADA),
            noTermino(hoy, ahora));
        return pagina(criterio, pageable, Sort.Direction.ASC);
    }

    @Override
    public Page<Reserva> historial(UUID estudianteCodigo, LocalDate hoy, String ahora, Pageable pageable) {
        // * Una CANCELADA solo es del historial si alguna vez estuvo confirmada: la que no logró confirmarse (sin
        // * horas, Pagos caído) nunca fue una clase del estudiante, solo queda como registro del intento
        Criteria cerrada = new Criteria().orOperator(
            Criteria.where("estado").in(EstadoReserva.COMPLETADA, EstadoReserva.NO_ASISTIO),
            Criteria.where("estado").is(EstadoReserva.CANCELADA)
                .and("historialEstados.estado").is(EstadoReserva.CONFIRMADA));
        Criteria confirmadaYTerminada = new Criteria().andOperator(
            Criteria.where("estado").is(EstadoReserva.CONFIRMADA),
            new Criteria().norOperator(noTermino(hoy, ahora)));
        Criteria criterio = new Criteria().andOperator(
            delEstudiante(estudianteCodigo),
            new Criteria().orOperator(cerrada, confirmadaYTerminada));
        return pagina(criterio, pageable, Sort.Direction.DESC);
    }

    @Override
    public List<Reserva> pendientesDeEstudiante(UUID estudianteCodigo, Instant anteriorA) {
        Query consulta = Query.query(delEstudiante(estudianteCodigo)
            .and("estado").is(EstadoReserva.PENDIENTE_PAGO)
            .and("createdAt").lt(anteriorA));
        return mongoTemplate.find(consulta, Reserva.class);
    }

    @Override
    public Optional<Reserva> cambiarEstado(UUID codigoReserva, UUID estudianteCodigo, EstadoReserva esperado,
            EstadoReserva nuevo, String motivo, Instant ahora) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("estudianteCodigo").is(estudianteCodigo)
            .and("estado").is(esperado)
            .and("deletedAt").is(null));
        Update cambio = new Update().set("estado", nuevo).push("historialEstados",
            new HistorialEstado(nuevo, ahora, motivo));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    private static Criteria delEstudiante(UUID estudianteCodigo) {
        return Criteria.where("estudianteCodigo").is(estudianteCodigo).and("deletedAt").is(null);
    }

    /** La clase todavía no terminó: es de un día posterior a hoy, o de hoy con hora de fin posterior a ahora. */
    private static Criteria noTermino(LocalDate hoy, String ahora) {
        return new Criteria().orOperator(
            Criteria.where("fecha").gt(hoy),
            Criteria.where("fecha").is(hoy).and("horaFin").gt(ahora));
    }

    private Page<Reserva> pagina(Criteria criterio, Pageable pageable, Sort.Direction direccion) {
        Pageable ordenada = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(direccion, "fecha", "horaInicio"));
        Query consulta = Query.query(criterio);
        List<Reserva> contenido = mongoTemplate.find(Query.of(consulta).with(ordenada), Reserva.class);
        return PageableExecutionUtils.getPage(contenido, ordenada,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), Reserva.class));
    }
}
