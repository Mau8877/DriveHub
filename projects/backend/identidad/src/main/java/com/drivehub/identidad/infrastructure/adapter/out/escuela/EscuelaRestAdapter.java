package com.drivehub.identidad.infrastructure.adapter.out.escuela;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.drivehub.identidad.domain.port.out.EscuelaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaNoDisponibleException;
import com.drivehub.identidad.domain.port.out.EscuelaRef;

/**
 * Consulta a Escuela (el servicio dueño del dato) por REST interno, sin pasar
 * por el gateway: `GET {app.escuela.url}/api/v1/escuelas/{slug}`.
 *
 * * 404 = la escuela no existe o está inactiva -> vacío. Cualquier otro fallo
 * * (Escuela caída, timeout, 5xx) lanza {@link EscuelaNoDisponibleException} (503 `ERR_IDE_24`): sin poder
 * * confirmar la escuela no se registra a nadie ni se inicia sesión.
 */
@Component
public class EscuelaRestAdapter implements EscuelaConsultaPort {

    private static final Logger LOG = LoggerFactory.getLogger(EscuelaRestAdapter.class);

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
        } catch (RestClientException e) {
            throw noDisponible(e);
        }
    }

    @Override
    public Optional<EscuelaRef> buscarPorCodigo(UUID codigo) {
        try {
            SobreEscuelaEstado sobre = restClient.get()
                .uri("/api/v1/escuelas/por-codigo/{codigo}", codigo)
                .retrieve()
                .body(SobreEscuelaEstado.class);
            if (sobre == null || sobre.data() == null) {
                return Optional.empty();
            }
            DatosEscuelaEstado d = sobre.data();
            return Optional.of(new EscuelaRef(d.codigo(), d.slug(), d.nombre(), d.activa()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw noDisponible(e);
        }
    }

    private static EscuelaNoDisponibleException noDisponible(RestClientException e) {
        LOG.warn("No se pudo consultar la escuela en Escuela (tipo {})", e.getClass().getSimpleName(),
            kv("module", "escuelas"));
        return new EscuelaNoDisponibleException(e);
    }

    // * Sobre estándar de Escuela ({status, data, ...}); solo se leen estos campos
    private record SobreEscuela(DatosEscuela data) {
    }

    private record DatosEscuela(UUID codigo, String slug, String nombre) {
    }

    private record SobreEscuelaEstado(DatosEscuelaEstado data) {
    }

    private record DatosEscuelaEstado(UUID codigo, String slug, String nombre, boolean activa) {
    }
}
