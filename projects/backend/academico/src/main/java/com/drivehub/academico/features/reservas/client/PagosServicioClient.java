package com.drivehub.academico.features.reservas.client;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;

/**
 * Llama a las rutas internas de la billetera de Pagos con la credencial de SERVICIO, sin sesión de ningún estudiante
 * (CU44): `{app.pagos.url}/internal/v1/billeteras-horas`.
 *
 * * Lo usa el proceso programado (el orquestador), que corre sin ningún estudiante conectado. Se identifica con el
 * * header `X-Service-Key` (un secreto compartido con Pagos) y NUNCA manda `Authorization`.
 * * Misma `app.pagos.url` y mismo timeout que {@link PagosClient}, y el mismo manejo de errores: sin horas
 * * (`409 ERR_PAG_09`) es {@link SinHorasException}; devolver una clase sin consumo (`404 ERR_PAG_16`) no es un
 * * error; cualquier otra falla (5xx, `401 ERR_PAG_19`, timeout, corte) es 503 `ERR_ACA_17`.
 * * Sin credencial configurada NO sale ninguna petición: se registra un error y se trata como «Pagos no disponible».
 * ! Nunca se escribe la credencial, el cuerpo de las respuestas ni datos de pago en los registros; solo códigos.
 */
@Component("clienteDeServicio")
public class PagosServicioClient implements ClienteDePagos {

    static final String CABECERA_CLAVE = "Idempotency-Key";
    static final String CABECERA_CREDENCIAL = "X-Service-Key";

    private static final Logger LOG = LoggerFactory.getLogger(PagosServicioClient.class);
    private static final String RUTA = "/internal/v1/billeteras-horas";
    private static final String SIN_HORAS = "ERR_PAG_09";
    private static final String SIN_CONSUMO = "ERR_PAG_16";
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos verificar tus horas. Inténtalo de nuevo más tarde.";

    private final RestClient restClient;
    private final String credencial;

    public PagosServicioClient(
            @Value("${app.pagos.url}") String baseUrl,
            @Value("${app.pagos.timeout-segundos:5}") long timeoutSegundos,
            @Value("${app.pagos.credencial-servicio:}") String credencial) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
        this.credencial = credencial == null ? "" : credencial;
    }

    @Override
    public PagosClient.ResultadoConsumo descontar(UUID escuelaCodigo, UUID estudianteCodigo, UUID instructorCodigo,
            UUID claseCodigo, int horas, String idempotencyKey) {
        exigirCredencial();
        SobreConsumo sobre;
        try {
            sobre = restClient.post().uri(RUTA + "/consumos")
                .header(CABECERA_CREDENCIAL, credencial).header(CABECERA_CLAVE, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("escuelaCodigo", escuelaCodigo, "estudianteCodigo", estudianteCodigo, "horas", horas,
                    "instructorCodigo", instructorCodigo, "claseCodigo", claseCodigo))
                .retrieve().body(SobreConsumo.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT && SIN_HORAS.equals(codigoDeError(e))) {
                throw new SinHorasException();
            }
            throw fallo("descontar horas", e);
        } catch (RestClientException e) {
            throw fallo("descontar horas", e);
        }
        if (sobre == null || sobre.data() == null || sobre.data().horasDescontadas() == null
                || sobre.data().horasDisponibles() == null) {
            LOG.warn("Pagos respondió el descuento interno sin los datos esperados", kv("module", "reservas"));
            throw noDisponible();
        }
        return new PagosClient.ResultadoConsumo(sobre.data().horasDescontadas(), sobre.data().horasDisponibles());
    }

    @Override
    public void devolver(UUID escuelaCodigo, UUID estudianteCodigo, UUID claseCodigo, String idempotencyKey) {
        exigirCredencial();
        try {
            restClient.post().uri(RUTA + "/devoluciones")
                .header(CABECERA_CREDENCIAL, credencial).header(CABECERA_CLAVE, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("escuelaCodigo", escuelaCodigo, "estudianteCodigo", estudianteCodigo,
                    "claseCodigo", claseCodigo))
                .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND && SIN_CONSUMO.equals(codigoDeError(e))) {
                return;
            }
            throw fallo("devolver horas", e);
        } catch (RestClientException e) {
            throw fallo("devolver horas", e);
        }
    }

    /** Sin credencial no sale nada: nunca se manda un header vacío. */
    private void exigirCredencial() {
        if (credencial.isBlank()) {
            LOG.error("No hay credencial de servicio configurada para hablar con Pagos: no se hace ninguna llamada",
                kv("module", "reservas"));
            throw noDisponible();
        }
    }

    /** El código de negocio (`error`) del sobre de una respuesta de error, o null si no se puede leer. */
    private static String codigoDeError(HttpClientErrorException e) {
        try {
            SobreError sobre = e.getResponseBodyAs(SobreError.class);
            return sobre == null ? null : sobre.error();
        } catch (RuntimeException ignorada) {
            return null;
        }
    }

    private static ServicioNoDisponibleException fallo(String operacion, RestClientException e) {
        if (e instanceof HttpClientErrorException http
                && (http.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || http.getStatusCode() == HttpStatus.FORBIDDEN)) {
            // * Pagos rechazó la credencial: no se arregla solo, quien opera el servicio tiene que verlo
            LOG.error("Pagos rechazó la credencial de servicio al {}: revisar PAGOS_CREDENCIAL_ACADEMICO", operacion,
                kv("module", "reservas"));
        } else {
            LOG.warn("No se pudo {} en Pagos con la credencial de servicio (tipo {})", operacion,
                e.getClass().getSimpleName(), kv("module", "reservas"));
        }
        return noDisponible();
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, MENSAJE_NO_DISPONIBLE);
    }

    // * Sobre estándar de Pagos ({status, data, error, ...}); solo se leen estos campos
    private record SobreConsumo(DatosConsumo data) {
    }

    private record DatosConsumo(Integer horasDescontadas, Integer horasDisponibles) {
    }

    private record SobreError(String error) {
    }
}
