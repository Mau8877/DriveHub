package com.drivehub.identidad.infrastructure.adapter.out.geocodificacion;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;

/**
 * Geocodificación inversa con OpenStreetMap Nominatim:
 * `GET {app.geocodificacion.url}/reverse?format=jsonv2&lat=..&lon=..&zoom=18&accept-language=es`.
 *
 * * La política de uso del servidor público pide un User-Agent identificable y no más de una
 * * consulta por segundo: un candado espacia las llamadas, y si esperar su turno tardaría más que
 * * el tiempo máximo, se devuelve vacío en vez de bloquear la petición.
 * ! Nunca lanza ni escribe las coordenadas en los registros.
 */
@Component
public class NominatimGeocodificadorAdapter implements GeocodificadorInversoPort {

    private static final Logger LOG = LoggerFactory.getLogger(NominatimGeocodificadorAdapter.class);
    private static final String MODULO = "geocodificacion";
    private static final int LARGO_MAXIMO = 300;
    private static final long ESPACIADO_MS = 1000;

    private final RestClient restClient;
    private final long timeoutMs;
    private final ReentrantLock candado = new ReentrantLock();
    private long ultimaLlamadaMs;

    public NominatimGeocodificadorAdapter(
            @Value("${app.geocodificacion.url}") String baseUrl,
            @Value("${app.geocodificacion.timeout-segundos:3}") long timeoutSegundos,
            @Value("${app.geocodificacion.user-agent:DriveHub/1.0}") String userAgent) {
        this.timeoutMs = timeoutSegundos * 1000;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(fabrica)
            .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
            .build();
    }

    @Override
    public Optional<String> direccionDe(BigDecimal latitud, BigDecimal longitud) {
        try {
            if (!esperarTurno()) {
                LOG.warn("Geocodificación omitida: esperar el turno tardaría demasiado", kv("module", MODULO));
                return Optional.empty();
            }
            RespuestaNominatim respuesta = restClient.get()
                .uri(uri -> uri.path("/reverse")
                    .queryParam("format", "jsonv2")
                    .queryParam("lat", latitud.toPlainString())
                    .queryParam("lon", longitud.toPlainString())
                    .queryParam("zoom", 18)
                    .queryParam("accept-language", "es")
                    .build())
                .retrieve()
                .body(RespuestaNominatim.class);
            if (respuesta == null || respuesta.error() != null || respuesta.display_name() == null
                    || respuesta.display_name().isBlank()) {
                LOG.warn("El punto no tiene dirección conocida", kv("module", MODULO));
                return Optional.empty();
            }
            String direccion = respuesta.display_name().trim();
            return Optional.of(direccion.length() > LARGO_MAXIMO ? direccion.substring(0, LARGO_MAXIMO) : direccion);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo calcular la dirección: se guarda el punto sin ella",
                kv("module", MODULO), kv("causa", e.getClass().getSimpleName()));
            return Optional.empty();
        }
    }

    /** Espera su turno respetando el espaciado entre llamadas; false si tardaría más que el tiempo máximo. */
    private boolean esperarTurno() {
        try {
            if (!candado.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        try {
            long ahora = System.currentTimeMillis();
            long espera = ultimaLlamadaMs + ESPACIADO_MS - ahora;
            if (espera > timeoutMs) {
                return false;
            }
            if (espera > 0) {
                Thread.sleep(espera);
            }
            ultimaLlamadaMs = System.currentTimeMillis();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            candado.unlock();
        }
    }

    // * Solo se leen estos campos de la respuesta de Nominatim
    private record RespuestaNominatim(String display_name, String error) {
    }
}
