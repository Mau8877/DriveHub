package com.drivehub.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpServer;

/**
 * Prueba de punta a punta del enrutamiento: un HttpServer del JDK hace de
 * microservicio falso (Identidad y Escuela) y se verifica que el gateway le reenvíe las
 * requests de sus recursos (y solo esas), y que el preflight CORS lo
 * resuelva el propio gateway.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    // * Las rutas que recibió cada servicio falso: sirve para probar que algo NO llegó
    private static final java.util.Map<String, java.util.List<String>> RECIBIDAS =
        new java.util.concurrent.ConcurrentHashMap<>();
    private static final HttpServer IDENTIDAD_FALSA = levantarServicioFalso("identidad");
    private static final HttpServer ESCUELA_FALSA = levantarServicioFalso("escuela");
    private static final HttpServer PAGOS_FALSO = levantarServicioFalso("pagos");
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @LocalServerPort
    private int puerto;

    @DynamicPropertySource
    static void apuntarAIdentidadFalsa(DynamicPropertyRegistry registry) {
        registry.add("app.auth.jwks-uri", () -> "http://localhost:" + IDENTIDAD_FALSA.getAddress().getPort() + "/.well-known/jwks.json");
        registry.add("IDENTIDAD_URL", () -> "http://localhost:" + IDENTIDAD_FALSA.getAddress().getPort());
        registry.add("ESCUELA_URL", () -> "http://localhost:" + ESCUELA_FALSA.getAddress().getPort());
        registry.add("PAGOS_URL", () -> "http://localhost:" + PAGOS_FALSO.getAddress().getPort());
    }

    @AfterAll
    static void apagarIdentidadFalsa() {
        IDENTIDAD_FALSA.stop(0);
        ESCUELA_FALSA.stop(0);
        PAGOS_FALSO.stop(0);
    }

    @Test
    void recursoDeIdentidad_seReenviaAlServicioConLaMismaRuta() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/usuarios/123", TokensDePrueba.valido());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo("identidad:/api/v1/usuarios/123");
    }

    @Test
    void recursoEscuelas_seReenviaAEscuelaConLaMismaRuta() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/escuelas/escuelaexample");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo("escuela:/api/v1/escuelas/escuelaexample");
    }

    // * CU08: el estudiante es una cuenta con su perfil, así que el recurso lo atiende Identidad, no Escuela
    @Test
    void recursoEstudiantes_seReenviaAIdentidadYNoAEscuela() throws Exception {
        HttpResponse<String> lista = get("/api/v1/estudiantes", TokensDePrueba.valido());
        HttpResponse<String> detalle = get("/api/v1/estudiantes/0198f0a0-0000-7000-8000-000000000001", TokensDePrueba.valido());

        assertThat(lista.statusCode()).isEqualTo(200);
        assertThat(lista.body()).isEqualTo("identidad:/api/v1/estudiantes");
        assertThat(detalle.body()).isEqualTo("identidad:/api/v1/estudiantes/0198f0a0-0000-7000-8000-000000000001");
    }

    // * CU09: el instructor tiene la ficha en Escuela (recurso `instructores`) y la cuenta en Identidad
    @Test
    void recursoInstructores_seReenviaAEscuelaYNoAIdentidad() throws Exception {
        HttpResponse<String> lista = get("/api/v1/instructores", TokensDePrueba.valido());
        HttpResponse<String> detalle = get("/api/v1/instructores/0198f0a0-0000-7000-8000-000000000001", TokensDePrueba.valido());

        assertThat(lista.statusCode()).isEqualTo(200);
        assertThat(lista.body()).isEqualTo("escuela:/api/v1/instructores");
        assertThat(detalle.body()).isEqualTo("escuela:/api/v1/instructores/0198f0a0-0000-7000-8000-000000000001");
    }

    @Test
    void cuentasDeInstructores_seReenvianAIdentidadYNoAEscuela() throws Exception {
        HttpResponse<String> busqueda = get("/api/v1/usuarios/instructores", TokensDePrueba.valido());
        HttpResponse<String> consulta = post("/api/v1/usuarios/instructores/consulta", TokensDePrueba.valido());

        assertThat(busqueda.statusCode()).isEqualTo(200);
        assertThat(busqueda.body()).isEqualTo("identidad:/api/v1/usuarios/instructores");
        assertThat(consulta.body()).isEqualTo("identidad:/api/v1/usuarios/instructores/consulta");
    }

    @Test
    void recursoCategoriasLicencia_sigueEnEscuela() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/categorias-licencia", TokensDePrueba.valido());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo("escuela:/api/v1/categorias-licencia");
    }

    // * CU25 y CU26: los planes y los pagos los atiende Pagos
    @Test
    void recursosDePagos_seReenvianAPagosConLaMismaRuta() throws Exception {
        HttpResponse<String> disponibles = get("/api/v1/planes-pago/disponibles", TokensDePrueba.valido());
        HttpResponse<String> compra = post("/api/v1/pagos", TokensDePrueba.valido());
        HttpResponse<String> estado = get("/api/v1/pagos/0198f0a0-0000-7000-8000-000000000001/estado",
            TokensDePrueba.valido());
        HttpResponse<String> retorno = get("/api/v1/pagos/retorno");

        assertThat(disponibles.body()).isEqualTo("pagos:/api/v1/planes-pago/disponibles");
        assertThat(compra.body()).isEqualTo("pagos:/api/v1/pagos");
        assertThat(estado.body()).isEqualTo("pagos:/api/v1/pagos/0198f0a0-0000-7000-8000-000000000001/estado");
        assertThat(retorno.body()).isEqualTo("pagos:/api/v1/pagos/retorno");
    }

    // * CU27: la billetera de horas y los avisos de la pasarela tambien los atiende Pagos
    @Test
    void billeteraYAvisosDeLaPasarela_seReenvianAPagosConLaMismaRuta() throws Exception {
        HttpResponse<String> mia = get("/api/v1/billeteras-horas/mia", TokensDePrueba.valido());
        HttpResponse<String> consumo = post("/api/v1/billeteras-horas/consumos", TokensDePrueba.valido());
        HttpResponse<String> aviso = CLIENTE.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + puerto + "/api/v1/webhooks-stripe"))
            .header("Content-Type", "application/json").header("Stripe-Signature", "t=1,v1=x")
            .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());

        assertThat(mia.body()).isEqualTo("pagos:/api/v1/billeteras-horas/mia");
        assertThat(consumo.body()).isEqualTo("pagos:/api/v1/billeteras-horas/consumos");
        assertThat(aviso.body()).isEqualTo("pagos:/api/v1/webhooks-stripe");
    }

    // * CU29: las horas de los alumnos de un instructor tambien las atiende Pagos
    @Test
    void horasDeLosAlumnos_seReenvianAPagosConLaMismaRuta() throws Exception {
        HttpResponse<String> lista = get("/api/v1/horas-alumnos", TokensDePrueba.valido());
        HttpResponse<String> detalle = get("/api/v1/horas-alumnos/0198f0a0-0000-7000-8000-000000000001",
            TokensDePrueba.valido());

        assertThat(lista.body()).isEqualTo("pagos:/api/v1/horas-alumnos");
        assertThat(detalle.body()).isEqualTo("pagos:/api/v1/horas-alumnos/0198f0a0-0000-7000-8000-000000000001");
    }

    // * CU26: no hay cuotas ni inscripciones a un plan; Pagos ya no las tiene y el gateway no las enruta
    @Test
    void cuotasEInscripcionesAUnPlan_yaNoSeEnrutan() throws Exception {
        HttpResponse<String> cuotas = get("/api/v1/cuotas", TokensDePrueba.valido());
        HttpResponse<String> inscripciones = get("/api/v1/inscripciones-plan", TokensDePrueba.valido());

        assertThat(cuotas.statusCode()).isEqualTo(404);
        assertThat(inscripciones.statusCode()).isEqualTo(404);
        assertThat(cuotas.body()).doesNotContain("pagos:");
        assertThat(inscripciones.body()).doesNotContain("pagos:");
    }

    @Test
    void rutaQueNoEsDeNingunServicio_devuelve404SinTocarIdentidad() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/recurso-inexistente", TokensDePrueba.valido());

        assertThat(respuesta.statusCode()).isEqualTo(404);
        assertThat(respuesta.body()).doesNotContain("identidad:");
    }

    // * CU44: las rutas internas de Pagos (descontar y devolver horas con la credencial de servicio) NUNCA se enrutan.
    // * Si alguien registra `/internal/**` en un predicado `Path=`, estos tests se ponen en rojo.
    @Test
    void rutasInternasDePagos_noSeReenvianANingunServicio_conSesion() throws Exception {
        RECIBIDAS.values().forEach(java.util.List::clear);

        HttpResponse<String> consumos = post("/internal/v1/billeteras-horas/consumos", TokensDePrueba.valido());
        HttpResponse<String> devoluciones = post("/internal/v1/billeteras-horas/devoluciones", TokensDePrueba.valido());

        assertThat(consumos.statusCode()).isEqualTo(404);
        assertThat(devoluciones.statusCode()).isEqualTo(404);
        assertThat(RECIBIDAS.values()).allSatisfy(rutas -> assertThat(rutas).noneMatch(r -> r.contains("internal")));
    }

    @Test
    void rutasInternasDePagos_sinSesionOConOtrosPrefijos_tampocoLlegan() throws Exception {
        RECIBIDAS.values().forEach(java.util.List::clear);

        HttpResponse<String> sinSesion = post("/internal/v1/billeteras-horas/consumos", null);
        HttpResponse<String> prefijoApi = post("/api/v1/internal/v1/billeteras-horas/consumos",
            TokensDePrueba.valido());
        HttpResponse<String> conCredencial = postConCredencial("/internal/v1/billeteras-horas/consumos");

        assertThat(sinSesion.statusCode()).isIn(401, 404);
        assertThat(prefijoApi.statusCode()).isEqualTo(404);
        assertThat(conCredencial.statusCode()).isIn(401, 404);
        assertThat(RECIBIDAS.values()).allSatisfy(rutas -> assertThat(rutas).noneMatch(r -> r.contains("internal")));
    }

    @Test
    void preflightDesdeElShell_loRespondeElGatewayConCabecerasCors() throws Exception {
        HttpRequest preflight = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/usuarios"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .build();

        HttpResponse<String> respuesta = CLIENTE.send(preflight, HttpResponse.BodyHandlers.ofString());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:4200");
    }

    private HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
        return get(ruta, null);
    }

    private HttpResponse<String> get(String ruta, String token) throws IOException, InterruptedException {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET();
        if (token != null) {
            constructor.header("Authorization", "Bearer " + token);
        }
        HttpRequest request = constructor.build();
        return CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String ruta, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .headers(token == null ? new String[] {"Content-Type", "application/json"}
                : new String[] {"Authorization", "Bearer " + token, "Content-Type", "application/json"})
            .POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        return CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postConCredencial(String ruta) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .header("X-Service-Key", "una-credencial").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        return CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer levantarServicioFalso(String nombre) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", intercambio -> {
                String ruta = intercambio.getRequestURI().getPath();
                RECIBIDAS.computeIfAbsent(nombre, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(ruta);
                // * Identidad también publica las claves con las que el gateway verifica los tokens
                String texto = ruta.equals("/.well-known/jwks.json") ? TokensDePrueba.jwks() : nombre + ":" + ruta;
                byte[] cuerpo = texto.getBytes(StandardCharsets.UTF_8);
                intercambio.sendResponseHeaders(200, cuerpo.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(cuerpo);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar el servicio falso", e);
        }
    }
}
