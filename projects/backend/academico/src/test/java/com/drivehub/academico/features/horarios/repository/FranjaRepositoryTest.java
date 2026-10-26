package com.drivehub.academico.features.horarios.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.common.mongo.AsignarIdentificadoresCallback;
import com.drivehub.academico.common.mongo.SecuenciaService;
import com.drivehub.academico.config.MongoConfig;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;

@DataMongoTest
@Import({AsignarIdentificadoresCallback.class, SecuenciaService.class, MongoConfig.class})
class FranjaRepositoryTest extends MongoIntegrationTestBase {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    @Autowired
    private FranjaRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final UUID instructor = UUID.randomUUID();
    private final UUID otroInstructor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        // * remove() y no dropCollection(): borrar la colección se llevaría los índices únicos
        mongoTemplate.remove(new Query(), Franja.class);
    }

    private Franja guardar(UUID propietario, Long plantillaId, LocalDate fecha, String hora, EstadoFranja estado) {
        Franja franja = new Franja(escuela, plantillaId, propietario, fecha, hora, "23:59");
        franja.setEstado(estado);
        return repository.save(franja);
    }

    private Franja libre(UUID propietario, LocalDate fecha, String hora) {
        return guardar(propietario, 1L, fecha, hora, EstadoFranja.LIBRE);
    }

    private static PageRequest pagina(int numero, int tamano) {
        return PageRequest.of(numero, tamano, Sort.by("fecha", "horaInicio"));
    }

    // ---------------------------------------------------------------- buscar

    @Test
    void buscarPorCodigoEInstructor_soloDevuelveLaDeEseInstructor() {
        Franja franja = libre(instructor, HOY, "10:00");

        assertThat(repository.findByCodigoFranjaAndInstructorCodigoAndDeletedAtIsNull(franja.getCodigoFranja(),
            instructor)).isPresent();
        assertThat(repository.findByCodigoFranjaAndInstructorCodigoAndDeletedAtIsNull(franja.getCodigoFranja(),
            otroInstructor)).isEmpty();
    }

    // ---------------------------------------------------------------- listar

    @Test
    void listar_porFechaExacta_ordenadasPorHora() {
        libre(instructor, HOY, "11:00");
        libre(instructor, HOY, "10:00");
        libre(instructor, HOY.plusDays(1), "10:00");
        libre(otroInstructor, HOY, "10:00");

        var resultado = repository.listar(instructor, HOY, HOY, null, pagina(0, 20));

        assertThat(resultado.getContent()).extracting(Franja::getHoraInicio).containsExactly("10:00", "11:00");
        assertThat(resultado.getTotalElements()).isEqualTo(2);
    }

    @Test
    void listar_desdeUnaFecha_incluyeEsaFechaYLasPosteriores() {
        libre(instructor, HOY.minusDays(1), "10:00");
        libre(instructor, HOY, "10:00");
        libre(instructor, HOY.plusDays(3), "10:00");

        var resultado = repository.listar(instructor, HOY, null, null, pagina(0, 20));

        assertThat(resultado.getContent()).extracting(Franja::getFecha).containsExactly(HOY, HOY.plusDays(3));
    }

    @Test
    void listar_rangoDeFechas_incluyeAmbosExtremosYExcluyeLoDeAfuera() {
        libre(instructor, HOY.minusDays(1), "10:00");
        libre(instructor, HOY, "10:00");
        libre(instructor, HOY.plusDays(2), "10:00");
        libre(instructor, HOY.plusDays(3), "10:00");

        var resultado = repository.listar(instructor, HOY, HOY.plusDays(2), null, pagina(0, 20));

        assertThat(resultado.getContent()).extracting(Franja::getFecha).containsExactly(HOY, HOY.plusDays(2));
    }

    @Test
    void listar_soloHasta_noTieneTopeHaciaAtras() {
        libre(instructor, HOY.minusDays(5), "10:00");
        libre(instructor, HOY, "10:00");
        libre(instructor, HOY.plusDays(1), "10:00");

        var resultado = repository.listar(instructor, null, HOY, null, pagina(0, 20));

        assertThat(resultado.getContent()).extracting(Franja::getFecha).containsExactly(HOY.minusDays(5), HOY);
    }

    @Test
    void listar_filtraPorEstado() {
        libre(instructor, HOY, "10:00");
        guardar(instructor, 1L, HOY, "11:00", EstadoFranja.BLOQUEADA);
        guardar(instructor, 1L, HOY, "12:00", EstadoFranja.RESERVADA);

        var bloqueadas = repository.listar(instructor, HOY, HOY, EstadoFranja.BLOQUEADA, pagina(0, 20));

        assertThat(bloqueadas.getContent()).extracting(Franja::getHoraInicio).containsExactly("11:00");
    }

    @Test
    void listar_sinFiltros_devuelveTodasLasDelInstructorPaginadas() {
        for (int hora = 8; hora < 13; hora++) {
            libre(instructor, HOY, "%02d:00".formatted(hora));
        }
        libre(otroInstructor, HOY, "08:00");

        var primera = repository.listar(instructor, null, null, null, pagina(0, 2));
        var ultima = repository.listar(instructor, null, null, null, pagina(2, 2));

        assertThat(primera.getTotalElements()).isEqualTo(5);
        assertThat(primera.getTotalPages()).isEqualTo(3);
        assertThat(primera.getContent()).hasSize(2);
        assertThat(ultima.getContent()).extracting(Franja::getHoraInicio).containsExactly("12:00");
    }

    @Test
    void listar_unaFechaSinFranjas_devuelveVacio() {
        libre(instructor, HOY, "10:00");

        var resultado = repository.listar(instructor, HOY.plusDays(9), HOY.plusDays(9), null, pagina(0, 20));

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }

    @Test
    void listar_noIncluyeLasBorradasLogicamente() {
        Franja franja = libre(instructor, HOY, "10:00");
        franja.setDeletedAt(Instant.now());
        repository.save(franja);

        assertThat(repository.listar(instructor, HOY, null, null, pagina(0, 20)).getContent()).isEmpty();
    }

    // ---------------------------------------------------------------- cambiarEstado

    @Test
    void cambiarEstado_siCoincideElEstadoEsperado_lo_cambiaYDevuelveLaFranja() {
        Franja franja = libre(instructor, HOY, "10:00");

        var actualizada = repository.cambiarEstado(franja.getCodigoFranja(), instructor, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA);

        assertThat(actualizada).isPresent();
        assertThat(actualizada.get().getEstado()).isEqualTo(EstadoFranja.BLOQUEADA);
        assertThat(repository.findById(franja.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoFranja.BLOQUEADA);
    }

    @Test
    void cambiarEstado_siElEstadoNoCoincide_devuelveVacioYNoCambiaNada() {
        Franja reservada = guardar(instructor, 1L, HOY, "10:00", EstadoFranja.RESERVADA);

        var resultado = repository.cambiarEstado(reservada.getCodigoFranja(), instructor, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA);

        assertThat(resultado).isEmpty();
        assertThat(repository.findById(reservada.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoFranja.RESERVADA);
    }

    @Test
    void cambiarEstado_deLaFranjaDeOtroInstructor_devuelveVacioYNoLaToca() {
        Franja franja = libre(instructor, HOY, "10:00");

        var resultado = repository.cambiarEstado(franja.getCodigoFranja(), otroInstructor, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA);

        assertThat(resultado).isEmpty();
        assertThat(repository.findById(franja.getId()).orElseThrow().getEstado()).isEqualTo(EstadoFranja.LIBRE);
    }

    @Test
    void cambiarEstado_dosVecesSeguidas_laSegundaNoEncuentraNada() {
        Franja franja = libre(instructor, HOY, "10:00");

        var primera = repository.cambiarEstado(franja.getCodigoFranja(), instructor, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA);
        var segunda = repository.cambiarEstado(franja.getCodigoFranja(), instructor, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA);

        assertThat(primera).isPresent();
        assertThat(segunda).isEmpty();
    }

    // ---------------------------------------------------------------- eliminarFuturasNoReservadas

    @Test
    void eliminarFuturasNoReservadas_borraLibresYBloqueadasFuturas() {
        libre(instructor, HOY.plusDays(1), "10:00");
        guardar(instructor, 1L, HOY.plusDays(2), "10:00", EstadoFranja.BLOQUEADA);

        long borradas = repository.eliminarFuturasNoReservadas(1L, HOY, "09:30");

        assertThat(borradas).isEqualTo(2);
        assertThat(repository.count()).isZero();
    }

    @Test
    void eliminarFuturasNoReservadas_noBorraReservadasNiPasadasNiDeOtroHorario() {
        Franja reservada = guardar(instructor, 1L, HOY.plusDays(1), "10:00", EstadoFranja.RESERVADA);
        Franja pasada = libre(instructor, HOY.minusDays(1), "10:00");
        Franja deOtroHorario = guardar(instructor, 2L, HOY.plusDays(1), "11:00", EstadoFranja.LIBRE);

        long borradas = repository.eliminarFuturasNoReservadas(1L, HOY, "09:30");

        assertThat(borradas).isZero();
        assertThat(repository.findAll()).extracting(Franja::getId)
            .containsExactlyInAnyOrder(reservada.getId(), pasada.getId(), deOtroHorario.getId());
    }

    @Test
    void eliminarFuturasNoReservadas_deHoy_soloLasQueEmpiezanDespuesDeLaHoraActual() {
        Franja yaEmpezada = libre(instructor, HOY, "09:00");
        Franja exactaAhora = libre(instructor, HOY, "09:30");
        libre(instructor, HOY, "10:00");

        long borradas = repository.eliminarFuturasNoReservadas(1L, HOY, "09:30");

        assertThat(borradas).isEqualTo(1);
        assertThat(repository.findAll()).extracting(Franja::getId)
            .containsExactlyInAnyOrder(yaEmpezada.getId(), exactaAhora.getId());
    }

    @Test
    void eliminarFuturasNoReservadas_despuesDeBorrar_sePuedeVolverAGenerarLaMismaHora() {
        libre(instructor, HOY.plusDays(1), "10:00");
        repository.eliminarFuturasNoReservadas(1L, HOY, "09:30");

        // * Borrado físico: la clave única (instructor, fecha, hora) quedó libre
        assertThat(repository.insertarSiNoExiste(new Franja(escuela, 2L, instructor, HOY.plusDays(1), "10:00",
            "11:00"))).isTrue();
    }

    // ---------------------------------------------------------------- insertarSiNoExiste

    @Test
    void insertarSiNoExiste_primeraVezTrue_segundaVezFalseSinLanzar() {
        Franja primera = new Franja(escuela, 1L, instructor, HOY, "10:00", "11:00");
        Franja repetida = new Franja(escuela, 1L, instructor, HOY, "10:00", "11:00");

        assertThat(repository.insertarSiNoExiste(primera)).isTrue();
        assertThat(repository.insertarSiNoExiste(repetida)).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void insertarSiNoExiste_otroInstructorEnLaMismaHora_sePuede() {
        assertThat(repository.insertarSiNoExiste(new Franja(escuela, 1L, instructor, HOY, "10:00", "11:00")))
            .isTrue();
        assertThat(repository.insertarSiNoExiste(new Franja(escuela, 1L, otroInstructor, HOY, "10:00", "11:00")))
            .isTrue();
    }

    @Test
    void insertarSiNoExiste_asignaIdYCodigo() {
        Franja franja = new Franja(escuela, 1L, instructor, HOY, "10:00", "11:00");

        repository.insertarSiNoExiste(franja);

        assertThat(franja.getId()).isNotNull();
        assertThat(franja.getCodigoFranja().version()).isEqualTo(7);
        assertThat(franja.getEstado()).isEqualTo(EstadoFranja.LIBRE);
    }

    // ---------------------------------------------------------------- cambiarEstadoDelDia

    @Test
    void cambiarEstadoDelDia_soloCambiaLasDelEstadoEsperadoDeEseDia() {
        libre(instructor, HOY, "08:00");
        libre(instructor, HOY, "09:00");
        guardar(instructor, 1L, HOY, "10:00", EstadoFranja.RESERVADA);
        guardar(instructor, 1L, HOY, "11:00", EstadoFranja.BLOQUEADA);
        libre(instructor, HOY.plusDays(1), "08:00");
        libre(otroInstructor, HOY, "08:00");

        long cambiadas = repository.cambiarEstadoDelDia(instructor, HOY, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA, null);

        assertThat(cambiadas).isEqualTo(2);
        var delDia = repository.listar(instructor, HOY, HOY, null, pagina(0, 20)).getContent();
        assertThat(delDia).extracting(Franja::getHoraInicio, Franja::getEstado).containsExactly(
            org.assertj.core.api.Assertions.tuple("08:00", EstadoFranja.BLOQUEADA),
            org.assertj.core.api.Assertions.tuple("09:00", EstadoFranja.BLOQUEADA),
            org.assertj.core.api.Assertions.tuple("10:00", EstadoFranja.RESERVADA),
            org.assertj.core.api.Assertions.tuple("11:00", EstadoFranja.BLOQUEADA));
        assertThat(repository.listar(instructor, HOY.plusDays(1), HOY.plusDays(1), null, pagina(0, 20))
            .getContent()).extracting(Franja::getEstado).containsExactly(EstadoFranja.LIBRE);
        assertThat(repository.listar(otroInstructor, HOY, HOY, null, pagina(0, 20)).getContent())
            .extracting(Franja::getEstado).containsExactly(EstadoFranja.LIBRE);
    }

    @Test
    void cambiarEstadoDelDia_conHoraMinima_soloTocaLasQueEmpiezanDespues() {
        libre(instructor, HOY, "08:00");
        libre(instructor, HOY, "09:00");
        libre(instructor, HOY, "10:00");

        long cambiadas = repository.cambiarEstadoDelDia(instructor, HOY, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA, "09:00");

        assertThat(cambiadas).isEqualTo(1);
        assertThat(repository.listar(instructor, HOY, HOY, EstadoFranja.LIBRE, pagina(0, 20)).getContent())
            .extracting(Franja::getHoraInicio).containsExactly("08:00", "09:00");
    }

    @Test
    void cambiarEstadoDelDia_sinFranjasDelEstadoEsperado_devuelveCero() {
        guardar(instructor, 1L, HOY, "10:00", EstadoFranja.RESERVADA);

        assertThat(repository.cambiarEstadoDelDia(instructor, HOY, EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA, null))
            .isZero();
    }

    @Test
    void cambiarEstadoDelDia_repetirloNoCambiaNadaMas() {
        libre(instructor, HOY, "08:00");

        long primera = repository.cambiarEstadoDelDia(instructor, HOY, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA, null);
        long segunda = repository.cambiarEstadoDelDia(instructor, HOY, EstadoFranja.LIBRE,
            EstadoFranja.BLOQUEADA, null);

        assertThat(primera).isEqualTo(1);
        assertThat(segunda).isZero();
    }

    @Test
    void cambiarEstadoDelDia_noToca_lasBorradasLogicamente() {
        Franja franja = libre(instructor, HOY, "08:00");
        franja.setDeletedAt(Instant.now());
        repository.save(franja);

        assertThat(repository.cambiarEstadoDelDia(instructor, HOY, EstadoFranja.LIBRE, EstadoFranja.BLOQUEADA, null))
            .isZero();
    }

    // ---------------------------------------------------------------- disponibles para reservar (CU14)

    private Page<Franja> disponibles(LocalDate desde, LocalDate hasta, UUID deInstructor, String horaDesde,
            String horaHasta) {
        return repository.disponibles(escuela, HOY, "09:00", desde, hasta, deInstructor, horaDesde, horaHasta,
            PageRequest.of(0, 50));
    }

    @Test
    void disponibles_soloLibresFuturasDeLaEscuela_ordenadasPorFechaHoraEInstructor() {
        libre(instructor, HOY.plusDays(1), "10:00");
        libre(otroInstructor, HOY, "10:00");
        libre(instructor, HOY, "10:00");
        libre(instructor, HOY, "08:00");                              // ya empezó (ahora 09:00)
        libre(instructor, HOY, "09:00");                              // empieza justo ahora: ya no
        guardar(instructor, 1L, HOY, "11:00", EstadoFranja.RESERVADA);
        guardar(instructor, 1L, HOY, "12:00", EstadoFranja.BLOQUEADA);
        libre(instructor, HOY.minusDays(1), "10:00");
        Franja deOtraEscuela = new Franja(UUID.randomUUID(), 1L, instructor, HOY, "13:00", "14:00");
        repository.save(deOtraEscuela);
        Franja borrada = libre(instructor, HOY, "15:00");
        borrada.setDeletedAt(Instant.now());
        repository.save(borrada);

        Page<Franja> resultado = disponibles(null, null, null, null, null);

        assertThat(resultado.getContent()).extracting(Franja::getFecha, Franja::getHoraInicio)
            .containsExactly(Tuple.tuple(HOY, "10:00"), Tuple.tuple(HOY, "10:00"),
                Tuple.tuple(HOY.plusDays(1), "10:00"));
        assertThat(resultado.getTotalElements()).isEqualTo(3);
    }

    @Test
    void disponibles_filtraPorFechaExactaYPorRango() {
        libre(instructor, HOY, "10:00");
        libre(instructor, HOY.plusDays(1), "10:00");
        libre(instructor, HOY.plusDays(2), "10:00");

        assertThat(disponibles(HOY.plusDays(1), HOY.plusDays(1), null, null, null).getContent())
            .extracting(Franja::getFecha).containsExactly(HOY.plusDays(1));
        assertThat(disponibles(HOY.plusDays(1), HOY.plusDays(2), null, null, null).getContent()).hasSize(2);
        assertThat(disponibles(HOY.plusDays(2), null, null, null, null).getContent())
            .extracting(Franja::getFecha).containsExactly(HOY.plusDays(2));
    }

    @Test
    void disponibles_filtraPorInstructor() {
        libre(instructor, HOY, "10:00");
        libre(otroInstructor, HOY, "11:00");

        assertThat(disponibles(null, null, otroInstructor, null, null).getContent())
            .extracting(Franja::getInstructorCodigo).containsExactly(otroInstructor);
    }

    @Test
    void disponibles_laHoraDesdeEsInclusiva_yLaHoraHastaExclusiva() {
        libre(instructor, HOY.plusDays(1), "11:00");
        libre(instructor, HOY.plusDays(1), "12:00");
        libre(instructor, HOY.plusDays(1), "17:00");
        libre(instructor, HOY.plusDays(1), "18:00");

        assertThat(disponibles(null, null, null, "12:00", "18:00").getContent())
            .extracting(Franja::getHoraInicio).containsExactly("12:00", "17:00");
        assertThat(disponibles(null, null, null, "18:00", null).getContent())
            .extracting(Franja::getHoraInicio).containsExactly("18:00");
        assertThat(disponibles(null, null, null, null, "12:00").getContent())
            .extracting(Franja::getHoraInicio).containsExactly("11:00");
    }

    @Test
    void disponibles_combinaTodosLosFiltros() {
        libre(instructor, HOY.plusDays(1), "10:00");
        libre(instructor, HOY.plusDays(1), "15:00");
        libre(otroInstructor, HOY.plusDays(1), "15:00");
        libre(instructor, HOY.plusDays(2), "15:00");

        assertThat(disponibles(HOY.plusDays(1), HOY.plusDays(1), instructor, "12:00", "18:00").getContent())
            .hasSize(1).first().satisfies(f -> {
                assertThat(f.getInstructorCodigo()).isEqualTo(instructor);
                assertThat(f.getHoraInicio()).isEqualTo("15:00");
            });
    }

    @Test
    void disponibles_paginaConElTotalCorrecto() {
        for (int i = 0; i < 5; i++) {
            libre(instructor, HOY.plusDays(1), "1%d:00".formatted(i));
        }

        Page<Franja> segunda = repository.disponibles(escuela, HOY, "09:00", null, null, null, null, null,
            PageRequest.of(1, 2));

        assertThat(segunda.getContent()).extracting(Franja::getHoraInicio).containsExactly("12:00", "13:00");
        assertThat(segunda.getTotalElements()).isEqualTo(5);
        assertThat(segunda.getTotalPages()).isEqualTo(3);
    }

    @Test
    void instructoresConDisponibles_sinRepetirYSoloLosQueTienenFranjasLibres() {
        libre(instructor, HOY.plusDays(1), "10:00");
        libre(instructor, HOY.plusDays(1), "11:00");
        guardar(otroInstructor, 1L, HOY.plusDays(1), "10:00", EstadoFranja.RESERVADA);

        assertThat(repository.instructoresConDisponibles(escuela, HOY, "09:00", null, null))
            .containsExactly(instructor);
        assertThat(repository.instructoresConDisponibles(escuela, HOY, "09:00", HOY.plusDays(5), null)).isEmpty();
    }

    // ---------------------------------------------------------------- apartar y liberar (CU14)

    @Test
    void apartar_pasaLaFranjaLibreAReservadaYGuardaLaReserva() {
        Franja franja = libre(instructor, HOY.plusDays(1), "10:00");

        var apartada = repository.apartar(franja.getCodigoFranja(), escuela, 77L, HOY, "09:00");

        assertThat(apartada).isPresent();
        assertThat(apartada.get().getEstado()).isEqualTo(EstadoFranja.RESERVADA);
        assertThat(apartada.get().getReservaId()).isEqualTo(77L);
    }

    @Test
    void apartar_noApartaFranjasDeOtraEscuelaBloqueadasReservadasYaEmpezadasOBorradas() {
        Franja deOtraEscuela = repository.save(new Franja(UUID.randomUUID(), 1L, instructor, HOY.plusDays(1),
            "10:00", "11:00"));
        Franja bloqueada = guardar(instructor, 1L, HOY.plusDays(1), "11:00", EstadoFranja.BLOQUEADA);
        Franja reservada = guardar(instructor, 1L, HOY.plusDays(1), "12:00", EstadoFranja.RESERVADA);
        Franja empezada = libre(instructor, HOY, "08:00");
        Franja borrada = libre(instructor, HOY.plusDays(1), "13:00");
        borrada.setDeletedAt(Instant.now());
        repository.save(borrada);

        for (Franja f : new Franja[] {deOtraEscuela, bloqueada, reservada, empezada, borrada}) {
            assertThat(repository.apartar(f.getCodigoFranja(), escuela, 1L, HOY, "09:00")).isEmpty();
        }
        assertThat(repository.findById(bloqueada.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoFranja.BLOQUEADA);
        assertThat(repository.findById(reservada.getId()).orElseThrow().getReservaId()).isNull();
    }

    @Test
    void apartar_dosHilosALaVez_unoSoloSeQuedaConLaFranja() throws Exception {
        Franja franja = libre(instructor, HOY.plusDays(1), "10:00");
        int hilos = 8;
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (long reserva = 1; reserva <= hilos; reserva++) {
            long reservaId = reserva;
            resultados.add(ejecutor.submit(() -> {
                salida.await();
                return repository.apartar(franja.getCodigoFranja(), escuela, reservaId, HOY, "09:00").isPresent();
            }));
        }
        salida.countDown();
        int ganadoras = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get()) {
                ganadoras++;
            }
        }
        ejecutor.shutdown();

        assertThat(ganadoras).isEqualTo(1);
    }

    @Test
    void liberar_devuelveLaFranjaALibreSoloSiSigueReservadaPorEsaReserva() {
        Franja franja = libre(instructor, HOY.plusDays(1), "10:00");
        repository.apartar(franja.getCodigoFranja(), escuela, 5L, HOY, "09:00");

        assertThat(repository.liberar(franja.getCodigoFranja(), 99L)).isFalse();
        assertThat(repository.findById(franja.getId()).orElseThrow().getEstado()).isEqualTo(EstadoFranja.RESERVADA);

        assertThat(repository.liberar(franja.getCodigoFranja(), 5L)).isTrue();
        Franja liberada = repository.findById(franja.getId()).orElseThrow();
        assertThat(liberada.getEstado()).isEqualTo(EstadoFranja.LIBRE);
        assertThat(liberada.getReservaId()).isNull();

        assertThat(repository.liberar(franja.getCodigoFranja(), 5L)).isFalse();
    }

    @Test
    void buscarPorCodigoYEscuela_distingueLaFranjaInexistenteDeLaDeOtraEscuela() {
        Franja franja = libre(instructor, HOY.plusDays(1), "10:00");

        assertThat(repository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(franja.getCodigoFranja(), escuela))
            .isPresent();
        assertThat(repository.findByCodigoFranjaAndEscuelaCodigoAndDeletedAtIsNull(franja.getCodigoFranja(),
            UUID.randomUUID())).isEmpty();
    }
}
