package com.drivehub.escuela.infrastructure.client;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ServicioNoDisponibleException;
import com.drivehub.escuela.domain.entity.MomentoDelDia;
import com.drivehub.escuela.domain.gateway.HorariosGateway;
import com.drivehub.escuela.domain.gateway.ResumenFranjas;

/**
 * Llama a Académico por REST interno, sin pasar por el gateway:
 * `{app.academico.url}/api/v1/franjas/resumen`.
 *
 * * Reenvía el Bearer del Administrador que hizo la petición: Académico vuelve a validar el token y el rol, y
 * * toma la escuela de él. Escuela no guarda las franjas.
 * * Cualquier falla (5xx, 4xx, timeout, corte de red, respuesta sin datos) es `ERR_ESC_20`.
 * ! Nunca se escribe el token ni los códigos de los instructores en los registros.
 */
@Component
public class AcademicoRestClient implements HorariosGateway {

    private static final Logger LOG = LoggerFactory.getLogger(AcademicoRestClient.class);
    private static final String RUTA = "/api/v1/franjas/resumen";
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos consultar los horarios de los instructores. Inténtalo de nuevo más tarde.";

    private final RestClient restClient;

    public AcademicoRestClient(
            @Value("${app.academico.url}") String baseUrl,
            @Value("${app.academico.timeout-segundos:5}") long timeoutSegundos) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    @Override
    public Map<UUID, ResumenFranjas> resumenDelDia(LocalDate fecha, MomentoDelDia momento) {
        SobreResumen sobre;
        try {
            sobre = restClient.get()
                .uri(uri -> {
                    uri.path(RUTA).queryParam("filter[fecha]", fecha);
                    if (momento != null && momento.horaDesde() != null) {
                        uri.queryParam("filter[horaDesde]", momento.horaDesde());
                    }
                    if (momento != null && momento.horaHasta() != null) {
                        uri.queryParam("filter[horaHasta]", momento.horaHasta());
                    }
                    return uri.build();
                })
                .header(HttpHeaders.AUTHORIZATION, TraductorDeErroresDeIdentidad.bearerDeLaSesion())
                .retrieve()
                .body(SobreResumen.class);
        } catch (RestClientException e) {
            LOG.warn("No se pudo consultar el resumen de franjas en Académico (tipo {})",
                e.getClass().getSimpleName(), kv("module", "disponibilidad"),
                kv("errorCode", EscuelaErrorCodes.HORARIOS_NO_DISPONIBLES));
            throw noDisponible();
        }
        if (sobre == null || sobre.data() == null) {
            LOG.warn("Académico respondió el resumen de franjas sin los datos esperados",
                kv("module", "disponibilidad"), kv("errorCode", EscuelaErrorCodes.HORARIOS_NO_DISPONIBLES));
            throw noDisponible();
        }
        Map<UUID, ResumenFranjas> resumen = new HashMap<>();
        for (DatosResumen d : sobre.data()) {
            resumen.put(d.instructorCodigo(), new ResumenFranjas(d.total(), d.libres(), d.reservadas(), d.bloqueadas()));
        }
        return resumen;
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(EscuelaErrorCodes.HORARIOS_NO_DISPONIBLES, MENSAJE_NO_DISPONIBLE);
    }

    // * Sobre estándar de Académico ({status, data, ...}); solo se leen estos campos
    private record SobreResumen(List<DatosResumen> data) {
    }

    private record DatosResumen(UUID instructorCodigo, int total, int libres, int reservadas, int bloqueadas) {
    }
}
