package com.drivehub.academico;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

import com.jayway.jsonpath.JsonPath;

/**
 * CU19 en Académico de punta a punta: controller, service con su @PreAuthorize, repositorio y MongoDB
 * real (replica set de un nodo). Cada método nombra el criterio de aceptación (CAxx).
 *
 * * El container se maneja a mano para poder saltear el test si Docker no está disponible
 * * (ver TESTING_BACKEND.md#testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
class GestionPreguntasIntegrationTest {

    private static final String RUTA = "/api/v1/preguntas";
    private static final UUID ESCUELA_A = UUID.randomUUID();
    private static final UUID ESCUELA_B = UUID.randomUUID();

    private static MongoDBContainer mongo;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void levantarMongoSiHayDocker() {
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible, "Docker no está disponible: se salta el test con base real");
        mongo = new MongoDBContainer("mongo:8");
        mongo.start();
    }

    @AfterAll
    static void detenerMongo() {
        if (mongo != null) {
            mongo.stop();
        }
    }

    @DynamicPropertySource
    static void propiedadesDeBaseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> mongo.getReplicaSetUrl("academico"));
    }

    private static RequestPostProcessor admin(UUID escuela) {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("escuela", escuela.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_ESCUELA"));
    }

    private static String cuerpo(String enunciado, String tema, String categoria, String tipo, String opciones, int dificultad) {
        return """
            {"categoria":"%s","tema":"%s","tipo":"%s","enunciado":"%s","opciones":%s,"explicacion":"Porque sí","dificultad":%d}
            """.formatted(categoria, tema, tipo, enunciado, opciones, dificultad);
    }

    private static final String UNICA = "[{\"texto\":\"Uno\",\"correcta\":true},{\"texto\":\"Dos\",\"correcta\":false}]";

    private ResultActions crear(RequestPostProcessor sesion, String json) throws Exception {
        return mockMvc.perform(post(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private UUID crearYObtenerCodigo(UUID escuela, String enunciado, String tema) throws Exception {
        String respuesta = crear(admin(escuela), cuerpo(enunciado, tema, "GENERAL", "OPCION_UNICA", UNICA, 1))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(respuesta, "$.data.codigo"));
    }

    @Test
    void ca10_sinSesionResponde401YConOtroRolResponde403() throws Exception {
        UUID codigo = UUID.randomUUID();
        for (String rol : new String[] {"SUPER_ADMIN", "INSTRUCTOR", "ESTUDIANTE"}) {
            RequestPostProcessor sesion = jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
                .claim("escuela", ESCUELA_A.toString())).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
            crear(sesion, cuerpo("Enunciado de prueba", "Tema", "GENERAL", "OPCION_UNICA", UNICA, 1))
                .andExpect(status().isForbidden());
            mockMvc.perform(get(RUTA).with(sesion)).andExpect(status().isForbidden());
            mockMvc.perform(post(RUTA + "/" + codigo + "/desactivar").with(sesion)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void ca02_ca04_creaConIdsPorOrdenYRechazaOpcionesInvalidas() throws Exception {
        crear(admin(ESCUELA_A), cuerpo("¿Cuál es correcta?", "Reglas", "B", "OPCION_MULTIPLE",
            "[{\"texto\":\"Sí\",\"correcta\":true},{\"texto\":\"También\",\"correcta\":true},{\"texto\":\"No\",\"correcta\":false}]", 2))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.activa").value(true))
            .andExpect(jsonPath("$.data.opciones[0].id").value("A"))
            .andExpect(jsonPath("$.data.opciones[2].id").value("C"))
            .andExpect(jsonPath("$.data.creadoEn").exists());

        crear(admin(ESCUELA_A), cuerpo("Sin ninguna correcta", "Reglas", "B", "OPCION_UNICA",
            "[{\"texto\":\"Uno\",\"correcta\":false},{\"texto\":\"Dos\",\"correcta\":false}]", 1))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        crear(admin(ESCUELA_A), cuerpo("Verdadero falso con tres", "Reglas", "B", "VERDADERO_FALSO",
            "[{\"texto\":\"V\",\"correcta\":true},{\"texto\":\"F\",\"correcta\":false},{\"texto\":\"Q\",\"correcta\":false}]", 1))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void ca01_ca09_cadaEscuelaVeYTocaSoloSuBanco() throws Exception {
        UUID deA = crearYObtenerCodigo(ESCUELA_A, "Pregunta exclusiva de A", "Aislamiento");
        crearYObtenerCodigo(ESCUELA_B, "Pregunta exclusiva de B", "Aislamiento");

        mockMvc.perform(get(RUTA).with(admin(ESCUELA_A)).param("filter[tema]", "Aislamiento"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].enunciado").value("Pregunta exclusiva de A"))
            .andExpect(jsonPath("$.meta.totalItems").value(1));

        // * Una pregunta de otra escuela es, para quien la pide, una que no existe
        mockMvc.perform(get(RUTA + "/" + deA).with(admin(ESCUELA_B)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_ACA_14"));
        mockMvc.perform(patch(RUTA + "/" + deA).with(admin(ESCUELA_B)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tema\":\"Hackeado\"}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post(RUTA + "/" + deA + "/desactivar").with(admin(ESCUELA_B)))
            .andExpect(status().isNotFound());
        mockMvc.perform(get(RUTA + "/" + deA).with(admin(ESCUELA_A)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.activa").value(true));
    }

    @Test
    void ca01_elListadoBuscaFiltraYPagina() throws Exception {
        UUID escuela = UUID.randomUUID();
        crear(admin(escuela), cuerpo("Alfa señal de PARE", "Señales", "GENERAL", "OPCION_UNICA", UNICA, 1)).andExpect(status().isCreated());
        crear(admin(escuela), cuerpo("Beta prioridad", "Prioridad", "B", "OPCION_UNICA", UNICA, 2)).andExpect(status().isCreated());
        UUID tercera = crearYObtenerCodigo(escuela, "Gamma verdadero", "Prioridad");
        mockMvc.perform(post(RUTA + "/" + tercera + "/desactivar").with(admin(escuela))).andExpect(status().isOk());

        mockMvc.perform(get(RUTA).with(admin(escuela)).param("search", "pare"))
            .andExpect(jsonPath("$.meta.totalItems").value(1));
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("search", "PRIORIDAD"))
            .andExpect(jsonPath("$.meta.totalItems").value(2));
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("search", ".*"))
            .andExpect(jsonPath("$.meta.totalItems").value(0));
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("filter[categoria]", "B"))
            .andExpect(jsonPath("$.meta.totalItems").value(1));
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("filter[activa]", "false"))
            .andExpect(jsonPath("$.data[0].enunciado").value("Gamma verdadero"));
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("filter[dificultad]", "2").param("filter[tipo]", "OPCION_UNICA"))
            .andExpect(jsonPath("$.meta.totalItems").value(1));
        // * Más reciente primero y paginado
        mockMvc.perform(get(RUTA).with(admin(escuela)).param("pageSize", "2"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].enunciado").value("Gamma verdadero"))
            .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void ca05_editaSoloLoEnviadoYRevalidaElTipo() throws Exception {
        UUID codigo = crearYObtenerCodigo(ESCUELA_A, "Pregunta para editar", "Edicion");

        mockMvc.perform(patch(RUTA + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tema\":\"Edición\",\"dificultad\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tema").value("Edición"))
            .andExpect(jsonPath("$.data.dificultad").value(3))
            .andExpect(jsonPath("$.data.enunciado").value("Pregunta para editar"))
            .andExpect(jsonPath("$.data.opciones.length()").value(2));
        mockMvc.perform(patch(RUTA + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"opciones\":[{\"texto\":\"X\",\"correcta\":true},{\"texto\":\"Y\",\"correcta\":true}]}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(patch(RUTA + "/" + codigo).with(admin(ESCUELA_A)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"VERDADERO_FALSO\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.tipo").value("VERDADERO_FALSO"));
    }

    @Test
    void ca06_ca07_desactivaYActivaYRechazaRepetir() throws Exception {
        UUID codigo = crearYObtenerCodigo(ESCUELA_A, "Pregunta para desactivar", "Estado");

        mockMvc.perform(post(RUTA + "/" + codigo + "/activar").with(admin(ESCUELA_A)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACA_15"));
        mockMvc.perform(post(RUTA + "/" + codigo + "/desactivar").with(admin(ESCUELA_A)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.activa").value(false));
        mockMvc.perform(post(RUTA + "/" + codigo + "/desactivar").with(admin(ESCUELA_A)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ERR_ACA_15"));
        mockMvc.perform(post(RUTA + "/" + codigo + "/activar").with(admin(ESCUELA_A)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.activa").value(true));
    }
}
