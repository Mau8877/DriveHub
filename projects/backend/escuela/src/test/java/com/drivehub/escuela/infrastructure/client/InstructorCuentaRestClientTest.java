package com.drivehub.escuela.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.drivehub.escuela.common.exception.BusinessException;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.gateway.DatosCuentaInstructor;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;
import com.drivehub.escuela.domain.gateway.PersonaInstructor;
import com.sun.net.httpserver.HttpServer;

/**
 * El cliente de instructores contra una Identidad falsa (HttpServer del JDK).
 */
class InstructorCuentaRestClientTest {

    private static final String CLAVE = "claveSecreta123";
    private static final String RUTA = "/api/v1/usuarios/instructores";

    private HttpServer identidadFalsa;
    private InstructorCuentaRestClient cliente;

    private record Peticion(String metodo, String rutaYConsulta, String autorizacion, String cuerpo) {
    }

    private record Respuesta(int estado, String json) {
    }

    // * Todo lo que recibió la Identidad falsa, en orden
    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();
    // * Las respuestas a las próximas peticiones; cuando se acaban, se repite la última configurada
    private final ConcurrentLinkedDeque<Respuesta> respuestas = new ConcurrentLinkedDeque<>();
    private volatile Respuesta ultimaRespuesta = new Respuesta(200, "{}");
    private volatile long demoraMs = 0;

