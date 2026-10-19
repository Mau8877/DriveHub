package com.drivehub.identidad.infrastructure.adapter.out.escuela;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.drivehub.identidad.domain.port.out.EscuelaRef;
import com.sun.net.httpserver.HttpServer;

/**
 * El adaptador contra un Escuela falso (HttpServer del JDK).
 */
class EscuelaRestAdapterTest {

    private static final UUID CODIGO = UUID.randomUUID();
    private static final HttpServer ESCUELA_FALSA = levantarEscuelaFalsa();

    private final EscuelaRestAdapter adaptador =
        new EscuelaRestAdapter("http://localhost:" + ESCUELA_FALSA.getAddress().getPort());

    @AfterAll
    static void apagar() {
        ESCUELA_FALSA.stop(0);
    }

    @Test
    void buscarActivaPorSlug_conEscuelaExistente_devuelveSusDatos() {
        assertThat(adaptador.buscarActivaPorSlug("escuelaexample")).hasValueSatisfying(escuela -> {
            assertThat(escuela).isEqualTo(new EscuelaRef(CODIGO, "escuelaexample", "Escuela Example"));
        });
    }

    @Test
    void buscarActivaPorSlug_cuandoEscuelaRespondeNotFound_devuelveVacio() {
        assertThat(adaptador.buscarActivaPorSlug("no-existe")).isEmpty();
    }

    @Test
    void buscarActivaPorSlug_cuandoEscuelaFalla_propagaLaExcepcion() {
        assertThatThrownBy(() -> adaptador.buscarActivaPorSlug("rota")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void buscarPorCodigo_conEscuelaSuspendida_laDevuelveConActivaFalsa() {
        assertThat(adaptador.buscarPorCodigo(CODIGO)).hasValueSatisfying(escuela -> {
            assertThat(escuela.codigo()).isEqualTo(CODIGO);
            assertThat(escuela.activa()).isFalse();
        });
    }

    @Test
    void buscarPorCodigo_conCodigoInexistente_devuelveVacio() {
        assertThat(adaptador.buscarPorCodigo(UUID.randomUUID())).isEmpty();
    }

    private static HttpServer levantarEscuelaFalsa() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v1/escuelas/", intercambio -> {
                String slug = intercambio.getRequestURI().getPath().substring("/api/v1/escuelas/".length());
                int estado;
                String cuerpo;
                if (slug.equals("por-codigo/" + CODIGO)) {
                    estado = 200;
                    cuerpo = "{\"status\":\"success\",\"data\":{\"codigo\":\"" + CODIGO
                        + "\",\"slug\":\"escuelaexample\",\"nombre\":\"Escuela Example\",\"activa\":false},"
                        + "\"message\":\"ok\",\"timestamp\":\"2026-10-19T00:00:00Z\"}";
                } else if (slug.startsWith("por-codigo/") && slug.endsWith("-error")) {
                    estado = 500;
                    cuerpo = "{\"status\":\"failed\",\"error\":\"ERR_SYS_00\"}";
                } else if (slug.startsWith("por-codigo/")) {
                    estado = 404;
                    cuerpo = "{\"status\":\"failed\",\"data\":null,\"message\":\"no existe\","
                        + "\"timestamp\":\"2026-10-19T00:00:00Z\",\"error\":\"ERR_ESC_02\"}";
                } else if ("escuelaexample".equals(slug)) {
                    estado = 200;
                    cuerpo = "{\"status\":\"success\",\"data\":{\"codigo\":\"" + CODIGO
                        + "\",\"slug\":\"escuelaexample\",\"nombre\":\"Escuela Example\"},"
                        + "\"message\":\"ok\",\"timestamp\":\"2026-10-19T00:00:00Z\"}";
                } else if ("no-existe".equals(slug)) {
                    estado = 404;
                    cuerpo = "{\"status\":\"failed\",\"data\":null,\"message\":\"no existe\","
                        + "\"timestamp\":\"2026-10-19T00:00:00Z\",\"error\":\"ERR_ESC_01\"}";
                } else {
                    estado = 500;
                    cuerpo = "{\"status\":\"failed\",\"error\":\"ERR_SYS_00\"}";
                }
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
}
