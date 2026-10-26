package com.drivehub.academico.features.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.repository.FranjaRepository;
import com.drivehub.academico.features.reservas.client.ServidorFalso;
import com.drivehub.academico.features.reservas.client.ServidorFalso.Peticion;
import com.drivehub.academico.features.reservas.client.ServidorFalso.Respuesta;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;
import com.drivehub.academico.features.reservas.repository.ReservaRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * CU14 de punta a punta: controller, service con su @PreAuthorize, repositorios y MongoDB real (replica set de un
 * nodo). Pagos e Identidad son servidores falsos: el de Pagos lleva un saldo y respeta la idempotencia, como el
 * de verdad. Cada método nombra el criterio de aceptación (CAxx).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservarHorarioIntegracionTest extends MongoIntegrationTestBase {

    private static final String RUTA = "/api/v1/reservas";
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final Pattern CLAVE = Pattern.compile("\"([0-9a-f-]{36})\"");

    private static ServidorFalso pagos;
    private static ServidorFalso identidad;

    /** El saldo y los descuentos del Pagos falso. */
    private static final AtomicInteger SALDO = new AtomicInteger();
    private static final Set<String> CLAVES_DESCONTADAS = new HashSet<>();
    private static final AtomicInteger DESCUENTOS_REALES = new AtomicInteger();
    private static volatile boolean pagosCaido;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private FranjaRepository franjaRepository;
    @Autowired
    private ReservaRepository reservaRepository;

    private final UUID escuela = UUID.randomUUID();
    private final UUID instructorA = UUID.randomUUID();
    private final UUID instructorB = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID otroEstudiante = UUID.randomUUID();
    private final LocalDate manana = LocalDate.now(ZONA).plusDays(3);

    @BeforeAll
    static void levantarServidoresFalsos() throws IOException {
        pagos = new ServidorFalso();
        pagos.responder(ReservarHorarioIntegracionTest::responderPagos);
        identidad = new ServidorFalso();
        identidad.responder(ReservarHorarioIntegracionTest::responderIdentidad);
    }

    @AfterAll
    static void apagarServidoresFalsos() {
        if (pagos != null) {
            pagos.close();
        }
        if (identidad != null) {
            identidad.close();
        }
    }

    @DynamicPropertySource
    static void propiedadesDeLosServiciosFalsos(DynamicPropertyRegistry registry) {
        // * Los servidores los crea @BeforeAll, antes de que Spring cargue el contexto
        registry.add("app.pagos.url", () -> pagos.url());
        registry.add("app.identidad.url", () -> identidad.url());
    }

    @BeforeEach
    void limpiar() {
        mongoTemplate.remove(new Query(), Reserva.class);
        mongoTemplate.remove(new Query(), Franja.class);
        synchronized (CLAVES_DESCONTADAS) {
            CLAVES_DESCONTADAS.clear();
        }
        DESCUENTOS_REALES.set(0);
        SALDO.set(3);
        pagosCaido = false;
    }

    // ---------------------------------------------------------------- los servicios falsos

    private static Respuesta responderPagos(Peticion p) {
        // * Cae solo el descuento: el saldo y las devoluciones siguen respondiendo
        if (pagosCaido && p.ruta().endsWith("/consumos")) {
            return new Respuesta(500, "{}");
        }
        if (p.ruta().endsWith("/mia")) {
            return ok(200, "{\"horasDisponibles\":" + SALDO.get() + "}");
        }
        if (p.ruta().endsWith("/consumos")) {
            synchronized (CLAVES_DESCONTADAS) {
                if (CLAVES_DESCONTADAS.contains(p.claveIdempotente())) {
                    return ok(201, "{\"horasDescontadas\":1,\"horasDisponibles\":" + SALDO.get() + "}");
                }
                if (SALDO.get() <= 0) {
                    return new Respuesta(409, "{\"status\":\"failed\",\"error\":\"ERR_PAG_09\"}");
                }
                CLAVES_DESCONTADAS.add(p.claveIdempotente());
                DESCUENTOS_REALES.incrementAndGet();
                return ok(201, "{\"horasDescontadas\":1,\"horasDisponibles\":" + SALDO.decrementAndGet() + "}");
            }
        }
        if (p.ruta().endsWith("/devoluciones")) {
            return new Respuesta(404, "{\"status\":\"failed\",\"error\":\"ERR_PAG_16\"}");
        }
        return new Respuesta(404, "{}");
    }

    private static Respuesta responderIdentidad(Peticion p) {
        // * Devuelve un nombre para cada código que llegó en la petición
        List<String> datos = new ArrayList<>();
        Matcher m = CLAVE.matcher(p.cuerpo());
        while (m.find()) {
            datos.add("{\"codigo\":\"" + m.group(1) + "\",\"nombreCompleto\":\"Instructor " + m.group(1).substring(0, 4)
                + "\"}");
        }
        return ok(200, "[" + String.join(",", datos) + "]");
    }

    private static Respuesta ok(int estado, String data) {
        return new Respuesta(estado, "{\"status\":\"success\",\"data\":" + data + ",\"message\":\"ok\"}");
    }

    // ---------------------------------------------------------------- apoyo

    private RequestPostProcessor comoEstudiante(UUID quien, UUID deEscuela) {
        return jwt().jwt(j -> j.subject(quien.toString()).claim("escuela", deEscuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"));
    }

    private Franja franja(UUID deEscuela, UUID instructor, LocalDate fecha, String inicio, String fin) {
        return franjaRepository.save(new Franja(deEscuela, 1L, instructor, fecha, inicio, fin));
    }

    private static String cuerpo(Franja... franjas) {
        StringBuilder json = new StringBuilder("{\"franjaCodigos\":[");
        for (int i = 0; i < franjas.length; i++) {
            json.append(i == 0 ? "" : ",").append('"').append(franjas[i].getCodigoFranja()).append('"');
        }
        return json.append("]}").toString();
    }

    private String reservar(UUID quien, UUID deEscuela, Franja... franjas) throws Exception {
        return mockMvc.perform(post(RUTA).with(comoEstudiante(quien, deEscuela))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(franjas)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private Franja recargar(Franja franja) {
        return franjaRepository.findById(franja.getId()).orElseThrow();
    }

    // ---------------------------------------------------------------- escenarios

    @Test
    void ca03_ca08_conTresHorasYCuatroFranjas_reservaTresRechazaLaCuartaYElSaldoQuedaEnCero() throws Exception {
        Franja a = franja(escuela, instructorA, manana, "08:00", "09:00");
        Franja b = franja(escuela, instructorA, manana, "09:00", "10:00");
        Franja c = franja(escuela, instructorB, manana, "10:00", "11:00");
        Franja d = franja(escuela, instructorB, manana, "11:00", "12:00");

        String respuesta = reservar(estudiante, escuela, a, b, c, d);

        assertThat(JsonPath.<List<String>>read(respuesta, "$.data.reservadas[*].horaInicio"))
            .containsExactly("08:00", "09:00", "10:00");
        assertThat(JsonPath.<String>read(respuesta, "$.data.rechazadas[0].error")).isEqualTo("ERR_ACA_19");
        assertThat(JsonPath.<String>read(respuesta, "$.data.rechazadas[0].horaInicio")).isEqualTo("11:00");
        assertThat(JsonPath.<Integer>read(respuesta, "$.data.horasDisponibles")).isZero();
        assertThat(SALDO.get()).isZero();
        assertThat(recargar(a).getEstado()).isEqualTo(EstadoFranja.RESERVADA);
        assertThat(recargar(d).getEstado()).isEqualTo(EstadoFranja.LIBRE);

        // * Mis clases: las tres, de la más próxima a la más lejana, con el nombre del instructor
        mockMvc.perform(get(RUTA).with(comoEstudiante(estudiante, escuela)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].horaInicio").value("08:00"))
            .andExpect(jsonPath("$.data[2].horaInicio").value("10:00"))
            .andExpect(jsonPath("$.data[0].estado").value("CONFIRMADA"))
            .andExpect(jsonPath("$.data[0].instructorNombre").value("Instructor " + instructorA.toString().substring(0, 4)))
            .andExpect(jsonPath("$.meta.totalItems").value(3));
        // * y el historial todavía no tiene nada
        mockMvc.perform(get(RUTA).param("filter[vista]", "historial").with(comoEstudiante(estudiante, escuela)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void ca05_dosEstudiantesPidenLaMismaFranjaALaVez_unoSoloLaReservaYPagosDescuentaUnaVez() throws Exception {
        SALDO.set(10);
        Franja franja = franja(escuela, instructorA, manana, "08:00", "09:00");
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> respuestas = new ArrayList<>();
        for (UUID quien : List.of(estudiante, otroEstudiante)) {
            respuestas.add(ejecutor.submit(() -> {
                salida.await();
                return reservar(quien, escuela, franja);
            }));
        }
        salida.countDown();
        int reservadas = 0;
        int rechazadas = 0;
        for (Future<String> respuesta : respuestas) {
            String json = respuesta.get();
            reservadas += JsonPath.<List<?>>read(json, "$.data.reservadas").size();
            List<String> errores = JsonPath.read(json, "$.data.rechazadas[*].error");
            rechazadas += errores.size();
            errores.forEach(e -> assertThat(e).isEqualTo("ERR_ACA_18"));
        }
        ejecutor.shutdown();

        assertThat(reservadas).isEqualTo(1);
        assertThat(rechazadas).isEqualTo(1);
        assertThat(DESCUENTOS_REALES.get()).isEqualTo(1);
        assertThat(reservaRepository.findAll().stream().filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA))
            .hasSize(1);
        assertThat(recargar(franja).getEstado()).isEqualTo(EstadoFranja.RESERVADA);
    }

    @Test
    void ca09_siPagosNoRespondeAlDescontar_laFranjaQuedaLibreYNoHayReservaVisible() throws Exception {
        Franja franja = franja(escuela, instructorA, manana, "08:00", "09:00");
        pagosCaido = true;

        String respuesta = reservar(estudiante, escuela, franja);

        assertThat(JsonPath.<List<?>>read(respuesta, "$.data.reservadas")).isEmpty();
        assertThat(JsonPath.<String>read(respuesta, "$.data.rechazadas[0].error")).isEqualTo("ERR_ACA_17");
        assertThat(recargar(franja).getEstado()).isEqualTo(EstadoFranja.LIBRE);
        pagosCaido = false;
        mockMvc.perform(get(RUTA).with(comoEstudiante(estudiante, escuela)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        assertThat(reservaRepository.findAll()).allSatisfy(r -> assertThat(r.getEstado())
            .isEqualTo(EstadoReserva.CANCELADA));
    }

    @Test
    void ca11_unaReservaAMediasDeMasDeDosMinutos_seConfirmaAlConsultarSusClases() throws Exception {
        Franja franja = franja(escuela, instructorA, manana, "08:00", "09:00");
        Reserva pendiente = reservaRepository.save(new Reserva(estudiante, instructorA, franja.getId(), manana,
            "08:00", "09:00", 1, Instant.now()));
        franjaRepository.apartar(franja.getCodigoFranja(), escuela, pendiente.getId(), LocalDate.now(ZONA), "00:00");
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(pendiente.getId())),
            Update.update("createdAt", Instant.now().minusSeconds(600)), Reserva.class);

        mockMvc.perform(get(RUTA).with(comoEstudiante(estudiante, escuela)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].estado").value("CONFIRMADA"));

        assertThat(DESCUENTOS_REALES.get()).isEqualTo(1);
        assertThat(reservaRepository.findById(pendiente.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(recargar(franja).getEstado()).isEqualTo(EstadoFranja.RESERVADA);
    }

    @Test
    void ca11_unaReservaAMediasSinHoras_seCancelaYLaFranjaQuedaLibre() throws Exception {
        SALDO.set(0);
        Franja franja = franja(escuela, instructorA, manana, "08:00", "09:00");
        Reserva pendiente = reservaRepository.save(new Reserva(estudiante, instructorA, franja.getId(), manana,
            "08:00", "09:00", 1, Instant.now()));
        franjaRepository.apartar(franja.getCodigoFranja(), escuela, pendiente.getId(), LocalDate.now(ZONA), "00:00");
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(pendiente.getId())),
            Update.update("createdAt", Instant.now().minusSeconds(600)), Reserva.class);

        mockMvc.perform(get(RUTA).with(comoEstudiante(estudiante, escuela)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));

        assertThat(reservaRepository.findById(pendiente.getId()).orElseThrow().getEstado())
            .isEqualTo(EstadoReserva.CANCELADA);
        assertThat(recargar(franja).getEstado()).isEqualTo(EstadoFranja.LIBRE);
    }

    @Test
    void ca16_unEstudianteNoVeLasReservasDeOtro_yUnaFranjaDeOtraEscuelaSeRechaza() throws Exception {
        Franja suya = franja(escuela, instructorA, manana, "08:00", "09:00");
        Franja deOtraEscuela = franja(UUID.randomUUID(), instructorB, manana, "09:00", "10:00");
        reservar(estudiante, escuela, suya);

        mockMvc.perform(get(RUTA).with(comoEstudiante(otroEstudiante, escuela)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));

        String respuesta = reservar(otroEstudiante, escuela, deOtraEscuela);
        assertThat(JsonPath.<String>read(respuesta, "$.data.rechazadas[0].error")).isEqualTo("ERR_ACA_18");
        assertThat(recargar(deOtraEscuela).getEstado()).isEqualTo(EstadoFranja.LIBRE);
    }

    @Test
    void ca07_elEstudianteNoPuedeTenerDosClasesALaMismaHora() throws Exception {
        Franja conA = franja(escuela, instructorA, manana, "08:00", "09:00");
        Franja conB = franja(escuela, instructorB, manana, "08:00", "09:00");
        reservar(estudiante, escuela, conA);

        String respuesta = reservar(estudiante, escuela, conB);

        assertThat(JsonPath.<String>read(respuesta, "$.data.rechazadas[0].error")).isEqualTo("ERR_ACA_20");
        assertThat(recargar(conB).getEstado()).isEqualTo(EstadoFranja.LIBRE);
        assertThat(DESCUENTOS_REALES.get()).isEqualTo(1);
    }

    @Test
    void ca15_sinSesionResponde401_yConOtroRolResponde403() throws Exception {
        Franja franja = franja(escuela, instructorA, manana, "08:00", "09:00");
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo(franja)))
            .andExpect(status().isUnauthorized());
        RequestPostProcessor instructor = jwt().jwt(j -> j.subject(instructorA.toString())
            .claim("escuela", escuela.toString())).authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
        mockMvc.perform(post(RUTA).with(instructor).contentType(MediaType.APPLICATION_JSON).content(cuerpo(franja)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA).with(instructor)).andExpect(status().isForbidden());
    }
}
