package com.drivehub.academico.features.reservas.client;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;

/**
 * Pregunta a Identidad por la red interna, sin pasar por el gateway, el nombre de los instructores (CU14):
 * `{app.identidad.url}/api/v1/usuarios/instructores/nombres`.
 *
 * * Reenvía el Bearer del estudiante: Identidad vuelve a validar el token y toma de su cuenta la escuela, así que
 * * solo devuelve instructores de esa escuela. Académico no guarda nombres: los une a cada respuesta.
 * * Cualquier falla (5xx, 4xx, timeout, corte de red, respuesta incompleta) es 503 `ERR_ACA_17`.
 * ! Nunca se escribe el token ni los nombres en los registros; solo códigos.
 */
@Component
public class IdentidadClient {

    static final int LOTE_NOMBRES = 100;

    private static final Logger LOG = LoggerFactory.getLogger(IdentidadClient.class);
    private static final String RUTA = "/api/v1/usuarios/instructores/nombres";
    private static final String RUTA_ESTUDIANTES = "/api/v1/estudiantes/consulta";
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos obtener los datos de los instructores. Inténtalo de nuevo más tarde.";

    private final RestClient restClient;

    public IdentidadClient(
            @Value("${app.identidad.url}") String baseUrl,
            @Value("${app.identidad.timeout-segundos:5}") long timeoutSegundos) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    /**
     * El nombre completo de esos instructores, pedidos en lotes de {@value #LOTE_NOMBRES} sin repetidos. Los que
     * Identidad no devuelve (de otra escuela, que no son instructores, que no existen) no están en el mapa.
     *
     * @param bearer la cabecera `Authorization` completa (`Bearer ...`)
     */
    public Map<UUID, String> nombresDeInstructores(List<UUID> codigos, String bearer) {
        Map<UUID, String> nombres = new HashMap<>();
        List<UUID> sinRepetidos = codigos.stream().distinct().toList();
        for (int desde = 0; desde < sinRepetidos.size(); desde += LOTE_NOMBRES) {
            List<UUID> lote = sinRepetidos.subList(desde, Math.min(desde + LOTE_NOMBRES, sinRepetidos.size()));
            SobreNombres sobre;
            try {
                sobre = restClient.post().uri(RUTA)
                    .header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("codigos", lote)).retrieve().body(SobreNombres.class);
            } catch (RestClientException e) {
                LOG.warn("No se pudieron consultar los nombres en Identidad (tipo {})",
                    e.getClass().getSimpleName(), kv("module", "reservas"));
                throw noDisponible();
            }
            if (sobre == null || sobre.data() == null) {
                LOG.warn("Identidad respondió los nombres sin los datos esperados", kv("module", "reservas"));
                throw noDisponible();
            }
            for (DatosNombre instructor : sobre.data()) {
                if (instructor.codigo() != null && instructor.nombreCompleto() != null) {
                    nombres.put(instructor.codigo(), instructor.nombreCompleto().trim());
                }
            }
        }
        return nombres;
    }

    /**
     * El nombre completo de esos estudiantes de la escuela de quien pregunta (un Administrador o un instructor,
     * CU22), en lotes de {@value #LOTE_NOMBRES}. Los que Identidad no devuelve (de otra escuela, que no existen) no
     * están en el mapa.
     */
    public Map<UUID, String> nombresDeEstudiantes(List<UUID> codigos, String bearer) {
        Map<UUID, String> nombres = new HashMap<>();
        List<UUID> sinRepetidos = codigos.stream().distinct().toList();
        for (int desde = 0; desde < sinRepetidos.size(); desde += LOTE_NOMBRES) {
            List<UUID> lote = sinRepetidos.subList(desde, Math.min(desde + LOTE_NOMBRES, sinRepetidos.size()));
            SobreEstudiantes sobre;
            try {
                sobre = restClient.post().uri(RUTA_ESTUDIANTES)
                    .header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("codigos", lote)).retrieve().body(SobreEstudiantes.class);
            } catch (RestClientException e) {
                LOG.warn("No se pudieron consultar los estudiantes en Identidad (tipo {})",
                    e.getClass().getSimpleName(), kv("module", "examenes"));
                throw new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE,
                    "No pudimos obtener los datos de los estudiantes. Inténtalo de nuevo más tarde.");
            }
            if (sobre == null || sobre.data() == null) {
                LOG.warn("Identidad respondió los estudiantes sin los datos esperados", kv("module", "examenes"));
                throw new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE,
                    "No pudimos obtener los datos de los estudiantes. Inténtalo de nuevo más tarde.");
            }
            for (DatosEstudiante e : sobre.data()) {
                if (e.codigo() != null) {
                    nombres.put(e.codigo(), ((e.nombres() == null ? "" : e.nombres()) + " "
                        + (e.apellidos() == null ? "" : e.apellidos())).trim());
                }
            }
        }
        return nombres;
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, MENSAJE_NO_DISPONIBLE);
    }

    // * Sobre estándar de Identidad ({status, data, ...}); solo se leen estos campos
    private record SobreNombres(List<DatosNombre> data) {
    }

    private record DatosNombre(UUID codigo, String nombreCompleto) {
    }

    private record SobreEstudiantes(List<DatosEstudiante> data) {
    }

    private record DatosEstudiante(UUID codigo, String nombres, String apellidos) {
    }
}
