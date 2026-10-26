package com.drivehub.academico.features.reservas.client;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
 * Llama a la billetera de horas de Pagos por la red interna, sin pasar por el gateway (CU14):
 * `{app.pagos.url}/api/v1/billeteras-horas`.
 *
 * * Reenvía el Bearer del estudiante: Pagos vuelve a validar el token y solo deja al estudiante gastar sus propias
 * * horas. Por eso solo se puede usar dentro de una petición del estudiante, nunca desde un proceso sin sesión.
 * * Descontar y devolver llevan una clave de idempotencia: repetirlas con la misma clave no gasta ni devuelve dos
 * * veces (contrato en `docs/DriveHub/CONTRATO_BILLETERA_HORAS.md`).
 * * Sin horas suficientes (`409 ERR_PAG_09`) es {@link SinHorasException}; cualquier otra falla (5xx, 4xx, timeout,
 * * corte de red, respuesta incompleta) es 503 `ERR_ACA_17`.
 * ! Nunca se escribe el token ni el cuerpo de las respuestas en los registros; solo códigos.
 */
@Component
public class PagosClient {

    static final String CABECERA_CLAVE = "Idempotency-Key";

    private static final Logger LOG = LoggerFactory.getLogger(PagosClient.class);
    private static final String RUTA = "/api/v1/billeteras-horas";
    private static final String SIN_HORAS = "ERR_PAG_09";
    private static final String SIN_CONSUMO = "ERR_PAG_16";
    private static final String MENSAJE_NO_DISPONIBLE =
        "No pudimos verificar tus horas. Inténtalo de nuevo más tarde.";

    private final RestClient restClient;

    public PagosClient(
            @Value("${app.pagos.url}") String baseUrl,
            @Value("${app.pagos.timeout-segundos:5}") long timeoutSegundos) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    /**
     * Las horas disponibles del estudiante del token.
     *
     * @param bearer la cabecera `Authorization` completa (`Bearer ...`)
     */
    public int saldo(String bearer) {
        SobreSaldo sobre;
        try {
            sobre = restClient.get().uri(RUTA + "/mia").header(HttpHeaders.AUTHORIZATION, bearer).retrieve()
                .body(SobreSaldo.class);
        } catch (RestClientException e) {
            throw fallo("consultar el saldo", e);
        }
        if (sobre == null || sobre.data() == null || sobre.data().horasDisponibles() == null) {
            LOG.warn("Pagos respondió el saldo sin los datos esperados", kv("module", "reservas"));
            throw noDisponible();
        }
        return sobre.data().horasDisponibles();
    }

    /**
     * Descuenta horas de la billetera del estudiante por una clase.
     *
     * @param claseCodigo el código de la reserva: de ahí sale cuántas horas devolver si se cancela
     * @param idempotencyKey la misma clave devuelve el resultado anterior sin descontar de nuevo
     * @throws SinHorasException si no hay horas suficientes (no se descontó nada)
     */
    public ResultadoConsumo descontar(String bearer, UUID estudianteCodigo, UUID instructorCodigo, UUID claseCodigo,
            int horas, String idempotencyKey) {
        SobreConsumo sobre;
        try {
            sobre = restClient.post().uri(RUTA + "/consumos")
                .header(HttpHeaders.AUTHORIZATION, bearer).header(CABECERA_CLAVE, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("estudianteCodigo", estudianteCodigo, "horas", horas,
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
            LOG.warn("Pagos respondió el descuento sin los datos esperados", kv("module", "reservas"));
            throw noDisponible();
        }
        return new ResultadoConsumo(sobre.data().horasDescontadas(), sobre.data().horasDisponibles());
    }

    /**
     * Devuelve lo que descontó la clase. Que la clase no haya descontado nada (`404 ERR_PAG_16`) no es un error: es
     * el caso de una compensación que se hace sin saber si el descuento llegó a ocurrir.
     */
    public void devolver(String bearer, UUID estudianteCodigo, UUID claseCodigo, String idempotencyKey) {
        try {
            restClient.post().uri(RUTA + "/devoluciones")
                .header(HttpHeaders.AUTHORIZATION, bearer).header(CABECERA_CLAVE, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("estudianteCodigo", estudianteCodigo, "claseCodigo", claseCodigo))
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
        LOG.warn("No se pudo {} en Pagos (tipo {})", operacion, e.getClass().getSimpleName(),
            kv("module", "reservas"));
        return noDisponible();
    }

    private static ServicioNoDisponibleException noDisponible() {
        return new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, MENSAJE_NO_DISPONIBLE);
    }

    /** Lo que Pagos informó al descontar: cuántas horas gastó y cuántas le quedan al estudiante. */
    public record ResultadoConsumo(int horasDescontadas, int horasDisponibles) {
    }

    // * Sobre estándar de Pagos ({status, data, error, ...}); solo se leen estos campos
    private record SobreSaldo(DatosSaldo data) {
    }

    private record DatosSaldo(Integer horasDisponibles) {
    }

    private record SobreConsumo(DatosConsumo data) {
    }

    private record DatosConsumo(Integer horasDescontadas, Integer horasDisponibles) {
    }

    private record SobreError(String error) {
    }
}
