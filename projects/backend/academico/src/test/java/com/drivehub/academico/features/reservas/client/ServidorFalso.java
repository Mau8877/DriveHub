package com.drivehub.academico.features.reservas.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Un servidor HTTP mínimo (el `HttpServer` del JDK) para probar los clientes de otros servicios sin red real.
 *
 * * Guarda cada petición que recibe; la respuesta la decide el test.
 */
public final class ServidorFalso implements AutoCloseable {

    /** Lo que llegó: método, ruta, cabeceras que interesan y cuerpo. */
    public record Peticion(String metodo, String ruta, String authorization, String claveIdempotente, String cuerpo,
            String credencialServicio) {
    }

    /** Lo que se responde. */
    public record Respuesta(int estado, String cuerpo) {
    }

    private final HttpServer servidor;
    private final List<Peticion> peticiones = new ArrayList<>();
    private final AtomicInteger contador = new AtomicInteger();
    private volatile Function<Peticion, Respuesta> manejador = p -> new Respuesta(404, "{}");

    public ServidorFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public void responder(Function<Peticion, Respuesta> nuevo) {
        this.manejador = nuevo;
    }

    public synchronized List<Peticion> peticiones() {
        return List.copyOf(peticiones);
    }

    public int cantidad() {
        return contador.get();
    }

    private void atender(HttpExchange intercambio) throws IOException {
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Peticion peticion = new Peticion(intercambio.getRequestMethod(), intercambio.getRequestURI().getPath(),
            intercambio.getRequestHeaders().getFirst("Authorization"),
            intercambio.getRequestHeaders().getFirst("Idempotency-Key"), cuerpo,
            intercambio.getRequestHeaders().getFirst("X-Service-Key"));
        synchronized (this) {
            peticiones.add(peticion);
        }
        contador.incrementAndGet();
        Respuesta respuesta = manejador.apply(peticion);
        byte[] bytes = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(respuesta.estado(), bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        }
        intercambio.close();
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
