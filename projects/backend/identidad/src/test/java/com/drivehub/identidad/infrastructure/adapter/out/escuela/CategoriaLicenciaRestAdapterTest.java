package com.drivehub.identidad.infrastructure.adapter.out.escuela;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.drivehub.identidad.domain.port.out.EscuelaNoDisponibleException;
import com.sun.net.httpserver.HttpServer;

/**
 * El adaptador contra una Escuela falsa (HttpServer del JDK). Lo que la Escuela falsa recibe
 * queda en {@link #PETICIONES}, para comprobar la URL y el Bearer reenviado.
 */
class CategoriaLicenciaRestAdapterTest {

    private static final String BEARER = "token-del-administrador";
    private static final List<String> PETICIONES = new CopyOnWriteArrayList<>();
    private static final HttpServer ESCUELA_FALSA = levantarEscuelaFalsa();

    private final CategoriaLicenciaRestAdapter adaptador =
        new CategoriaLicenciaRestAdapter("http://localhost:" + ESCUELA_FALSA.getAddress().getPort());

    @AfterAll
    static void apagar() {
        ESCUELA_FALSA.stop(0);
    }

    @BeforeEach
    void iniciarSesion() {
        PETICIONES.clear();
        Jwt jwt = Jwt.withTokenValue(BEARER).header("alg", "none").claim("sub", "admin").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void cerrarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void existe_cuandoEscuelaResponde200_devuelveTrueYReenviaElBearer() {
        assertThat(adaptador.existe("B")).isTrue();

        assertThat(PETICIONES).containsExactly("/api/v1/categorias-licencia/B|Bearer " + BEARER);
    }

    @Test
    void existe_cuandoEscuelaResponde404_devuelveFalse() {
        assertThat(adaptador.existe("ZZZ")).isFalse();
    }

    @Test
    void existe_conSinCategoria_noLlamaANadie() {
        assertThat(adaptador.existe("SIN")).isTrue();
        assertThat(adaptador.existe("sin")).isTrue();

        assertThat(PETICIONES).isEmpty();
    }

    @Test
    void existe_cuandoEscuelaResponde500_lanzaEscuelaNoDisponible() {
        assertThatThrownBy(() -> adaptador.existe("ROTA")).isInstanceOf(EscuelaNoDisponibleException.class);
    }

    @Test
    void existe_cuandoEscuelaRechazaElToken_lanzaEscuelaNoDisponible() {
        assertThatThrownBy(() -> adaptador.existe("PROHIBIDA")).isInstanceOf(EscuelaNoDisponibleException.class);
    }

    @Test
    void existe_cuandoEscuelaNoContesta_lanzaEscuelaNoDisponible() {
        assertThatThrownBy(() -> adaptador.existe("LENTA")).isInstanceOf(EscuelaNoDisponibleException.class);
    }

    @Test
    void existe_cuandoEscuelaEstaCaida_lanzaEscuelaNoDisponible() {
        CategoriaLicenciaRestAdapter sinServidor = new CategoriaLicenciaRestAdapter("http://localhost:1");

        assertThatThrownBy(() -> sinServidor.existe("B")).isInstanceOf(EscuelaNoDisponibleException.class);
    }

    @Test
    void existe_codificaLaSiglaEnLaUrl() {
        adaptador.existe("A B/ñ");

        assertThat(PETICIONES).hasSize(1);
        assertThat(PETICIONES.get(0)).startsWith("/api/v1/categorias-licencia/A%20B%2F%C3%B1|");
    }

    @Test
    void existe_sinSesion_lanzaEscuelaNoDisponibleSinLlamarANadie() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> adaptador.existe("B")).isInstanceOf(EscuelaNoDisponibleException.class);
        assertThat(PETICIONES).isEmpty();
    }

    private static HttpServer levantarEscuelaFalsa() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/api/v1/categorias-licencia/", intercambio -> {
                String ruta = intercambio.getRequestURI().getRawPath();
                PETICIONES.add(ruta + "|" + intercambio.getRequestHeaders().getFirst("Authorization"));
                String sigla = ruta.substring("/api/v1/categorias-licencia/".length());
                int estado = switch (sigla) {
                    case "B" -> 200;
                    case "ROTA" -> 500;
                    case "PROHIBIDA" -> 403;
                    case "LENTA" -> dormir();
                    default -> 404;
                };
                String cuerpo = estado == 200
                    ? "{\"status\":\"success\",\"data\":{\"sigla\":\"B\",\"descripcion\":\"Profesional B\"}}"
                    : "{\"status\":\"failed\",\"error\":\"ERR_ESC_07\"}";
                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(estado, bytes.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar la Escuela falsa", e);
        }
    }

    // * Más que el tiempo de lectura del adaptador (5 s): la respuesta llega tarde
    private static int dormir() {
        try {
            Thread.sleep(6_500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 200;
    }
}
