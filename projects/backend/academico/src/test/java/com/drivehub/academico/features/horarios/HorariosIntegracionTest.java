package com.drivehub.academico.features.horarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.features.horarios.enums.EstadoFranja;
import com.drivehub.academico.features.horarios.model.Franja;
import com.drivehub.academico.features.horarios.model.PlantillaHorario;
import com.drivehub.academico.features.horarios.service.FranjaGeneracionService;
import com.drivehub.academico.features.horarios.service.ResumenExtension;
import com.jayway.jsonpath.JsonPath;

/**
 * CU12 de punta a punta: seguridad, controllers, services y repositorios contra un MongoDB real.
 *
 * * El JWT se simula con jwt() (no se levanta Identidad ni el gateway).
 * * El reloj es de prueba y se mueve a mano: arranca un lunes a las 09:30 de la zona de la escuela, de
 * * modo que de las 4 franjas del lunes de hoy ya empezaron las de las 08:00 y las 09:00.
 * * La generación diaria no corre sola (job deshabilitado): se invoca a mano.
 */
@SpringBootTest(properties = "app.horarios.job-habilitado=false")
@AutoConfigureMockMvc
class HorariosIntegracionTest extends MongoIntegrationTestBase {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // * Lunes 26/10/2026 09:30. Ventana de 28 días: lunes 26/10, 02/11, 09/11 y 16/11
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 26, 9, 30);
    private static final String HOY = "2026-10-26";
    private static final String LUNES_SIGUIENTE = "2026-11-02";
    private static final String DOS_LUNES_DESPUES = "2026-11-09";
    private static final String RUTA_HORARIOS = "/api/v1/plantillas-horario";
    private static final String RUTA_FRANJAS = "/api/v1/franjas";
    private static final String LUNES_DE_OCHO_A_DOCE = """
        {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 60}
        """;

    /** Reloj que se mueve a mano para probar el paso del tiempo. */
    static class RelojMutable extends Clock {

        private volatile Instant instante = INICIO.atZone(ZONA).toInstant();

        void fijar(LocalDateTime fechaHora) {
            instante = fechaHora.atZone(ZONA).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZONA;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instante, zone);
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    private static final RelojMutable RELOJ = new RelojMutable();

    @TestConfiguration
    static class RelojDePrueba {

        @Bean
        @Primary
        Clock relojDePrueba() {
            return RELOJ;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private FranjaGeneracionService generacion;

    private final UUID escuela = UUID.randomUUID();
    private final UUID instructorA = UUID.randomUUID();
    private final UUID instructorB = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        // * remove() y no dropCollection(): borrar la colección se llevaría los índices únicos
        mongoTemplate.remove(new Query(), PlantillaHorario.class);
        mongoTemplate.remove(new Query(), Franja.class);
        RELOJ.fijar(INICIO);
    }

    // ---------------------------------------------------------------- ayudas

    private RequestPostProcessor comoInstructor(UUID instructor) {
        return jwt().jwt(j -> j.subject(instructor.toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
    }

    private RequestPostProcessor comoEstudiante() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"));
    }

    private ResultActions crearHorario(UUID instructor, String cuerpo) throws Exception {
        return mockMvc.perform(post(RUTA_HORARIOS).contentType(MediaType.APPLICATION_JSON).content(cuerpo)
            .with(comoInstructor(instructor)));
    }

    private String crearLunesDeOchoADoce(UUID instructor) throws Exception {
        String respuesta = crearHorario(instructor, LUNES_DE_OCHO_A_DOCE).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.data.codigo");
    }

    private ResultActions listarFranjas(UUID instructor, String fecha) throws Exception {
        var peticion = get(RUTA_FRANJAS).with(comoInstructor(instructor));
        return mockMvc.perform(fecha == null ? peticion : peticion.param("filter[fecha]", fecha));
    }

    /** El código de la franja en la posición indicada de las de esa fecha (ordenadas por hora). */
    private String codigoDeFranja(UUID instructor, String fecha, int posicion) throws Exception {
        String respuesta = listarFranjas(instructor, fecha).andExpect(status().isOk()).andReturn().getResponse()
            .getContentAsString();
        return JsonPath.read(respuesta, "$.data[" + posicion + "].codigo");
    }

    private void marcarReservada(String codigoFranja) {
        mongoTemplate.updateFirst(query(where("codigoFranja").is(UUID.fromString(codigoFranja))),
            new Update().set("estado", EstadoFranja.RESERVADA), Franja.class);
    }

    private ResultActions accion(String ruta, UUID instructor) throws Exception {
        return mockMvc.perform(post(ruta).with(comoInstructor(instructor)));
    }

    // ---------------------------------------------------------------- crear (CA02, CA03, CA04)

    @Test
    void crear_lunesDeOchoADoce_generaCatorceFranjasPorqueDosDeHoyYaEmpezaron() throws Exception {
        crearHorario(instructorA, LUNES_DE_OCHO_A_DOCE)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Horario creado correctamente"))
            .andExpect(jsonPath("$.data.franjasGeneradas").value(14))
            .andExpect(jsonPath("$.data.activa").value(true))
            .andExpect(jsonPath("$.data.vigenteDesde").value(HOY))
            .andExpect(jsonPath("$.data.codigo").exists());

        assertThat(mongoTemplate.count(new Query(), Franja.class)).isEqualTo(14);
        assertThat(mongoTemplate.count(new Query(), PlantillaHorario.class)).isEqualTo(1);

        // * Hoy (09:30): ya empezaron las de las 08:00 y las 09:00; quedan las de las 10:00 y las 11:00
        listarFranjas(instructorA, HOY).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].horaInicio").value("10:00"))
            .andExpect(jsonPath("$.data[1].horaInicio").value("11:00"));
        listarFranjas(instructorA, LUNES_SIGUIENTE).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(4))
            .andExpect(jsonPath("$.data[0].horaInicio").value("08:00"))
            .andExpect(jsonPath("$.data[3].horaFin").value("12:00"))
            .andExpect(jsonPath("$.data[0].estado").value("LIBRE"));
    }

    @Test
    void crear_laEscuelaQuedaGuardadaEnElHorarioYEnSusFranjas() throws Exception {
        crearLunesDeOchoADoce(instructorA);

        assertThat(mongoTemplate.findAll(PlantillaHorario.class)).allSatisfy(h -> {
            assertThat(h.getEscuelaCodigo()).isEqualTo(escuela);
            assertThat(h.getInstructorCodigo()).isEqualTo(instructorA);
        });
        assertThat(mongoTemplate.findAll(Franja.class)).allSatisfy(f -> {
            assertThat(f.getEscuelaCodigo()).isEqualTo(escuela);
            assertThat(f.getVehiculoCodigo()).isNull();
        });
    }

    @Test
    void dosInstructores_conElMismoHorario_ambosGeneranSusFranjasSinChocar() throws Exception {
        crearLunesDeOchoADoce(instructorA);
        crearHorario(instructorB, LUNES_DE_OCHO_A_DOCE).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.franjasGeneradas").value(14));

        assertThat(mongoTemplate.count(new Query(), Franja.class)).isEqualTo(28);
    }

    // ---------------------------------------------------------------- listar horarios (CA01)

    @Test
    void listarHorarios_conYSinFiltroDeActiva_yConMeta() throws Exception {
        crearLunesDeOchoADoce(instructorA);

        mockMvc.perform(get(RUTA_HORARIOS).with(comoInstructor(instructorA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].diaSemana").value(1))
            .andExpect(jsonPath("$.data[0].horaInicio").value("08:00"))
            .andExpect(jsonPath("$.meta.totalItems").value(1))
            .andExpect(jsonPath("$.meta.totalPages").value(1));
        mockMvc.perform(get(RUTA_HORARIOS).param("filter[activa]", "true").with(comoInstructor(instructorA)))
            .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get(RUTA_HORARIOS).param("filter[activa]", "false").with(comoInstructor(instructorA)))
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    @Test
    void listarHorarios_ordenaPorDiaYHora() throws Exception {
        crearHorario(instructorA, """
            {"diaSemana": 3, "horaInicio": "08:00", "horaFin": "10:00", "duracionClaseMin": 60}""")
            .andExpect(status().isCreated());
        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "14:00", "horaFin": "16:00", "duracionClaseMin": 60}""")
            .andExpect(status().isCreated());
        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "10:00", "duracionClaseMin": 60}""")
            .andExpect(status().isCreated());

        mockMvc.perform(get(RUTA_HORARIOS).with(comoInstructor(instructorA)))
            .andExpect(jsonPath("$.data[0].diaSemana").value(1))
            .andExpect(jsonPath("$.data[0].horaInicio").value("08:00"))
            .andExpect(jsonPath("$.data[1].horaInicio").value("14:00"))
            .andExpect(jsonPath("$.data[2].diaSemana").value(3));
    }

    @Test
    void listarFranjas_sinFecha_devuelveDesdeHoyEnAdelante() throws Exception {
        crearLunesDeOchoADoce(instructorA);

        listarFranjas(instructorA, null).andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.totalItems").value(14))
            .andExpect(jsonPath("$.data[0].fecha").value(HOY));
    }

    @Test
    void listarFranjas_fechaSinFranjas_devuelveListaVaciaNoError() throws Exception {
        crearLunesDeOchoADoce(instructorA);

        listarFranjas(instructorA, "2026-11-03").andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    // ---------------------------------------------------------------- datos inválidos (CA05, CA06)

    @Test
    void crear_datosInvalidos_respondenConSuCodigoYNoCreanNada() throws Exception {
        crearHorario(instructorA, """
            {"diaSemana": 9, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 60}""")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "12:00", "horaFin": "12:00", "duracionClaseMin": 60}""")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ACA_01"));
        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 90}""")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ACA_02"));
        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 10}""")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ACA_02"));
        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 60,
             "vigenteDesde": "2026-11-10", "vigenteHasta": "2026-11-01"}""")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ACA_03"));
        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 60,
             "vigenteDesde": "2026-10-25"}""")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_ACA_03"));

        assertThat(mongoTemplate.count(new Query(), PlantillaHorario.class)).isZero();
        assertThat(mongoTemplate.count(new Query(), Franja.class)).isZero();
    }

    // ---------------------------------------------------------------- cruce (CA07)

    @Test
    void crear_cruceConOtroHorario_responde409_peroCompartirElBordeSiSePermite() throws Exception {
        crearLunesDeOchoADoce(instructorA);

        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "11:00", "horaFin": "13:00", "duracionClaseMin": 60}""")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACA_04"));
        assertThat(mongoTemplate.count(new Query(), PlantillaHorario.class)).isEqualTo(1);

        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "12:00", "horaFin": "14:00", "duracionClaseMin": 60}""")
            .andExpect(status().isCreated());
        assertThat(mongoTemplate.count(new Query(), PlantillaHorario.class)).isEqualTo(2);
    }

    // ---------------------------------------------------------------- bloquear y desbloquear (CA09 a CA11)

    @Test
    void bloquearYDesbloquear_unaFranjaLibreFutura() throws Exception {
        crearLunesDeOchoADoce(instructorA);
        String franja = codigoDeFranja(instructorA, LUNES_SIGUIENTE, 0);

        accion(RUTA_FRANJAS + "/" + franja + "/bloquear", instructorA).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.estado").value("BLOQUEADA"));
        listarFranjas(instructorA, LUNES_SIGUIENTE).andExpect(jsonPath("$.data[0].estado").value("BLOQUEADA"));

        accion(RUTA_FRANJAS + "/" + franja + "/desbloquear", instructorA).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.estado").value("LIBRE"));
        listarFranjas(instructorA, LUNES_SIGUIENTE).andExpect(jsonPath("$.data[0].estado").value("LIBRE"));
    }

    @Test
    void bloquear_laYaBloqueada_409ErrAca08_yDesbloquearUnaLibre_409ErrAca09() throws Exception {
        crearLunesDeOchoADoce(instructorA);
        String franja = codigoDeFranja(instructorA, LUNES_SIGUIENTE, 0);

        accion(RUTA_FRANJAS + "/" + franja + "/desbloquear", instructorA).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_09"));
        accion(RUTA_FRANJAS + "/" + franja + "/bloquear", instructorA).andExpect(status().isOk());
        accion(RUTA_FRANJAS + "/" + franja + "/bloquear", instructorA).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_08"));
    }

    @Test
    void bloquear_unaFranjaReservada_409ErrAca08YSinCambios() throws Exception {
        crearLunesDeOchoADoce(instructorA);
        String franja = codigoDeFranja(instructorA, LUNES_SIGUIENTE, 1);
        marcarReservada(franja);

        accion(RUTA_FRANJAS + "/" + franja + "/bloquear", instructorA).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_08"));
        accion(RUTA_FRANJAS + "/" + franja + "/desbloquear", instructorA).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_09"));
        listarFranjas(instructorA, LUNES_SIGUIENTE).andExpect(jsonPath("$.data[1].estado").value("RESERVADA"));
    }

    @Test
    void bloquearYDesbloquear_unaFranjaQueYaEmpezo_409ErrAca10() throws Exception {
        crearLunesDeOchoADoce(instructorA);
        String libre = codigoDeFranja(instructorA, HOY, 0);
        String bloqueada = codigoDeFranja(instructorA, HOY, 1);
        accion(RUTA_FRANJAS + "/" + bloqueada + "/bloquear", instructorA).andExpect(status().isOk());

        // * Pasa el tiempo: son las 11:30 y las franjas de las 10:00 y las 11:00 de hoy ya empezaron
        RELOJ.fijar(LocalDateTime.of(2026, 10, 26, 11, 30));

        accion(RUTA_FRANJAS + "/" + libre + "/bloquear", instructorA).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_10"));
        accion(RUTA_FRANJAS + "/" + bloqueada + "/desbloquear", instructorA).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_10"));
    }

    // ---------------------------------------------------------------- ajeno (CA12, CA17)

    @Test
    void otroInstructor_noVeNiTocaLoDeA_yRecibeNoEncontrado() throws Exception {
        String horarioDeA = crearLunesDeOchoADoce(instructorA);
        String franjaDeA = codigoDeFranja(instructorA, LUNES_SIGUIENTE, 0);

        mockMvc.perform(get(RUTA_HORARIOS).with(comoInstructor(instructorB)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        listarFranjas(instructorB, LUNES_SIGUIENTE).andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
        accion(RUTA_FRANJAS + "/" + franjaDeA + "/bloquear", instructorB).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ACA_07"));
        accion(RUTA_FRANJAS + "/" + franjaDeA + "/desbloquear", instructorB).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ACA_07"));
        accion(RUTA_HORARIOS + "/" + horarioDeA + "/desactivar", instructorB).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ACA_05"));

        // * Nada de A cambió
        listarFranjas(instructorA, LUNES_SIGUIENTE).andExpect(jsonPath("$.data[0].estado").value("LIBRE"));
        mockMvc.perform(get(RUTA_HORARIOS).param("filter[activa]", "true").with(comoInstructor(instructorA)))
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void inexistentes_responden404() throws Exception {
        accion(RUTA_FRANJAS + "/" + UUID.randomUUID() + "/bloquear", instructorA).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ERR_ACA_07"));
        accion(RUTA_HORARIOS + "/" + UUID.randomUUID() + "/desactivar", instructorA)
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACA_05"));
    }

    // ---------------------------------------------------------------- desactivar (CA13, CA14)

    @Test
    void desactivar_quitaLasFranjasFuturasNoReservadas_conservaLasReservadas_yPermiteCrearOtroEnLaMismaHora()
            throws Exception {
        String horario = crearLunesDeOchoADoce(instructorA);
        String reservada = codigoDeFranja(instructorA, DOS_LUNES_DESPUES, 0);
        marcarReservada(reservada);
        String bloqueada = codigoDeFranja(instructorA, LUNES_SIGUIENTE, 2);
        accion(RUTA_FRANJAS + "/" + bloqueada + "/bloquear", instructorA).andExpect(status().isOk());

        accion(RUTA_HORARIOS + "/" + horario + "/desactivar", instructorA).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Horario desactivado correctamente"))
            .andExpect(jsonPath("$.data").doesNotExist());

        // * Solo queda la reservada: se borraron las libres y la bloqueada
        assertThat(mongoTemplate.findAll(Franja.class)).singleElement()
            .satisfies(f -> assertThat(f.getEstado()).isEqualTo(EstadoFranja.RESERVADA));
        mockMvc.perform(get(RUTA_HORARIOS).param("filter[activa]", "false").with(comoInstructor(instructorA)))
            .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get(RUTA_HORARIOS).param("filter[activa]", "true").with(comoInstructor(instructorA)))
            .andExpect(jsonPath("$.data.length()").value(0));

        accion(RUTA_HORARIOS + "/" + horario + "/desactivar", instructorA).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ERR_ACA_06"));

        // * CA14: otro horario en la misma hora; la franja reservada sigue ocupando su lugar (13 de 14)
        crearHorario(instructorA, LUNES_DE_OCHO_A_DOCE).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.franjasGeneradas").value(13));
    }

    @Test
    void desactivar_noTocaLasFranjasPasadas() throws Exception {
        String horario = crearLunesDeOchoADoce(instructorA);
        RELOJ.fijar(LocalDateTime.of(2026, 11, 2, 9, 30));

        accion(RUTA_HORARIOS + "/" + horario + "/desactivar", instructorA).andExpect(status().isOk());

        // * Las de las 10:00 y las 11:00 de hoy (2/11) ya estaban "futuras" y se borran; las de 08:00 y 09:00
        // * de hoy y todo lo anterior (26/10: 10:00 y 11:00) ya habían empezado y se conservan
        assertThat(mongoTemplate.findAll(Franja.class)).extracting(f -> f.getFecha().toString())
            .contains(HOY)
            .doesNotContain(DOS_LUNES_DESPUES);
    }

    // ---------------------------------------------------------------- generación diaria (CA16)

    @Test
    void generacionDiaria_esIdempotente_completaLoQueFalta_yIgnoraLosHorariosInactivos() throws Exception {
        String horario = crearLunesDeOchoADoce(instructorA);

        ResumenExtension mismaHora = generacion.extenderHorariosActivos();
        assertThat(mismaHora.horariosProcesados()).isEqualTo(1);
        assertThat(mismaHora.franjasCreadas()).isZero();
        assertThat(mongoTemplate.count(new Query(), Franja.class)).isEqualTo(14);

        // * Una semana después entra en la ventana el lunes 23/11 (4 franjas); el resto ya existía
        RELOJ.fijar(LocalDateTime.of(2026, 11, 2, 9, 30));
        ResumenExtension unaSemanaDespues = generacion.extenderHorariosActivos();
        assertThat(unaSemanaDespues.franjasCreadas()).isEqualTo(4);
        assertThat(generacion.extenderHorariosActivos().franjasCreadas()).isZero();

        accion(RUTA_HORARIOS + "/" + horario + "/desactivar", instructorA).andExpect(status().isOk());
        long antes = mongoTemplate.count(new Query(), Franja.class);
        RELOJ.fijar(LocalDateTime.of(2026, 11, 9, 9, 30));
        ResumenExtension inactivo = generacion.extenderHorariosActivos();

        assertThat(inactivo.horariosProcesados()).isZero();
        assertThat(inactivo.franjasCreadas()).isZero();
        assertThat(mongoTemplate.count(new Query(), Franja.class)).isEqualTo(antes);
    }

    @Test
    void generacionDiaria_noGeneraParaUnHorarioConLaVigenciaTerminada() throws Exception {
        crearHorario(instructorA, """
            {"diaSemana": 1, "horaInicio": "08:00", "horaFin": "12:00", "duracionClaseMin": 60,
             "vigenteDesde": "2026-10-26", "vigenteHasta": "2026-11-02"}""").andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.franjasGeneradas").value(6));
        RELOJ.fijar(LocalDateTime.of(2026, 11, 16, 9, 30));

        ResumenExtension resumen = generacion.extenderHorariosActivos();

        assertThat(resumen.horariosProcesados()).isZero();
        assertThat(resumen.franjasCreadas()).isZero();
    }

    // ---------------------------------------------------------------- seguridad (CA15)

    @Test
    void sinToken_cadaEndpointResponde401ErrSys04() throws Exception {
        mockMvc.perform(get(RUTA_HORARIOS)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
        mockMvc.perform(post(RUTA_HORARIOS).contentType(MediaType.APPLICATION_JSON).content(LUNES_DE_OCHO_A_DOCE))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA_HORARIOS + "/" + UUID.randomUUID() + "/desactivar"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA_FRANJAS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA_FRANJAS + "/" + UUID.randomUUID() + "/bloquear"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA_FRANJAS + "/" + UUID.randomUUID() + "/desbloquear"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unEstudiante_recibe403ErrSys05EnCadaEndpointYNoCreaNada() throws Exception {
        mockMvc.perform(get(RUTA_HORARIOS).with(comoEstudiante())).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(post(RUTA_HORARIOS).contentType(MediaType.APPLICATION_JSON).content(LUNES_DE_OCHO_A_DOCE)
                .with(comoEstudiante()))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        mockMvc.perform(post(RUTA_HORARIOS + "/" + UUID.randomUUID() + "/desactivar").with(comoEstudiante()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA_FRANJAS).with(comoEstudiante())).andExpect(status().isForbidden());
        mockMvc.perform(post(RUTA_FRANJAS + "/" + UUID.randomUUID() + "/bloquear").with(comoEstudiante()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post(RUTA_FRANJAS + "/" + UUID.randomUUID() + "/desbloquear").with(comoEstudiante()))
            .andExpect(status().isForbidden());

        assertThat(mongoTemplate.count(new Query(), PlantillaHorario.class)).isZero();
        assertThat(mongoTemplate.count(new Query(), Franja.class)).isZero();
    }

    @Test
    void entradaMalFormada_responde400EnVezDe500() throws Exception {
        mockMvc.perform(post(RUTA_HORARIOS).contentType(MediaType.APPLICATION_JSON).content("{roto")
                .with(comoInstructor(instructorA)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(get(RUTA_FRANJAS).param("filter[fecha]", "no-es-fecha").with(comoInstructor(instructorA)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(post(RUTA_FRANJAS + "/no-es-uuid/bloquear").with(comoInstructor(instructorA)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void laFechaDeHoyDeLaPrueba_esUnLunes() {
        // * Si esto falla, los números de arriba (14 franjas, etc.) dejan de tener sentido
        assertThat(LocalDate.parse(HOY).getDayOfWeek().getValue()).isEqualTo(1);
    }
}