    @BeforeEach
    void levantarIdentidadFalsa() throws IOException {
        identidadFalsa = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        identidadFalsa.createContext("/", exchange -> {
            peticiones.add(new Peticion(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Respuesta respuesta = respuestas.poll();
            if (respuesta == null) {
                respuesta = ultimaRespuesta;
            }
            byte[] bytes = respuesta.json().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(respuesta.estado(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream salida = exchange.getResponseBody()) {
                    salida.write(bytes);
                }
            }
            exchange.close();
        });
        identidadFalsa.start();
        cliente = new InstructorCuentaRestClient("http://localhost:" + identidadFalsa.getAddress().getPort(), 1);
        iniciarSesionComo("token-del-admin");
    }

    @AfterEach
    void apagar() {
        SecurityContextHolder.clearContext();
        identidadFalsa.stop(0);
    }

    private void iniciarSesionComo(String token) {
        Jwt jwt = Jwt.withTokenValue(token).header("alg", "none").claim("sub", UUID.randomUUID().toString())
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void responde(int estado, String json) {
        ultimaRespuesta = new Respuesta(estado, json);
    }

    private void respondeLuego(int estado, String json) {
        respuestas.add(new Respuesta(estado, json));
    }

    private static String error(String codigo, String mensaje) {
        return "{\"status\":\"failed\",\"data\":null,\"message\":\"" + mensaje + "\",\"error\":\"" + codigo + "\"}";
    }

    private static String persona(UUID codigo) {
        return "{\"codigo\":\"" + codigo + "\",\"nombres\":\"Ivan\",\"apellidos\":\"Pérez\","
            + "\"nombreCompleto\":\"Ivan Pérez\",\"fotoUrl\":null,\"edad\":36,\"fechaNacimiento\":\"1990-01-01\","
            + "\"sexo\":\"MASCULINO\",\"ci\":\"AB12345\",\"email\":\"ivan@gmail.com\",\"telefono\":null,"
            + "\"direccion\":\"Av. Arce\",\"latitud\":-17.7833,\"longitud\":-63.1821,\"fechaAlta\":\"2026-05-04\","
            + "\"passwordHash\":\"nunca\"}";
    }

    private static String sobreLista(List<UUID> codigos, int pagina, int totalPaginas) {
        return "{\"status\":\"success\",\"data\":["
            + String.join(",", codigos.stream().map(InstructorCuentaRestClientTest::persona).toList())
            + "],\"meta\":{\"page\":" + pagina + ",\"pageSize\":100,\"totalItems\":0,\"totalPages\":" + totalPaginas + "}}";
    }

    private static long cantidadDeCodigos(String cuerpo) {
        return java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            .matcher(cuerpo).results().count();
    }

    private static DatosCuentaInstructor datos(UUID codigo) {
        return new DatosCuentaInstructor(codigo, "Ivan", "Pérez", LocalDate.of(1990, 1, 1), "MASCULINO", "AB12345",
            null, "ivan@gmail.com", CLAVE, new BigDecimal("-17.7833"), new BigDecimal("-63.1821"));
    }

    @Test
    void crearCuenta_enviaElBearerYElCuerpoConElCodigoYDevuelveLaPersona() {
        UUID codigo = UUID.randomUUID();
        responde(201, "{\"status\":\"success\",\"data\":" + persona(codigo) + ",\"message\":\"ok\"}");

        PersonaInstructor creada = cliente.crearCuenta(datos(codigo));

        assertThat(creada.codigo()).isEqualTo(codigo);
        assertThat(creada.nombreCompleto()).isEqualTo("Ivan Pérez");
        assertThat(creada.fechaNacimiento()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(creada.fechaAlta()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(creada.latitud()).isEqualByComparingTo("-17.7833");
        Peticion peticion = peticiones.get(0);
        assertThat(peticion.metodo()).isEqualTo("POST");
        assertThat(peticion.rutaYConsulta()).isEqualTo(RUTA);
        assertThat(peticion.autorizacion()).isEqualTo("Bearer token-del-admin");
        assertThat(peticion.cuerpo()).contains("\"codigo\":\"" + codigo + "\"", "\"email\":\"ivan@gmail.com\"",
            "\"fechaNacimiento\":\"1990-01-01\"", "\"ci\":\"AB12345\"", "\"sexo\":\"MASCULINO\"", "\"latitud\":-17.7833")
            .doesNotContain("direccion");
    }

    @Test
    void crearCuenta_sinSesionConToken_noLlamaAIdentidad() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID()))).isInstanceOf(IllegalStateException.class);
        assertThat(peticiones).isEmpty();
    }

    @Test
    void crearCuenta_conLosRechazosDeNegocio_lanzaLaExcepcionEquivalenteConSuCodigoYMensaje() {
        responde(409, error("ERR_IDE_01", "Ya existe una cuenta con ese correo"));
        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID())))
            .isInstanceOfSatisfying(ConflictException.class, ex -> {
                assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_01");
                assertThat(ex.getMessage()).isEqualTo("Ya existe una cuenta con ese correo");
            });

        responde(409, error("ERR_IDE_19", "Ya existe una cuenta con ese CI"));
        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID())))
            .isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_19"));

        responde(400, error("ERR_IDE_02", "Debes tener al menos 18 años"));
        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID())))
            .isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_02"));
    }

    @Test
    void crearCuenta_conErrorDeServidorOTooManyRequests_lanzaIdentidadNoDisponibleYNoUnaExcepcionDeNegocio() {
        responde(500, error("ERR_SYS_00", "Ocurrió un error inesperado"));
        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID())))
            .isInstanceOf(IdentidadNoDisponibleException.class).isNotInstanceOf(BusinessException.class);

        responde(429, error("ERR_SYS_02", "Demasiadas peticiones"));
        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID())))
            .isInstanceOf(IdentidadNoDisponibleException.class);
    }

    @Test
    void crearCuenta_siIdentidadTardaMasQueElTiempoDeEsperaOEstaCaida_lanzaIdentidadNoDisponible() {
        responde(201, "{}");
        demoraMs = 2500;
        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID())))
            .isInstanceOf(IdentidadNoDisponibleException.class);

        identidadFalsa.stop(0);
        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID())))
            .isInstanceOf(IdentidadNoDisponibleException.class);
    }

    @Test
    void crearCuenta_ningunErrorNiMensajeNiTextoDeLosDatosContieneLaContrasena() {
        responde(500, error("ERR_SYS_00", "x"));

        assertThatThrownBy(() -> cliente.crearCuenta(datos(UUID.randomUUID()))).satisfies(ex -> {
            assertThat(String.valueOf(ex.getMessage())).doesNotContain(CLAVE);
            assertThat(String.valueOf(ex.getCause())).doesNotContain(CLAVE);
        });
        assertThat(datos(UUID.randomUUID()).toString()).doesNotContain(CLAVE, "AB12345", "ivan@gmail.com");
    }

    @Test
    void revertirCuenta_llamaADeleteConElCodigoYElBearer_yNoFallaSiYaNoExiste() {
        UUID codigo = UUID.randomUUID();
        responde(200, "{\"status\":\"success\",\"data\":null}");

        assertThatCode(() -> cliente.revertirCuenta(codigo)).doesNotThrowAnyException();

        Peticion peticion = peticiones.get(0);
        assertThat(peticion.metodo()).isEqualTo("DELETE");
        assertThat(peticion.rutaYConsulta()).isEqualTo(RUTA + "/" + codigo);
        assertThat(peticion.autorizacion()).isEqualTo("Bearer token-del-admin");
    }

    @Test
    void revertirCuenta_conUnaCuentaYaUsada_lanzaConflictErrIde23() {
        responde(409, error("ERR_IDE_23", "La cuenta ya se usó"));

        assertThatThrownBy(() -> cliente.revertirCuenta(UUID.randomUUID()))
            .isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_23"));
    }

    @Test
    void consultar_conListaVacia_noLlamaANadie() {
        assertThat(cliente.consultar(List.of())).isEmpty();
        assertThat(peticiones).isEmpty();
    }

    @Test
    void consultar_conDosCientosCincuentaCodigos_haceTresLlamadasYJuntaLosResultados() {
        List<UUID> codigos = IntStream.range(0, 250).mapToObj(i -> UUID.randomUUID()).toList();
        respondeLuego(200, "{\"status\":\"success\",\"data\":[" + persona(codigos.get(0)) + "]}");
        respondeLuego(200, "{\"status\":\"success\",\"data\":[" + persona(codigos.get(100)) + "]}");
        respondeLuego(200, "{\"status\":\"success\",\"data\":[" + persona(codigos.get(200)) + "]}");

        List<PersonaInstructor> personas = cliente.consultar(codigos);

        assertThat(personas).extracting(PersonaInstructor::codigo)
            .containsExactly(codigos.get(0), codigos.get(100), codigos.get(200));
        assertThat(peticiones).hasSize(3);
        assertThat(peticiones).allSatisfy(p -> {
            assertThat(p.metodo()).isEqualTo("POST");
            assertThat(p.rutaYConsulta()).isEqualTo(RUTA + "/consulta");
            assertThat(p.autorizacion()).isEqualTo("Bearer token-del-admin");
        });
        assertThat(cantidadDeCodigos(peticiones.get(0).cuerpo())).isEqualTo(100);
        assertThat(cantidadDeCodigos(peticiones.get(2).cuerpo())).isEqualTo(50);
    }

    @Test
    void buscarCodigos_recorreLasPaginasHastaAgotarlas() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        respondeLuego(200, sobreLista(List.of(a), 1, 2));
        respondeLuego(200, sobreLista(List.of(b), 2, 2));

        List<UUID> codigos = cliente.buscarCodigos("jose gomez");

        assertThat(codigos).containsExactly(a, b);
        assertThat(peticiones).hasSize(2);
        assertThat(peticiones.get(0).metodo()).isEqualTo("GET");
        assertThat(peticiones.get(0).rutaYConsulta()).contains("search=jose", "page=1", "pageSize=100");
        assertThat(peticiones.get(1).rutaYConsulta()).contains("page=2");
    }

    @Test
    void buscarCodigos_seDetieneEnLaQuintaPaginaAunqueHayaMas() {
        responde(200, sobreLista(List.of(UUID.randomUUID()), 1, 50));

        List<UUID> codigos = cliente.buscarCodigos("ivan");

        assertThat(peticiones).hasSize(5);
        assertThat(codigos).hasSize(5);
    }

    @Test
    void cambiarUbicacion_llamaAPatchYDevuelveLaPersona() {
        UUID codigo = UUID.randomUUID();
        responde(200, "{\"status\":\"success\",\"data\":" + persona(codigo) + "}");

        PersonaInstructor persona = cliente.cambiarUbicacion(codigo, new BigDecimal("-17.8"), new BigDecimal("-63.2"));

        assertThat(persona.codigo()).isEqualTo(codigo);
        Peticion peticion = peticiones.get(0);
        assertThat(peticion.metodo()).isEqualTo("PATCH");
        assertThat(peticion.rutaYConsulta()).isEqualTo(RUTA + "/" + codigo + "/ubicacion");
        assertThat(peticion.cuerpo()).contains("\"latitud\":-17.8", "\"longitud\":-63.2");
    }

    @Test
    void cambiarUbicacion_conUnInstructorQueNoExiste_lanzaNotFoundErrIde22() {
        responde(404, error("ERR_IDE_22", "El instructor no existe"));

        assertThatThrownBy(() -> cliente.cambiarUbicacion(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE))
            .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo("ERR_IDE_22"));
    }

    @Test
    void lasPeticionesSeGuardanEnOrdenSinMezclarseEntreLlamadas() {
        List<String> metodos = new ArrayList<>();
        responde(200, "{\"status\":\"success\",\"data\":null}");
        cliente.revertirCuenta(UUID.randomUUID());
        cliente.revertirCuenta(UUID.randomUUID());
        peticiones.forEach(p -> metodos.add(p.metodo()));

        assertThat(metodos).containsExactly("DELETE", "DELETE");
    }
}
