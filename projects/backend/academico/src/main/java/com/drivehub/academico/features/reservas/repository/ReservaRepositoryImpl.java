package com.drivehub.academico.features.reservas.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public boolean existeSolape(UUID estudianteCodigo, LocalDate fecha, String horaInicio,
            UUID excluyendoReserva) {
        Criteria criterio = Criteria.where("estudianteCodigo").is(estudianteCodigo)
            .and("fecha").is(fecha)
            .and("horaInicio").is(horaInicio)
            .and("estado").in(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA,
                EstadoReserva.REPROGRAMACION_PENDIENTE)
            .and("deletedAt").is(null);
        if (excluyendoReserva != null) {
            criterio = criterio.and("codigoReserva").ne(excluyendoReserva);
        }
        return mongoTemplate.exists(Query.query(criterio), Reserva.class);
    }

    @Override
    public Page<Reserva> proximas(UUID estudianteCodigo, LocalDate hoy, String ahora, Pageable pageable) {
        Criteria criterio = new Criteria().andOperator(
            delEstudiante(estudianteCodigo),
            Criteria.where("estado").in(EstadoReserva.CONFIRMADA, EstadoReserva.REPROGRAMACION_PENDIENTE),
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
    public List<Reserva> pendientesAntiguas(Set<EstadoReserva> estados, Instant anteriorA, int limite) {
        Query consulta = Query.query(Criteria.where("estado").in(estados)
            .and("deletedAt").is(null)
            .and("requiereRevision").ne(true)
            .and("updatedAt").lt(anteriorA))
            .with(Sort.by(Sort.Direction.ASC, "updatedAt"))
            .limit(limite);
        return mongoTemplate.find(consulta, Reserva.class);
    }

    @Override
    public Optional<Reserva> registrarIntentoFallido(UUID codigoReserva, Set<EstadoReserva> estados) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("estado").in(estados)
            .and("deletedAt").is(null));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, new Update().inc("intentosRetomar", 1),
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Optional<Reserva> marcarParaRevision(UUID codigoReserva, String motivo, Instant ahora,
            Set<EstadoReserva> estados) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("estado").in(estados)
            .and("requiereRevision").ne(true)
            .and("deletedAt").is(null));
        // * El estado NO cambia: el historial repite el estado actual con el motivo de la revisión
        Reserva actual = mongoTemplate.findOne(consulta, Reserva.class);
        if (actual == null) {
            return Optional.empty();
        }
        Update cambio = new Update().set("requiereRevision", true)
            .push("historialEstados", new HistorialEstado(actual.getEstado(), ahora, motivo));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Optional<Reserva> cambiarEstado(UUID codigoReserva, UUID estudianteCodigo, EstadoReserva esperado,
            EstadoReserva nuevo, String motivo, Instant ahora) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("estudianteCodigo").is(estudianteCodigo)
            .and("estado").is(esperado)
            .and("deletedAt").is(null));
        Update cambio = new Update().set("estado", nuevo).set("updatedAt", ahora).push("historialEstados",
            new HistorialEstado(nuevo, ahora, motivo));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Optional<Reserva> iniciarReprogramacion(UUID codigoReserva, UUID estudianteCodigo, Long franjaDestinoId,
            String motivo, Instant ahora) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("estudianteCodigo").is(estudianteCodigo)
            .and("estado").is(EstadoReserva.CONFIRMADA)
            .and("deletedAt").is(null));
        Update cambio = new Update().set("estado", EstadoReserva.REPROGRAMACION_PENDIENTE)
            .set("franjaDestinoId", franjaDestinoId).set("updatedAt", ahora)
            .push("historialEstados", new HistorialEstado(EstadoReserva.REPROGRAMACION_PENDIENTE, ahora, motivo));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Optional<Reserva> completarReprogramacion(UUID codigoReserva, UUID estudianteCodigo, Long franjaId,
            LocalDate fecha, String horaInicio, String horaFin, UUID instructorCodigo, Instant ahora) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("estudianteCodigo").is(estudianteCodigo)
            .and("estado").is(EstadoReserva.REPROGRAMACION_PENDIENTE)
            .and("deletedAt").is(null));
        Update cambio = new Update().set("estado", EstadoReserva.CONFIRMADA)
            .set("franjaId", franjaId).set("fecha", fecha).set("horaInicio", horaInicio).set("horaFin", horaFin)
            .set("instructorCodigo", instructorCodigo).set("updatedAt", ahora).unset("franjaDestinoId")
            .push("historialEstados", new HistorialEstado(EstadoReserva.CONFIRMADA, ahora, "Reprogramada"));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Optional<Reserva> revertirReprogramacion(UUID codigoReserva, UUID estudianteCodigo, String motivo,
            Instant ahora) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("estudianteCodigo").is(estudianteCodigo)
            .and("estado").is(EstadoReserva.REPROGRAMACION_PENDIENTE)
            .and("deletedAt").is(null));
        Update cambio = new Update().set("estado", EstadoReserva.CONFIRMADA).set("updatedAt", ahora)
            .unset("franjaDestinoId")
            .push("historialEstados", new HistorialEstado(EstadoReserva.CONFIRMADA, ahora, motivo));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Optional<Reserva> iniciarCancelacion(UUID codigoReserva, UUID estudianteCodigo, boolean sinDevolucion,
            String motivo, Instant ahora) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("estudianteCodigo").is(estudianteCodigo)
            .and("estado").is(EstadoReserva.CONFIRMADA)
            .and("deletedAt").is(null));
        Update cambio = new Update().set("estado", EstadoReserva.CANCELACION_PENDIENTE)
            .set("sinDevolucion", sinDevolucion).set("updatedAt", ahora)
            .push("historialEstados", new HistorialEstado(EstadoReserva.CANCELACION_PENDIENTE, ahora, motivo));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Optional<Reserva> iniciarCancelacionPorInstructor(UUID codigoReserva, UUID instructorCodigo,
            String motivo, Instant ahora) {
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("instructorCodigo").is(instructorCodigo)
            .and("estado").is(EstadoReserva.CONFIRMADA)
            .and("deletedAt").is(null));
        Update cambio = new Update().set("estado", EstadoReserva.CANCELACION_PENDIENTE)
            .set("sinDevolucion", false).set("canceladaPorInstructor", true).set("updatedAt", ahora)
            .push("historialEstados", new HistorialEstado(EstadoReserva.CANCELACION_PENDIENTE, ahora, motivo));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Optional<Reserva> cerrar(UUID codigoReserva, UUID instructorCodigo, EstadoReserva nuevoEstado,
            String motivo, Instant ahora) {
        if (nuevoEstado != EstadoReserva.COMPLETADA && nuevoEstado != EstadoReserva.NO_ASISTIO) {
            throw new IllegalArgumentException("Una clase solo se cierra como COMPLETADA o NO_ASISTIO");
        }
        Query consulta = Query.query(Criteria.where("codigoReserva").is(codigoReserva)
            .and("instructorCodigo").is(instructorCodigo)
            .and("estado").is(EstadoReserva.CONFIRMADA)
            .and("deletedAt").is(null));
        Update cambio = new Update().set("estado", nuevoEstado).set("updatedAt", ahora)
            .push("historialEstados", new HistorialEstado(nuevoEstado, ahora, motivo));
        Reserva actualizada = mongoTemplate.findAndModify(consulta, cambio,
            FindAndModifyOptions.options().returnNew(true), Reserva.class);
        return Optional.ofNullable(actualizada);
    }

    @Override
    public Page<Reserva> agendaDeInstructor(UUID instructorCodigo, LocalDate fecha, Pageable pageable) {
        // * Una CANCELADA solo es una clase si alguna vez estuvo confirmada (la misma regla del historial del estudiante)
        Criteria estado = new Criteria().orOperator(
            Criteria.where("estado").in(EstadoReserva.CONFIRMADA, EstadoReserva.REPROGRAMACION_PENDIENTE,
                EstadoReserva.COMPLETADA, EstadoReserva.NO_ASISTIO, EstadoReserva.CANCELACION_PENDIENTE),
            Criteria.where("estado").is(EstadoReserva.CANCELADA)
                .and("historialEstados.estado").is(EstadoReserva.CONFIRMADA));
        Criteria criterio = new Criteria().andOperator(
            Criteria.where("instructorCodigo").is(instructorCodigo).and("fecha").is(fecha)
                .and("deletedAt").is(null),
            estado);
        // * Por hora de inicio y, para que la paginación sea estable, por código
        Pageable ordenada = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.ASC, "horaInicio", "codigoReserva"));
        Query consulta = Query.query(criterio);
        List<Reserva> contenido = mongoTemplate.find(Query.of(consulta).with(ordenada), Reserva.class);
        return PageableExecutionUtils.getPage(contenido, ordenada,
            () -> mongoTemplate.count(Query.of(consulta).limit(-1).skip(-1), Reserva.class));
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
