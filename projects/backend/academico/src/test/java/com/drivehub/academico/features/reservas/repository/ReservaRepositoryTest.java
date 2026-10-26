package com.drivehub.academico.features.reservas.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.common.mongo.AsignarIdentificadoresCallback;
import com.drivehub.academico.common.mongo.SecuenciaService;
import com.drivehub.academico.config.MongoConfig;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.HistorialEstado;
import com.drivehub.academico.features.reservas.model.Reserva;

@DataMongoTest
@Import({AsignarIdentificadoresCallback.class, SecuenciaService.class, MongoConfig.class})
class ReservaRepositoryTest extends MongoIntegrationTestBase {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);
    private static final Instant AHORA = Instant.parse("2026-10-26T13:00:00Z");

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final UUID estudiante = UUID.randomUUID();
    private final UUID otroEstudiante = UUID.randomUUID();
    private final UUID instructor = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        // * remove() y no dropCollection(): borrar la colección se llevaría los índices
        mongoTemplate.remove(new Query(), Reserva.class);
    }

    private Reserva guardar(UUID propietario, LocalDate fecha, String inicio, String fin, EstadoReserva estado) {
        Reserva reserva = new Reserva(propietario, instructor, 1L, fecha, inicio, fin, 1, AHORA);
        reserva.setEstado(estado);
        return repository.save(reserva);
    }

    /** Una reserva que se confirmó y después se canceló (CU15): su historial de estados lo dice. */
    private Reserva canceladaQueFueConfirmada(UUID propietario, LocalDate fecha, String inicio, String fin) {
        Reserva reserva = new Reserva(propietario, instructor, 1L, fecha, inicio, fin, 1, AHORA);
        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.setHistorialEstados(new java.util.ArrayList<>(java.util.List.of(
            new HistorialEstado(EstadoReserva.PENDIENTE_PAGO, AHORA, null),
            new HistorialEstado(EstadoReserva.CONFIRMADA, AHORA, "Hora descontada"),
            new HistorialEstado(EstadoReserva.CANCELADA, AHORA, "El estudiante canceló"))));
        return repository.save(reserva);
    }

    private Reserva confirmada(UUID propietario, LocalDate fecha, String inicio, String fin) {
        return guardar(propietario, fecha, inicio, fin, EstadoReserva.CONFIRMADA);
    }

    private static PageRequest pagina() {
        return PageRequest.of(0, 20);
    }

    // ---------------------------------------------------------------- la reserva nueva

    @Test
    void reservaNueva_nacePendienteDePagoConSuPrimerHistorial() {
        Reserva reserva = new Reserva(estudiante, instructor, 7L, HOY, "08:00", "09:00", 1, AHORA);
        repository.save(reserva);

        Reserva leida = repository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(
            reserva.getCodigoReserva(), estudiante).orElseThrow();

        assertThat(leida.getEstado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(leida.getHorasCobradas()).isEqualTo(1);
        assertThat(leida.getFranjaId()).isEqualTo(7L);
        assertThat(leida.getHistorialEstados()).hasSize(1);
        assertThat(leida.getHistorialEstados().get(0).estado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
    }

    @Test
    void buscarPorCodigoYEstudiante_soloDevuelveLaDeEseEstudiante_yNoLasBorradas() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");

        assertThat(repository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(reserva.getCodigoReserva(),
            estudiante)).isPresent();
        assertThat(repository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(reserva.getCodigoReserva(),
            otroEstudiante)).isEmpty();

        reserva.setDeletedAt(AHORA);
        repository.save(reserva);
        assertThat(repository.findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull(reserva.getCodigoReserva(),
            estudiante)).isEmpty();
    }

    // ---------------------------------------------------------------- solape

    @Test
    void existeSolape_cuentaPendientesYConfirmadas_peroNoLasCanceladas() {
        guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        confirmada(estudiante, HOY, "09:00", "10:00");
        guardar(estudiante, HOY, "10:00", "11:00", EstadoReserva.CANCELADA);

        assertThat(repository.existeSolape(estudiante, HOY, "08:00")).isTrue();
        assertThat(repository.existeSolape(estudiante, HOY, "09:00")).isTrue();
        assertThat(repository.existeSolape(estudiante, HOY, "10:00")).isFalse();
        assertThat(repository.existeSolape(estudiante, HOY, "11:00")).isFalse();
        assertThat(repository.existeSolape(estudiante, HOY.plusDays(1), "08:00")).isFalse();
        assertThat(repository.existeSolape(otroEstudiante, HOY, "08:00")).isFalse();
    }

    // ---------------------------------------------------------------- próximas

    @Test
    void proximas_soloConfirmadasQueNoTerminaron_deLaMasCercanaALaMasLejana() {
        confirmada(estudiante, HOY.plusDays(2), "08:00", "09:00");
        confirmada(estudiante, HOY, "15:00", "16:00");
        confirmada(estudiante, HOY, "10:00", "11:00");
        confirmada(estudiante, HOY, "08:00", "09:00");
        confirmada(otroEstudiante, HOY, "12:00", "13:00");
        guardar(estudiante, HOY, "13:00", "14:00", EstadoReserva.PENDIENTE_PAGO);
        guardar(estudiante, HOY, "14:00", "15:00", EstadoReserva.CANCELADA);

        var resultado = repository.proximas(estudiante, HOY, "09:30", pagina());

        assertThat(resultado.getContent()).extracting(Reserva::getFecha, Reserva::getHoraInicio)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(HOY, "10:00"),
                org.assertj.core.groups.Tuple.tuple(HOY, "15:00"),
                org.assertj.core.groups.Tuple.tuple(HOY.plusDays(2), "08:00"));
        assertThat(resultado.getTotalElements()).isEqualTo(3);
    }

    @Test
    void proximas_unaClaseEnCursoSigueSiendoProxima_hastaQueTermina() {
        confirmada(estudiante, HOY, "09:00", "10:00");

        assertThat(repository.proximas(estudiante, HOY, "09:30", pagina()).getContent()).hasSize(1);
        assertThat(repository.proximas(estudiante, HOY, "09:59", pagina()).getContent()).hasSize(1);
        assertThat(repository.proximas(estudiante, HOY, "10:00", pagina()).getContent()).isEmpty();
    }

    // ---------------------------------------------------------------- historial

    @Test
    void historial_cerradasYConfirmadasTerminadas_delMasRecienteAlMasAntiguo() {
        guardar(estudiante, HOY.minusDays(3), "08:00", "09:00", EstadoReserva.COMPLETADA);
        guardar(estudiante, HOY.minusDays(1), "08:00", "09:00", EstadoReserva.NO_ASISTIO);
        canceladaQueFueConfirmada(estudiante, HOY.plusDays(1), "08:00", "09:00");
        confirmada(estudiante, HOY, "08:00", "09:00");
        // * no deben aparecer: una próxima, una pendiente, una de otro estudiante y una cancelada que nunca llegó
        // * a confirmarse (sin horas o Pagos caído: nunca fue una clase del estudiante)
        confirmada(estudiante, HOY, "15:00", "16:00");
        guardar(estudiante, HOY.minusDays(4), "08:00", "09:00", EstadoReserva.CANCELADA);
        guardar(estudiante, HOY.minusDays(2), "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        guardar(otroEstudiante, HOY.minusDays(1), "08:00", "09:00", EstadoReserva.COMPLETADA);

        var resultado = repository.historial(estudiante, HOY, "10:00", pagina());

        assertThat(resultado.getContent()).extracting(Reserva::getFecha, Reserva::getEstado)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(HOY.plusDays(1), EstadoReserva.CANCELADA),
                org.assertj.core.groups.Tuple.tuple(HOY, EstadoReserva.CONFIRMADA),
                org.assertj.core.groups.Tuple.tuple(HOY.minusDays(1), EstadoReserva.NO_ASISTIO),
                org.assertj.core.groups.Tuple.tuple(HOY.minusDays(3), EstadoReserva.COMPLETADA));
        assertThat(resultado.getTotalElements()).isEqualTo(4);
    }

    @Test
    void proximasEHistorial_sonComplementarias_paraLasConfirmadas() {
        confirmada(estudiante, HOY, "09:00", "10:00");

        assertThat(repository.proximas(estudiante, HOY, "09:30", pagina()).getTotalElements()).isEqualTo(1);
        assertThat(repository.historial(estudiante, HOY, "09:30", pagina()).getTotalElements()).isZero();

        assertThat(repository.proximas(estudiante, HOY, "10:00", pagina()).getTotalElements()).isZero();
        assertThat(repository.historial(estudiante, HOY, "10:00", pagina()).getTotalElements()).isEqualTo(1);
    }

    @Test
    void listados_noIncluyenLasReservasBorradas() {
        Reserva borrada = confirmada(estudiante, HOY.plusDays(1), "08:00", "09:00");
        borrada.setDeletedAt(AHORA);
        repository.save(borrada);

        assertThat(repository.proximas(estudiante, HOY, "09:30", pagina()).getContent()).isEmpty();
    }

    // ---------------------------------------------------------------- pendientes

    @Test
    void pendientesDeEstudiante_soloLasPendientesAntiguasDeEseEstudiante() throws InterruptedException {
        guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        guardar(estudiante, HOY, "09:00", "10:00", EstadoReserva.CONFIRMADA);
        guardar(otroEstudiante, HOY, "10:00", "11:00", EstadoReserva.PENDIENTE_PAGO);
        Thread.sleep(50);

        assertThat(repository.pendientesDeEstudiante(estudiante, Instant.now())).hasSize(1)
            .allSatisfy(r -> assertThat(r.getHoraInicio()).isEqualTo("08:00"));
        assertThat(repository.pendientesDeEstudiante(estudiante, Instant.now().minusSeconds(3600))).isEmpty();
    }

    // ---------------------------------------------------------------- cambiar estado

    @Test
    void cambiarEstado_cambiaSoloSiElEstadoCoincide_yAgregaAlHistorial() {
        Reserva reserva = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);

        var cambiada = repository.cambiarEstado(reserva.getCodigoReserva(), estudiante,
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA, "Hora descontada", AHORA);

        assertThat(cambiada).isPresent();
        assertThat(cambiada.get().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(cambiada.get().getHistorialEstados()).hasSize(2);
        assertThat(cambiada.get().getHistorialEstados().get(1).motivo()).isEqualTo("Hora descontada");

        // * ya no está pendiente: un segundo intento no cambia nada
        assertThat(repository.cambiarEstado(reserva.getCodigoReserva(), estudiante, EstadoReserva.PENDIENTE_PAGO,
            EstadoReserva.CANCELADA, "otra", AHORA)).isEmpty();
    }

    @Test
    void cambiarEstado_noTocaLaReservaDeOtroEstudiante() {
        Reserva reserva = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);

        assertThat(repository.cambiarEstado(reserva.getCodigoReserva(), otroEstudiante, EstadoReserva.PENDIENTE_PAGO,
            EstadoReserva.CONFIRMADA, null, AHORA)).isEmpty();
        assertThat(repository.findById(reserva.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoReserva.PENDIENTE_PAGO);
    }
}
