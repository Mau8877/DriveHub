package com.drivehub.identidad.infrastructure.adapter.out.escuela;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.drivehub.identidad.domain.model.CategoriaNombres;
import com.drivehub.identidad.domain.port.out.CategoriaLicenciaConsultaPort;
import com.drivehub.identidad.domain.port.out.EscuelaNoDisponibleException;

/**
 * Consulta el catálogo de categorías a Escuela por REST interno, sin pasar por el gateway:
 * `GET {app.escuela.url}/api/v1/categorias-licencia/{sigla}`.
 *
 * * Reenvía el Bearer de la sesión de quien llama (el Administrador): Escuela exige ese rol.
 * * 200 = existe; 404 = no existe. Cualquier otra respuesta, un timeout o un error de red lanza
 * * {@link EscuelaNoDisponibleException}: sin poder confirmar la categoría no se guarda nada.
 * ! El Bearer nunca se escribe en los registros.
 */
@Component
public class CategoriaLicenciaRestAdapter implements CategoriaLicenciaConsultaPort {

    private static final Logger LOG = LoggerFactory.getLogger(CategoriaLicenciaRestAdapter.class);
    private static final String MODULO = "estudiantes";

    private final RestClient restClient;

    public CategoriaLicenciaRestAdapter(@Value("${app.escuela.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    @Override
    public boolean existe(String sigla) {
        if (CategoriaNombres.SIN_CATEGORIA.equalsIgnoreCase(sigla)) {
            return true;
        }
        String bearer = bearerDeLaSesion();
        try {
            restClient.get()
                .uri("/api/v1/categorias-licencia/{sigla}", sigla)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                .retrieve()
                .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            LOG.warn("Escuela no pudo confirmar la categoría", kv("module", MODULO));
            throw new EscuelaNoDisponibleException(e);
        }
    }

    private String bearerDeLaSesion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }
        LOG.warn("No hay una sesión de la que reenviar el token a Escuela", kv("module", MODULO));
        throw new EscuelaNoDisponibleException();
    }
}
