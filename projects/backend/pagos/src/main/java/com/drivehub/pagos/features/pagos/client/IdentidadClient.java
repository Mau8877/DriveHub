package com.drivehub.pagos.features.pagos.client;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.ServicioNoDisponibleException;

/**
 * Llama a Identidad por la red interna, sin pasar por el gateway: `{app.identidad.url}/api/v1/usuarios/me`.
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
    private static final String RUTA = "/api/v1/usuarios/me";
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

    // * Sobre estándar de Identidad ({status, data, ...}); solo se leen estos campos
    private record SobrePerfil(DatosPerfil data) {
    }

    private record DatosPerfil(String nombres, String apellidos, String escuelaNombre) {
    }
}
