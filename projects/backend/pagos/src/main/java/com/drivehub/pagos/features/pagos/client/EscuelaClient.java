package com.drivehub.pagos.features.pagos.client;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;

/**
 * Llama a Escuela por la red interna, sin pasar por el gateway: los alumnos de un instructor (CU29).
 *
 * * Reenvía el Bearer del instructor: Escuela vuelve a validar el token y el rol, y toma de él quién es el
 * * instructor. Pagos no guarda la lista de alumnos.
 * * Pide páginas de 100 hasta agotarlas o llegar al tope de {@value #TOPE_ALUMNOS} alumnos (si hay más, se corta
 * * y se registra un WARN).
 * * Cualquier falla (5xx, 4xx distinto de 404, timeout, corte de red, respuesta incompleta) es 503 `ERR_PAG_15`.
 * ! Nunca se escribe el token ni los códigos de los alumnos en los registros.
 */
@Component
public class EscuelaClient {

    static final int TOPE_ALUMNOS = 500;
    static final int TAMANO_PAGINA = 100;

    private static final Logger LOG = LoggerFactory.getLogger(EscuelaClient.class);
    private static final String RUTA = "/api/v1/asignaciones-alumno/mis-alumnos";
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos consultar tus alumnos. Inténtalo de nuevo más tarde.";

    private final RestClient restClient;

    public EscuelaClient(
            @Value("${app.escuela.url}") String baseUrl,
            @Value("${app.escuela.timeout-segundos:5}") long timeoutSegundos) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    /**
     * Los códigos de los alumnos vigentes del instructor del token, hasta {@value #TOPE_ALUMNOS}.
     *
     * @param bearer la cabecera `Authorization` completa (`Bearer ...`)
     */
    public List<UUID> alumnosDelInstructor(String bearer) {
        List<UUID> alumnos = new ArrayList<>();
        int pagina = 1;
        while (true) {
            final int actual = pagina;
            SobreAlumnos sobre;
            try {
                sobre = restClient.get()
                    .uri(uri -> uri.path(RUTA).queryParam("page", actual).queryParam("pageSize", TAMANO_PAGINA)
                        .build())
                    .header(HttpHeaders.AUTHORIZATION, bearer).retrieve().body(SobreAlumnos.class);
            } catch (RestClientException e) {
                LOG.warn("No se pudieron consultar los alumnos en Escuela (tipo {})", e.getClass().getSimpleName(),
                    kv("module", "horas-alumnos"));
                throw noDisponible();
            }
            if (sobre == null || sobre.data() == null || sobre.meta() == null) {
                LOG.warn("Escuela respondió los alumnos sin los datos esperados", kv("module", "horas-alumnos"));
                throw noDisponible();
            }
            for (Alumno alumno : sobre.data()) {
                if (alumno.estudianteCodigo() == null) {
                    LOG.warn("Escuela respondió un alumno sin código", kv("module", "horas-alumnos"));
                    throw noDisponible();
                }
                alumnos.add(alumno.estudianteCodigo());
            }
            if (alumnos.size() >= TOPE_ALUMNOS) {
                if (sobre.meta().totalItems() > TOPE_ALUMNOS) {
                    LOG.warn("El instructor tiene más alumnos que el tope; se muestran los primeros {}",
                        TOPE_ALUMNOS, kv("module", "horas-alumnos"));
                }
                return List.copyOf(alumnos.subList(0, TOPE_ALUMNOS));
            }
            if (pagina >= sobre.meta().totalPages() || sobre.data().isEmpty()) {
                return List.copyOf(alumnos);
            }
            pagina++;
        }
    }

    /**
     * Si ese estudiante es alumno vigente del instructor del token. `404` es "no": no se distingue "no existe" de
     * "no es tuyo".
     */
    public boolean esMiAlumno(UUID estudianteCodigo, String bearer) {
        try {
            restClient.get().uri(RUTA + "/{codigo}", estudianteCodigo)
                .header(HttpHeaders.AUTHORIZATION, bearer).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            LOG.warn("No se pudo confirmar el alumno en Escuela (tipo {})", e.getClass().getSimpleName(),
                kv("module", "horas-alumnos"));
            throw noDisponible();
        }
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE,
            MENSAJE_NO_DISPONIBLE);
    }

    // * Sobre estándar de Escuela ({status, data, meta, ...}); solo se leen estos campos
    private record SobreAlumnos(List<Alumno> data, Meta meta) {
    }

    private record Alumno(UUID estudianteCodigo) {
    }

    private record Meta(long totalItems, int totalPages) {
    }
}
