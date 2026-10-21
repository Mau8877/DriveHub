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
 * La primera barrera (CU02): sin token válido no se llega a los servicios.
 * Un Identidad falso publica el JWKS y responde a cualquier ruta que le llegue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayAuthTest {

    private static final HttpServer SERVICIO_FALSO = levantar();
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @LocalServerPort
    private int puerto;

    @DynamicPropertySource
    static void apuntarAlServicioFalso(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + SERVICIO_FALSO.getAddress().getPort();
        registry.add("IDENTIDAD_URL", () -> url);
        registry.add("ESCUELA_URL", () -> url);
        registry.add("app.auth.jwks-uri", () -> url + "/.well-known/jwks.json");
    }

    @AfterAll
    static void apagar() {
        SERVICIO_FALSO.stop(0);
    }

    @Test
    void rutaPrivadaSinToken_responde401ConErrSys04EnElSobre() throws Exception {
        HttpResponse<String> r = enviar("GET", "/api/v1/estudiantes", null);

        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(r.body()).contains("\"status\":\"failed\"").contains("\"error\":\"ERR_SYS_04\"");
    }

    @Test
    void rutaPrivadaConTokenValido_llegaAlServicioConLaMismaRuta() throws Exception {
        HttpResponse<String> r = enviar("GET", "/api/v1/estudiantes", TokensDePrueba.valido());

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).isEqualTo("servicio:/api/v1/estudiantes");
    }

    @Test
    void tokenVencidoDeOtroEmisorOMalFirmado_responde401() throws Exception {
        for (String token : new String[] {TokensDePrueba.vencido(), TokensDePrueba.deOtroEmisor(),
            TokensDePrueba.conOtraClave(), "esto-no-es-un-jwt"}) {
            HttpResponse<String> r = enviar("GET", "/api/v1/estudiantes", token);

            assertThat(r.statusCode()).isEqualTo(401);
            assertThat(r.body()).contains("ERR_SYS_04");
        }
    }

    @Test
    void rutasPublicas_llegan_sinToken() throws Exception {
        assertThat(enviar("POST", "/api/v1/auth/login", null).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/v1/auth/refresh", null).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/v1/auth/logout", null).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/v1/auth/verificar-correo/confirmar", null).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/v1/auth/recuperar", null).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/v1/auth/restablecer", null).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/v1/usuarios", null).statusCode()).isEqualTo(200);
        assertThat(enviar("GET", "/api/v1/escuelas/escuelaexample", null).statusCode()).isEqualTo(200);
    }

    @Test
    void pedirLaVerificacionDelCorreo_exigeToken_yConfirmarEsPublico() throws Exception {
        var sinToken = enviar("POST", "/api/v1/auth/verificar-correo", null);
        assertThat(sinToken.statusCode()).isEqualTo(401);
        assertThat(sinToken.body()).contains("ERR_SYS_04");
        assertThat(enviar("POST", "/api/v1/auth/verificar-correo", TokensDePrueba.valido()).statusCode()).isEqualTo(200);
    }

    @Test
    void escuelasPorCodigo_exigeToken() throws Exception {
        String ruta = "/api/v1/escuelas/por-codigo/0198f0a0-0000-7000-8000-000000000001";

        assertThat(enviar("GET", ruta, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", ruta, TokensDePrueba.valido()).statusCode()).isEqualTo(200);
    }

    // * CU05: solo la consulta por slug (registro del estudiante) es pública. El resto de `escuelas`, que
    // * comparte el recurso, y los endpoints de administradores de `usuarios` exigen token.
    @Test
    void gestionDeEscuelas_exigeTokenEnCadaEndpoint_yLaConsultaPorSlugSigueSiendoPublica() throws Exception {
        String codigo = "0198f0a0-0000-7000-8000-000000000001";
        String[][] endpoints = {
            {"GET", "/api/v1/escuelas"},
            {"POST", "/api/v1/escuelas"},
            {"PATCH", "/api/v1/escuelas/" + codigo},
            {"DELETE", "/api/v1/escuelas/" + codigo},
            {"POST", "/api/v1/escuelas/" + codigo + "/reactivar"},
        };
        for (String[] endpoint : endpoints) {
            HttpResponse<String> sinToken = enviar(endpoint[0], endpoint[1], null);
            assertThat(sinToken.statusCode()).as(endpoint[0] + " " + endpoint[1] + " sin token").isEqualTo(401);
            assertThat(sinToken.body()).contains("ERR_SYS_04");
            assertThat(enviar(endpoint[0], endpoint[1], TokensDePrueba.valido()).statusCode())
                .as(endpoint[0] + " " + endpoint[1] + " con token").isEqualTo(200);
        }
        assertThat(enviar("GET", "/api/v1/escuelas/escuelaexample", null).statusCode()).isEqualTo(200);
    }

    @Test
    void administradoresDeEscuela_exigenToken() throws Exception {
        String ruta = "/api/v1/usuarios/administradores-escuela";
        for (String metodo : new String[] {"POST", "DELETE"}) {
            assertThat(enviar(metodo, ruta, null).statusCode()).as(metodo + " sin token").isEqualTo(401);
            assertThat(enviar(metodo, ruta, TokensDePrueba.valido()).statusCode()).as(metodo + " con token").isEqualTo(200);
        }
        // * El registro del estudiante, en el mismo recurso, sigue siendo la única ruta pública de `usuarios`
        assertThat(enviar("POST", "/api/v1/usuarios", null).statusCode()).isEqualTo(200);
    }

    @Test
    void getDeUsuariosSinToken_responde401PorqueSoloElRegistroEsPublico() throws Exception {
        assertThat(enviar("GET", "/api/v1/usuarios/123", null).statusCode()).isEqualTo(401);
    }

    @Test
    void preflightCors_noPideToken() throws Exception {
        HttpRequest preflight = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/estudiantes"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .build();

        HttpResponse<String> r = CLIENTE.send(preflight, HttpResponse.BodyHandlers.ofString());

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:4200");
    }

    @Test
    void health_sigueAbiertoSinToken() throws Exception {
        assertThat(enviar("GET", "/health", null).statusCode()).isNotEqualTo(401);
    }

    private HttpResponse<String> enviar(String metodo, String ruta, String token) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .method(metodo, metodo.equals("POST") ? HttpRequest.BodyPublishers.ofString("{}") : HttpRequest.BodyPublishers.noBody());
        if (metodo.equals("POST")) {
            b.header("Content-Type", "application/json");
        }
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return CLIENTE.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer levantar() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", intercambio -> {
                String ruta = intercambio.getRequestURI().getPath();
                String texto = ruta.equals("/.well-known/jwks.json") ? TokensDePrueba.jwks() : "servicio:" + ruta;
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
