package com.drivehub.pagos.features.pagos.client;

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

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;

/**
 * Llama a Identidad por la red interna, sin pasar por el gateway: el perfil propio
 * (`{app.identidad.url}/api/v1/usuarios/me`, CU26) y los nombres de un conjunto de estudiantes
 * (`/api/v1/estudiantes/consulta`, CU29).
 *
 * * Reenvía el Bearer del estudiante que compra: Identidad vuelve a validar el token y devuelve SU perfil.
 * * Sirve para copiar al pago el nombre del estudiante y el de su escuela (el comprobante sale de esa
 * * copia, cuando llega el aviso de la pasarela y ya no hay sesión de nadie).
 * * Cualquier falla (5xx, 4xx, timeout, corte de red, respuesta incompleta) es 503 `ERR_PAG_15`.
 * ! Nunca se escribe el token ni el perfil en los registros.
 */
@Component
public class IdentidadClient {

    private static final Logger LOG = LoggerFactory.getLogger(IdentidadClient.class);
    static final int LOTE_NOMBRES = 100;

    private static final String RUTA = "/api/v1/usuarios/me";
    private static final String RUTA_ESTUDIANTES = "/api/v1/estudiantes/consulta";
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos verificar tus datos. Inténtalo de nuevo más tarde.";

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
     * El perfil de quien hace la petición: su nombre completo y el nombre de su escuela.
     *
     * @param bearer la cabecera `Authorization` completa (`Bearer ...`)
     */
    public PerfilEstudiante perfilPropio(String bearer) {
        SobrePerfil sobre;
        try {
            sobre = restClient.get().uri(RUTA).header(HttpHeaders.AUTHORIZATION, bearer).retrieve()
                .body(SobrePerfil.class);
        } catch (RestClientException e) {
            LOG.warn("No se pudo consultar el perfil en Identidad (tipo {})", e.getClass().getSimpleName(),
                kv("module", "pagos"));
            throw noDisponible();
        }
        if (sobre == null || sobre.data() == null || sobre.data().nombres() == null
                || sobre.data().apellidos() == null) {
            LOG.warn("Identidad respondió el perfil sin los datos esperados", kv("module", "pagos"));
            throw noDisponible();
        }
        String nombre = (sobre.data().nombres() + " " + sobre.data().apellidos()).trim();
        return new PerfilEstudiante(nombre, sobre.data().escuelaNombre());
    }

    /**
     * Los nombres y apellidos de esos estudiantes (CU29), pedidos en lotes de {@value #LOTE_NOMBRES}.
     * Los que Identidad no devuelve (de otra escuela, que no son estudiantes, que no existen) no están en el mapa.
     *
     * @param bearer la cabecera `Authorization` completa del instructor (`Bearer ...`)
     */
    public Map<UUID, NombreEstudiante> nombresDe(List<UUID> codigos, String bearer) {
        Map<UUID, NombreEstudiante> nombres = new HashMap<>();
        List<UUID> sinRepetidos = codigos.stream().distinct().toList();
        for (int desde = 0; desde < sinRepetidos.size(); desde += LOTE_NOMBRES) {
            List<UUID> lote = sinRepetidos.subList(desde, Math.min(desde + LOTE_NOMBRES, sinRepetidos.size()));
            SobreEstudiantes sobre;
            try {
                sobre = restClient.post().uri(RUTA_ESTUDIANTES)
                    .header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("codigos", lote)).retrieve().body(SobreEstudiantes.class);
            } catch (RestClientException e) {
                LOG.warn("No se pudieron consultar los nombres en Identidad (tipo {})",
                    e.getClass().getSimpleName(), kv("module", "pagos"));
                throw noDisponible();
            }
            if (sobre == null || sobre.data() == null) {
                LOG.warn("Identidad respondió los nombres sin los datos esperados", kv("module", "pagos"));
                throw noDisponible();
            }
            for (DatosEstudiante estudiante : sobre.data()) {
                if (estudiante.codigo() != null && estudiante.nombres() != null && estudiante.apellidos() != null) {
                    nombres.put(estudiante.codigo(), new NombreEstudiante(estudiante.nombres().trim(),
                        estudiante.apellidos().trim()));
                }
            }
        }
        return nombres;
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE,
            MENSAJE_NO_DISPONIBLE);
    }

    /**
     * Lo que Pagos copia al pago.
     *
     * @param escuelaNombre null si la cuenta no es de ninguna escuela
     */
    public record PerfilEstudiante(String nombreCompleto, String escuelaNombre) {
    }

    /**
     * El nombre de un estudiante, tal como lo guarda Identidad (sin espacios en los extremos).
     */
    public record NombreEstudiante(String nombres, String apellidos) {

        public String completo() {
            return nombres + " " + apellidos;
        }
    }

    // * Sobre estándar de Identidad ({status, data, ...}); solo se leen estos campos
    private record SobrePerfil(DatosPerfil data) {
    }

    private record DatosPerfil(String nombres, String apellidos, String escuelaNombre) {
    }

    private record SobreEstudiantes(List<DatosEstudiante> data) {
    }

    private record DatosEstudiante(UUID codigo, String nombres, String apellidos) {
    }
}
