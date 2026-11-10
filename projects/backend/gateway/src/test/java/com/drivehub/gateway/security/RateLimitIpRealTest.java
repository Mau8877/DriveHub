package com.drivehub.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * El rate limit identifica al cliente por su IP real cuando el gateway está detrás de un proxy (el ingress de
 * Kubernetes, nginx). Corre contra el servidor real: la IP la resuelve Tomcat con X-Forwarded-For, algo que MockMvc
 * no ejecuta. Las requests salen de 127.0.0.1, que Tomcat trata como proxy interno, igual que al ingress.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.ratelimit.capacity=1",
            "app.ratelimit.refill-per-minute=1",
            // * Puerto 1: nadie escucha. Alcanza con que la request pase el filtro (responde 503, no 429).
            "ESCUELA_URL=http://localhost:1"
        })
class RateLimitIpRealTest {

    private static final HttpClient CLIENTE = HttpClient.newHttpClient();
    private static final String RUTA_PUBLICA = "/api/v1/escuelas/escuelaexample";

    @LocalServerPort
    private int puerto;

    @Test
    void clientesDetrasDelProxy_tienenCuposIndependientesSegunSuIp() throws Exception {
        assertThat(get("203.0.113.40").statusCode()).isNotEqualTo(429);
        assertThat(get("203.0.113.40").statusCode()).isEqualTo(429);

        assertThat(get("203.0.113.41").statusCode()).isNotEqualTo(429);
    }

    @Test
    void ipFalsificadaPorElCliente_noLeDaUnCupoNuevo() throws Exception {
        // * El proxy agrega la IP real al final de lo que mandó el cliente: la que cuenta es esa, no la inventada.
        assertThat(get("198.51.100.1, 203.0.113.50").statusCode()).isNotEqualTo(429);
        assertThat(get("198.51.100.2, 203.0.113.50").statusCode()).isEqualTo(429);
    }

    private HttpResponse<String> get(String xForwardedFor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + RUTA_PUBLICA))
                .header("X-Forwarded-For", xForwardedFor)
                .GET()
                .build();
        return CLIENTE.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
