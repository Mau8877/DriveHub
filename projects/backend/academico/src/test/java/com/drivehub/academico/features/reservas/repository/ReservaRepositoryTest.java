package com.drivehub.academico.features.reservas.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
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

    // ---------------------------------------------------------------- orquestador (CU44)

    private static final Set<EstadoReserva> PENDIENTES = Set.of(EstadoReserva.PENDIENTE_PAGO);

    private static Instant enElFuturo() {
        return Instant.now().plusSeconds(3600);
    }

    @Test
    void reservaNueva_naceSinIntentosNiRevision() {
        Reserva reserva = repository.save(new Reserva(estudiante, instructor, 7L, HOY, "08:00", "09:00", 1, AHORA));

        Reserva leida = repository.findById(reserva.getId()).orElseThrow();

        assertThat(leida.getIntentosRetomar()).isZero();
        assertThat(leida.isRequiereRevision()).isFalse();
    }

    @Test
    void reservaAntiguaSinLosCampos_seLeeConLosValoresPorDefecto() {
        Reserva reserva = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        // * Simula un documento anterior al CU44: se quitan los campos de la colección
        mongoTemplate.updateFirst(Query.query(org.springframework.data.mongodb.core.query.Criteria.where("_id")
            .is(reserva.getId())), new org.springframework.data.mongodb.core.query.Update()
            .unset("intentosRetomar").unset("requiereRevision"), Reserva.class);

        Reserva leida = repository.findById(reserva.getId()).orElseThrow();

        assertThat(leida.getIntentosRetomar()).isZero();
        assertThat(leida.isRequiereRevision()).isFalse();
    }

    @Test
    void pendientesAntiguas_traeLasDeTodosLosEstudiantes_soloAntiguas_yOrdenadas() {
        Reserva deUno = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        Reserva deOtro = guardar(otroEstudiante, HOY, "09:00", "10:00", EstadoReserva.PENDIENTE_PAGO);

        assertThat(repository.pendientesAntiguas(PENDIENTES, enElFuturo(), 10))
            .extracting(Reserva::getCodigoReserva)
            .containsExactly(deUno.getCodigoReserva(), deOtro.getCodigoReserva());
        // * ninguna es anterior a hace una hora: son recientes
        assertThat(repository.pendientesAntiguas(PENDIENTES, Instant.now().minusSeconds(3600), 10)).isEmpty();
    }

    @Test
    void pendientesAntiguas_noTraeLasQueNoEstanEnLosEstadosPedidos() {
        guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);
        guardar(estudiante, HOY, "09:00", "10:00", EstadoReserva.CANCELADA);
        guardar(estudiante, HOY, "10:00", "11:00", EstadoReserva.NO_ASISTIO);
        Reserva pendiente = guardar(estudiante, HOY, "11:00", "12:00", EstadoReserva.PENDIENTE_PAGO);

        assertThat(repository.pendientesAntiguas(PENDIENTES, enElFuturo(), 10))
            .extracting(Reserva::getCodigoReserva).containsExactly(pendiente.getCodigoReserva());
        // * con otro estado transitorio declarado por un manejador, también entra
        assertThat(repository.pendientesAntiguas(Set.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA),
            enElFuturo(), 10)).hasSize(2);
    }

    @Test
    void pendientesAntiguas_noTraeLasBorradasNiLasMarcadasParaRevision_yRespetaElLimite() {
        Reserva borrada = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        borrada.setDeletedAt(AHORA);
        repository.save(borrada);
        Reserva enRevision = guardar(estudiante, HOY, "09:00", "10:00", EstadoReserva.PENDIENTE_PAGO);
        repository.marcarParaRevision(enRevision.getCodigoReserva(), "revisar", AHORA, PENDIENTES);
        guardar(estudiante, HOY, "10:00", "11:00", EstadoReserva.PENDIENTE_PAGO);
        guardar(estudiante, HOY, "11:00", "12:00", EstadoReserva.PENDIENTE_PAGO);

        assertThat(repository.pendientesAntiguas(PENDIENTES, enElFuturo(), 10)).hasSize(2);
        assertThat(repository.pendientesAntiguas(PENDIENTES, enElFuturo(), 1)).hasSize(1);
    }

    @Test
    void registrarIntentoFallido_sumaUnoCadaVez_yDevuelveLaReservaActualizada() {
        Reserva reserva = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);

        assertThat(repository.registrarIntentoFallido(reserva.getCodigoReserva(), PENDIENTES).orElseThrow()
            .getIntentosRetomar()).isEqualTo(1);
        assertThat(repository.registrarIntentoFallido(reserva.getCodigoReserva(), PENDIENTES).orElseThrow()
            .getIntentosRetomar()).isEqualTo(2);
    }

    @Test
    void registrarIntentoFallido_sinReservaPendiente_noHaceNada() {
        Reserva confirmada = confirmada(estudiante, HOY, "08:00", "09:00");

        assertThat(repository.registrarIntentoFallido(confirmada.getCodigoReserva(), PENDIENTES)).isEmpty();
        assertThat(repository.registrarIntentoFallido(UUID.randomUUID(), PENDIENTES)).isEmpty();
        assertThat(repository.findById(confirmada.getId()).orElseThrow().getIntentosRetomar()).isZero();
    }

    @Test
    void registrarIntentoFallido_dosAlMismoTiempo_dejanElContadorEnDos() throws Exception {
        Reserva reserva = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> repository.registrarIntentoFallido(reserva.getCodigoReserva(), PENDIENTES));
            var b = pool.submit(() -> repository.registrarIntentoFallido(reserva.getCodigoReserva(), PENDIENTES));
            a.get();
            b.get();
        } finally {
            pool.shutdown();
        }

        assertThat(repository.findById(reserva.getId()).orElseThrow().getIntentosRetomar()).isEqualTo(2);
    }

    @Test
    void marcarParaRevision_dejaElEstadoIgual_agregaAlHistorial_yRepetirloEsInocuo() {
        Reserva reserva = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);

        var marcada = repository.marcarParaRevision(reserva.getCodigoReserva(), "Se agotaron los intentos", AHORA,
            PENDIENTES);

        assertThat(marcada).isPresent();
        assertThat(marcada.get().isRequiereRevision()).isTrue();
        assertThat(marcada.get().getEstado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(marcada.get().getHistorialEstados()).hasSize(2);
        assertThat(marcada.get().getHistorialEstados().get(1).motivo()).isEqualTo("Se agotaron los intentos");

        assertThat(repository.marcarParaRevision(reserva.getCodigoReserva(), "otra vez", AHORA, PENDIENTES)).isEmpty();
        assertThat(repository.findById(reserva.getId()).orElseThrow().getHistorialEstados()).hasSize(2);
    }

    @Test
    void marcarParaRevision_siYaNoEstaPendiente_noHaceNada() {
        Reserva confirmada = confirmada(estudiante, HOY, "08:00", "09:00");

        assertThat(repository.marcarParaRevision(confirmada.getCodigoReserva(), "x", AHORA, PENDIENTES)).isEmpty();
        assertThat(repository.findById(confirmada.getId()).orElseThrow().isRequiereRevision()).isFalse();
    }

    // ---------------------------------------------------------------- cancelar (CU15)

    private static final Set<EstadoReserva> TRANSITORIOS = Set.of(EstadoReserva.PENDIENTE_PAGO,
        EstadoReserva.CANCELACION_PENDIENTE);

    @Test
    void iniciarCancelacion_pasaDeConfirmadaACancelacionPendiente_fijaSinDevolucionYAgregaAlHistorial() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");
        Instant despues = AHORA.plusSeconds(60);

        var iniciada = repository.iniciarCancelacion(reserva.getCodigoReserva(), estudiante, true,
            "Cancelada por el estudiante", despues);

        assertThat(iniciada).isPresent();
        assertThat(iniciada.get().getEstado()).isEqualTo(EstadoReserva.CANCELACION_PENDIENTE);
        assertThat(iniciada.get().isSinDevolucion()).isTrue();
        assertThat(iniciada.get().getUpdatedAt()).isEqualTo(despues);
        assertThat(iniciada.get().getHistorialEstados()).hasSize(2);
        assertThat(iniciada.get().getHistorialEstados().get(1).estado()).isEqualTo(EstadoReserva.CANCELACION_PENDIENTE);
        assertThat(iniciada.get().getHistorialEstados().get(1).motivo()).isEqualTo("Cancelada por el estudiante");
    }

    @Test
    void iniciarCancelacion_aTiempo_dejaSinDevolucionEnFalso() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");

        var iniciada = repository.iniciarCancelacion(reserva.getCodigoReserva(), estudiante, false, "x", AHORA);

        assertThat(iniciada).isPresent();
        assertThat(iniciada.get().isSinDevolucion()).isFalse();
    }

    @Test
    void iniciarCancelacion_siNoEstaConfirmada_oEsDeOtro_oEstaBorrada_devuelveVacioYNoCambiaNada() {
        Reserva pendiente = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        Reserva cancelada = guardar(estudiante, HOY, "09:00", "10:00", EstadoReserva.CANCELADA);
        Reserva ajena = confirmada(otroEstudiante, HOY, "10:00", "11:00");
        Reserva borrada = confirmada(estudiante, HOY, "11:00", "12:00");
        borrada.setDeletedAt(AHORA);
        repository.save(borrada);

        assertThat(repository.iniciarCancelacion(pendiente.getCodigoReserva(), estudiante, false, "x", AHORA)).isEmpty();
        assertThat(repository.iniciarCancelacion(cancelada.getCodigoReserva(), estudiante, false, "x", AHORA)).isEmpty();
        assertThat(repository.iniciarCancelacion(ajena.getCodigoReserva(), estudiante, false, "x", AHORA)).isEmpty();
        assertThat(repository.iniciarCancelacion(borrada.getCodigoReserva(), estudiante, false, "x", AHORA)).isEmpty();
        assertThat(repository.findById(ajena.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void iniciarCancelacion_dosAlMismoTiempo_soloUnaGana() throws Exception {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> repository.iniciarCancelacion(reserva.getCodigoReserva(), estudiante, false, "a",
                AHORA));
            var b = pool.submit(() -> repository.iniciarCancelacion(reserva.getCodigoReserva(), estudiante, true, "b",
                AHORA));

            assertThat(java.util.stream.Stream.of(a.get(), b.get()).filter(java.util.Optional::isPresent).count())
                .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- cancelar por el instructor y agenda (CU16)

    private final UUID otroInstructor = UUID.randomUUID();

    private Reserva delInstructor(UUID elInstructor, LocalDate fecha, String inicio, String fin,
            EstadoReserva estado) {
        Reserva reserva = new Reserva(estudiante, elInstructor, 1L, fecha, inicio, fin, 1, AHORA);
        reserva.setEstado(estado);
        reserva.setHistorialEstados(new java.util.ArrayList<>(java.util.List.of(
            new HistorialEstado(EstadoReserva.PENDIENTE_PAGO, AHORA, null),
            new HistorialEstado(EstadoReserva.CONFIRMADA, AHORA, "Hora descontada"))));
        return repository.save(reserva);
    }

    @Test
    void iniciarCancelacionPorInstructor_marcaLosCamposYAgregaAlHistorial() {
        Reserva reserva = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);
        Instant despues = AHORA.plusSeconds(60);

        var iniciada = repository.iniciarCancelacionPorInstructor(reserva.getCodigoReserva(), instructor,
            "Cancelada por el instructor", despues);

        assertThat(iniciada).isPresent();
        assertThat(iniciada.get().getEstado()).isEqualTo(EstadoReserva.CANCELACION_PENDIENTE);
        assertThat(iniciada.get().isSinDevolucion()).isFalse();
        assertThat(iniciada.get().isCanceladaPorInstructor()).isTrue();
        assertThat(iniciada.get().getUpdatedAt()).isEqualTo(despues);
        assertThat(iniciada.get().getHistorialEstados()).hasSize(3);
        assertThat(iniciada.get().getHistorialEstados().get(2).estado()).isEqualTo(EstadoReserva.CANCELACION_PENDIENTE);
        assertThat(iniciada.get().getHistorialEstados().get(2).motivo()).isEqualTo("Cancelada por el instructor");
    }

    @Test
    void iniciarCancelacionPorInstructor_siNoEstaConfirmadaEsDeOtroOEstaBorrada_devuelveVacioYNoCambiaNada() {
        Reserva pendiente = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        Reserva cancelada = delInstructor(instructor, HOY, "09:00", "10:00", EstadoReserva.CANCELADA);
        Reserva completada = delInstructor(instructor, HOY, "10:00", "11:00", EstadoReserva.COMPLETADA);
        Reserva aMedias = delInstructor(instructor, HOY, "11:00", "12:00", EstadoReserva.REPROGRAMACION_PENDIENTE);
        Reserva ajena = delInstructor(otroInstructor, HOY, "12:00", "13:00", EstadoReserva.CONFIRMADA);
        Reserva borrada = delInstructor(instructor, HOY, "13:00", "14:00", EstadoReserva.CONFIRMADA);
        borrada.setDeletedAt(AHORA);
        repository.save(borrada);

        for (Reserva reserva : java.util.List.of(pendiente, cancelada, completada, aMedias, ajena, borrada)) {
            assertThat(repository.iniciarCancelacionPorInstructor(reserva.getCodigoReserva(), instructor, "x", AHORA))
                .isEmpty();
        }
        Reserva sinCambios = repository.findById(ajena.getId()).orElseThrow();
        assertThat(sinCambios.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(sinCambios.isCanceladaPorInstructor()).isFalse();
    }

    @Test
    void iniciarCancelacionPorInstructor_repetidaOSimultaneaConLaDelEstudiante_soloUnaGana() {
        Reserva reserva = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);

        assertThat(repository.iniciarCancelacionPorInstructor(reserva.getCodigoReserva(), instructor, "x", AHORA))
            .isPresent();
        assertThat(repository.iniciarCancelacionPorInstructor(reserva.getCodigoReserva(), instructor, "x", AHORA))
            .isEmpty();
        // * ya la tomó el instructor: el estudiante no puede iniciar la suya
        assertThat(repository.iniciarCancelacion(reserva.getCodigoReserva(), estudiante, false, "y", AHORA)).isEmpty();
    }

    // ---------------------------------------------------------------- cerrar la clase (CU17)

    @Test
    void cerrar_confirmadaPasaACompletadaOANoAsistioConSuHistorial() {
        Reserva presente = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);
        Reserva ausente = delInstructor(instructor, HOY, "09:00", "10:00", EstadoReserva.CONFIRMADA);
        Instant despues = AHORA.plusSeconds(60);

        var completada = repository.cerrar(presente.getCodigoReserva(), instructor, EstadoReserva.COMPLETADA,
            "Clase cerrada por el instructor (PRESENTE)", despues);
        var noAsistio = repository.cerrar(ausente.getCodigoReserva(), instructor, EstadoReserva.NO_ASISTIO,
            "Clase cerrada por el instructor (AUSENTE)", despues);

        assertThat(completada).isPresent();
        assertThat(completada.get().getEstado()).isEqualTo(EstadoReserva.COMPLETADA);
        assertThat(completada.get().getUpdatedAt()).isEqualTo(despues);
        assertThat(completada.get().getHistorialEstados()).hasSize(3);
        assertThat(completada.get().getHistorialEstados().get(2).estado()).isEqualTo(EstadoReserva.COMPLETADA);
        assertThat(completada.get().getHistorialEstados().get(2).motivo())
            .isEqualTo("Clase cerrada por el instructor (PRESENTE)");
        assertThat(noAsistio).isPresent();
        assertThat(noAsistio.get().getEstado()).isEqualTo(EstadoReserva.NO_ASISTIO);
    }

    @Test
    void cerrar_noTocaLasHorasLaFranjaNiLosCamposDeLasCancelaciones() {
        Reserva reserva = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);

        var cerrada = repository.cerrar(reserva.getCodigoReserva(), instructor, EstadoReserva.COMPLETADA, "x", AHORA)
            .orElseThrow();

        assertThat(cerrada.getHorasCobradas()).isEqualTo(reserva.getHorasCobradas());
        assertThat(cerrada.getFranjaId()).isEqualTo(reserva.getFranjaId());
        assertThat(cerrada.isSinDevolucion()).isFalse();
        assertThat(cerrada.isCanceladaPorInstructor()).isFalse();
    }

    @Test
    void cerrar_siNoEstaConfirmadaEsDeOtroOEstaBorrada_devuelveVacioYNoCambiaNada() {
        Reserva pendiente = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        Reserva cancelada = delInstructor(instructor, HOY, "09:00", "10:00", EstadoReserva.CANCELADA);
        Reserva completada = delInstructor(instructor, HOY, "10:00", "11:00", EstadoReserva.COMPLETADA);
        Reserva noAsistio = delInstructor(instructor, HOY, "11:00", "12:00", EstadoReserva.NO_ASISTIO);
        Reserva cancelando = delInstructor(instructor, HOY, "12:00", "13:00", EstadoReserva.CANCELACION_PENDIENTE);
        Reserva aMedias = delInstructor(instructor, HOY, "13:00", "14:00", EstadoReserva.REPROGRAMACION_PENDIENTE);
        Reserva ajena = delInstructor(otroInstructor, HOY, "14:00", "15:00", EstadoReserva.CONFIRMADA);
        Reserva borrada = delInstructor(instructor, HOY, "15:00", "16:00", EstadoReserva.CONFIRMADA);
        borrada.setDeletedAt(AHORA);
        repository.save(borrada);

        for (Reserva reserva : java.util.List.of(pendiente, cancelada, completada, noAsistio, cancelando, aMedias,
            ajena, borrada)) {
            assertThat(repository.cerrar(reserva.getCodigoReserva(), instructor, EstadoReserva.COMPLETADA, "x", AHORA))
                .isEmpty();
        }
        Reserva sinCambios = repository.findById(ajena.getId()).orElseThrow();
        assertThat(sinCambios.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(sinCambios.getHistorialEstados()).hasSize(2);
    }

    @Test
    void cerrar_repetidoOSimultaneoConLaCancelacion_soloUnoGana() {
        Reserva reserva = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);

        assertThat(repository.cerrar(reserva.getCodigoReserva(), instructor, EstadoReserva.COMPLETADA, "x", AHORA))
            .isPresent();
        assertThat(repository.cerrar(reserva.getCodigoReserva(), instructor, EstadoReserva.NO_ASISTIO, "y", AHORA))
            .isEmpty();
        assertThat(repository.iniciarCancelacionPorInstructor(reserva.getCodigoReserva(), instructor, "z", AHORA))
            .isEmpty();
        assertThat(repository.findById(reserva.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoReserva.COMPLETADA);
    }

    @Test
    void cerrar_conUnEstadoQueNoEsDeCierre_lanzaIllegalArgumentException() {
        Reserva reserva = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);

        for (EstadoReserva estado : java.util.List.of(EstadoReserva.CONFIRMADA, EstadoReserva.CANCELADA,
            EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CANCELACION_PENDIENTE)) {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> repository.cerrar(reserva.getCodigoReserva(), instructor, estado, "x", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(repository.findById(reserva.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void reservaSinElCampoCanceladaPorInstructor_seLeeComoFalso() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");

        assertThat(repository.findById(reserva.getId()).orElseThrow().isCanceladaPorInstructor()).isFalse();
    }

    @Test
    void agendaDeInstructor_devuelveLasDelInstructorYLaFechaOrdenadasPorHora() {
        Reserva tarde = delInstructor(instructor, HOY, "15:00", "16:00", EstadoReserva.CONFIRMADA);
        Reserva temprano = delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);
        delInstructor(instructor, HOY.plusDays(1), "08:00", "09:00", EstadoReserva.CONFIRMADA);
        delInstructor(otroInstructor, HOY, "09:00", "10:00", EstadoReserva.CONFIRMADA);

        var agenda = repository.agendaDeInstructor(instructor, HOY, pagina());

        assertThat(agenda.getContent()).extracting(Reserva::getCodigoReserva)
            .containsExactly(temprano.getCodigoReserva(), tarde.getCodigoReserva());
        assertThat(agenda.getTotalElements()).isEqualTo(2);
    }

    @Test
    void agendaDeInstructor_incluyeLasQueSonOFueronUnaClase() {
        delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.CONFIRMADA);
        delInstructor(instructor, HOY, "09:00", "10:00", EstadoReserva.REPROGRAMACION_PENDIENTE);
        delInstructor(instructor, HOY, "10:00", "11:00", EstadoReserva.COMPLETADA);
        delInstructor(instructor, HOY, "11:00", "12:00", EstadoReserva.NO_ASISTIO);
        delInstructor(instructor, HOY, "12:00", "13:00", EstadoReserva.CANCELACION_PENDIENTE);
        delInstructor(instructor, HOY, "13:00", "14:00", EstadoReserva.CANCELADA);

        assertThat(repository.agendaDeInstructor(instructor, HOY, pagina()).getTotalElements()).isEqualTo(6);
    }

    @Test
    void agendaDeInstructor_excluyeLasPendientesDePagoLasCanceladasQueNuncaFueronClaseYLasBorradas() {
        delInstructor(instructor, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        // * cancelada por el sistema antes de confirmarse: nunca fue una clase
        Reserva nuncaConfirmada = new Reserva(estudiante, instructor, 1L, HOY, "09:00", "10:00", 1, AHORA);
        nuncaConfirmada.setEstado(EstadoReserva.CANCELADA);
        repository.save(nuncaConfirmada);
        Reserva borrada = delInstructor(instructor, HOY, "10:00", "11:00", EstadoReserva.CONFIRMADA);
        borrada.setDeletedAt(AHORA);
        repository.save(borrada);

        assertThat(repository.agendaDeInstructor(instructor, HOY, pagina()).getContent()).isEmpty();
    }

    @Test
    void agendaDeInstructor_paginaConElTotalReal_yUnDiaSinClasesDevuelveVacio() {
        for (int hora = 8; hora < 11; hora++) {
            delInstructor(instructor, HOY, String.format("%02d:00", hora), String.format("%02d:00", hora + 1),
                EstadoReserva.CONFIRMADA);
        }

        var primera = repository.agendaDeInstructor(instructor, HOY, PageRequest.of(0, 2));
        var segunda = repository.agendaDeInstructor(instructor, HOY, PageRequest.of(1, 2));

        assertThat(primera.getContent()).hasSize(2);
        assertThat(segunda.getContent()).hasSize(1);
        assertThat(primera.getTotalElements()).isEqualTo(3);
        assertThat(repository.agendaDeInstructor(instructor, HOY.plusDays(5), pagina()).getContent()).isEmpty();
    }

    @Test
    void cambiarEstado_fijaUpdatedAt() {
        Reserva reserva = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.PENDIENTE_PAGO);
        Instant despues = AHORA.plusSeconds(120);

        var cambiada = repository.cambiarEstado(reserva.getCodigoReserva(), estudiante, EstadoReserva.PENDIENTE_PAGO,
            EstadoReserva.CONFIRMADA, "x", despues);

        assertThat(cambiada.orElseThrow().getUpdatedAt()).isEqualTo(despues);
    }

    @Test
    void reservaAntiguaSinLosCamposDeCancelacion_seLeeConLosValoresPorDefecto() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");

        Reserva leida = repository.findById(reserva.getId()).orElseThrow();

        assertThat(leida.isSinDevolucion()).isFalse();
        assertThat(leida.getFranjaDestinoId()).isNull();
    }

    @Test
    void pendientesAntiguas_midePorUpdatedAt_noPorCreatedAt() {
        // * una clase confirmada hace días que se acaba de cancelar: createdAt viejo, updatedAt reciente
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");
        mongoTemplate.updateFirst(Query.query(org.springframework.data.mongodb.core.query.Criteria.where("_id")
                .is(reserva.getId())),
            new org.springframework.data.mongodb.core.query.Update()
                .set("createdAt", Instant.now().minusSeconds(7 * 24 * 3600L)), Reserva.class);
        repository.iniciarCancelacion(reserva.getCodigoReserva(), estudiante, false, "x", Instant.now());

        assertThat(repository.pendientesAntiguas(TRANSITORIOS, Instant.now().minusSeconds(120), 10)).isEmpty();
        assertThat(repository.pendientesAntiguas(TRANSITORIOS, Instant.now().plusSeconds(60), 10))
            .extracting(Reserva::getCodigoReserva).containsExactly(reserva.getCodigoReserva());
    }

    // ---------------------------------------------------------------- reprogramar (CU15)

    @Test
    void iniciarReprogramacion_pasaDeConfirmadaAReprogramacionPendiente_yGuardaLaFranjaDestino() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");

        var iniciada = repository.iniciarReprogramacion(reserva.getCodigoReserva(), estudiante, 99L,
            "Reprogramación pedida", AHORA.plusSeconds(5));

        assertThat(iniciada).isPresent();
        assertThat(iniciada.get().getEstado()).isEqualTo(EstadoReserva.REPROGRAMACION_PENDIENTE);
        assertThat(iniciada.get().getFranjaDestinoId()).isEqualTo(99L);
        assertThat(iniciada.get().getFranjaId()).isEqualTo(1L);
        assertThat(iniciada.get().getHistorialEstados()).hasSize(2);
        // * una segunda reprogramación a la vez no pisa la primera
        assertThat(repository.iniciarReprogramacion(reserva.getCodigoReserva(), estudiante, 100L, "otra", AHORA))
            .isEmpty();
        assertThat(repository.findById(reserva.getId()).orElseThrow().getFranjaDestinoId()).isEqualTo(99L);
    }

    @Test
    void iniciarReprogramacion_siNoEstaConfirmadaOEsDeOtro_devuelveVacio() {
        Reserva cancelada = guardar(estudiante, HOY, "08:00", "09:00", EstadoReserva.CANCELADA);
        Reserva ajena = confirmada(otroEstudiante, HOY, "09:00", "10:00");

        assertThat(repository.iniciarReprogramacion(cancelada.getCodigoReserva(), estudiante, 9L, "x", AHORA)).isEmpty();
        assertThat(repository.iniciarReprogramacion(ajena.getCodigoReserva(), estudiante, 9L, "x", AHORA)).isEmpty();
    }

    @Test
    void completarReprogramacion_copiaLaFranjaNueva_vuelveAConfirmada_yBorraElDestino() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");
        repository.iniciarReprogramacion(reserva.getCodigoReserva(), estudiante, 99L, "x", AHORA);
        UUID otroInstructor = UUID.randomUUID();
        LocalDate manana = HOY.plusDays(1);

        var completada = repository.completarReprogramacion(reserva.getCodigoReserva(), estudiante, 99L, manana,
            "15:00", "16:00", otroInstructor, AHORA.plusSeconds(10));

        assertThat(completada).isPresent();
        Reserva leida = repository.findById(reserva.getId()).orElseThrow();
        assertThat(leida.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(leida.getFranjaId()).isEqualTo(99L);
        assertThat(leida.getFecha()).isEqualTo(manana);
        assertThat(leida.getHoraInicio()).isEqualTo("15:00");
        assertThat(leida.getHoraFin()).isEqualTo("16:00");
        assertThat(leida.getInstructorCodigo()).isEqualTo(otroInstructor);
        assertThat(leida.getFranjaDestinoId()).isNull();
        assertThat(leida.getHistorialEstados().get(leida.getHistorialEstados().size() - 1).motivo())
            .isEqualTo("Reprogramada");
        // * repetirlo es inocuo: ya no está a medias
        assertThat(repository.completarReprogramacion(reserva.getCodigoReserva(), estudiante, 99L, manana, "15:00",
            "16:00", otroInstructor, AHORA)).isEmpty();
    }

    @Test
    void completarReprogramacion_noTocaUnaReservaQueNoEstaAMedias() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");

        assertThat(repository.completarReprogramacion(reserva.getCodigoReserva(), estudiante, 99L, HOY, "15:00",
            "16:00", instructor, AHORA)).isEmpty();
        assertThat(repository.findById(reserva.getId()).orElseThrow().getFranjaId()).isEqualTo(1L);
    }

    @Test
    void revertirReprogramacion_vuelveAConfirmadaEnLaFranjaOriginal_yBorraElDestino() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");
        repository.iniciarReprogramacion(reserva.getCodigoReserva(), estudiante, 99L, "x", AHORA);

        var revertida = repository.revertirReprogramacion(reserva.getCodigoReserva(), estudiante,
            "La franja ya no estaba libre", AHORA.plusSeconds(10));

        assertThat(revertida).isPresent();
        Reserva leida = repository.findById(reserva.getId()).orElseThrow();
        assertThat(leida.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(leida.getFranjaId()).isEqualTo(1L);
        assertThat(leida.getHoraInicio()).isEqualTo("08:00");
        assertThat(leida.getFranjaDestinoId()).isNull();
        assertThat(repository.revertirReprogramacion(reserva.getCodigoReserva(), estudiante, "x", AHORA)).isEmpty();
    }

    @Test
    void existeSolape_cuentaLaReprogramacionAMedias_yPuedeExcluirALaPropiaReserva() {
        Reserva reserva = confirmada(estudiante, HOY, "08:00", "09:00");
        repository.iniciarReprogramacion(reserva.getCodigoReserva(), estudiante, 99L, "x", AHORA);

        assertThat(repository.existeSolape(estudiante, HOY, "08:00")).isTrue();
        assertThat(repository.existeSolape(estudiante, HOY, "08:00", null)).isTrue();
        // * la clase que se mueve no choca consigo misma
        assertThat(repository.existeSolape(estudiante, HOY, "08:00", reserva.getCodigoReserva())).isFalse();
        // * pero sí con otra distinta
        assertThat(repository.existeSolape(estudiante, HOY, "08:00", UUID.randomUUID())).isTrue();
    }

    @Test
    void proximas_incluyeLaReprogramacionAMedias_peroNoLaCancelacionPendiente() {
        Reserva moviendose = confirmada(estudiante, HOY.plusDays(1), "08:00", "09:00");
        repository.iniciarReprogramacion(moviendose.getCodigoReserva(), estudiante, 99L, "x", AHORA);
        Reserva cancelandose = confirmada(estudiante, HOY.plusDays(1), "09:00", "10:00");
        repository.iniciarCancelacion(cancelandose.getCodigoReserva(), estudiante, false, "x", AHORA);

        assertThat(repository.proximas(estudiante, HOY, "08:00", pagina()))
            .extracting(Reserva::getCodigoReserva).containsExactly(moviendose.getCodigoReserva());
        assertThat(repository.historial(estudiante, HOY, "08:00", pagina())).isEmpty();
    }
}
