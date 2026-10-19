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

    private static final HttpServer IDENTIDAD_FALSA = levantarServicioFalso("identidad");
    private static final HttpServer ESCUELA_FALSA = levantarServicioFalso("escuela");
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @LocalServerPort
    private int puerto;

    @DynamicPropertySource
    static void apuntarAIdentidadFalsa(DynamicPropertyRegistry registry) {
        registry.add("app.auth.jwks-uri", () -> "http://localhost:" + IDENTIDAD_FALSA.getAddress().getPort() + "/.well-known/jwks.json");
        registry.add("IDENTIDAD_URL", () -> "http://localhost:" + IDENTIDAD_FALSA.getAddress().getPort());
        registry.add("ESCUELA_URL", () -> "http://localhost:" + ESCUELA_FALSA.getAddress().getPort());
    }

    @AfterAll
    static void apagarIdentidadFalsa() {
        IDENTIDAD_FALSA.stop(0);
        ESCUELA_FALSA.stop(0);
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

    @Test
    void rutaQueNoEsDeNingunServicio_devuelve404SinTocarIdentidad() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/recurso-inexistente", TokensDePrueba.valido());

        assertThat(respuesta.statusCode()).isEqualTo(404);
        assertThat(respuesta.body()).doesNotContain("identidad:");
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

    private static HttpServer levantarServicioFalso(String nombre) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", intercambio -> {
                String ruta = intercambio.getRequestURI().getPath();
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
