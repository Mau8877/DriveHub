package com.drivehub.identidad.infrastructure.adapter.out.escuela;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaRef;

/**
 * Consulta a Escuela (el servicio dueño del dato) por REST interno, sin pasar
 * por el gateway: `GET {app.escuela.url}/api/v1/escuelas/{slug}`.
 *
 * * 404 = la escuela no existe o está inactiva -> vacío. Cualquier otro fallo
 * * (Escuela caída, timeout, 5xx) se propaga y cae en el 500 genérico: no se
 * * puede registrar a nadie sin poder confirmar la escuela.
 */
@Component
public class EscuelaRestAdapter implements EscuelaConsultaPort {

    private final RestClient restClient;

    public EscuelaRestAdapter(@Value("${app.escuela.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    @Override
    public Optional<EscuelaRef> buscarActivaPorSlug(String slug) {
        try {
            SobreEscuela sobre = restClient.get()
                .uri("/api/v1/escuelas/{slug}", slug)
                .retrieve()
                .body(SobreEscuela.class);
            if (sobre == null || sobre.data() == null) {
                return Optional.empty();
            }
            return Optional.of(new EscuelaRef(sobre.data().codigo(), sobre.data().slug(), sobre.data().nombre()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    // * Sobre estándar de Escuela ({status, data, ...}); solo se leen estos campos
    private record SobreEscuela(DatosEscuela data) {
    }

    private record DatosEscuela(UUID codigo, String slug, String nombre) {
    }
}
