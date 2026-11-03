package com.drivehub.pagos.features.pagos.client;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Pide un aviso a Notificaciones por la red interna, con la credencial de SERVICIO (CU36):
 * `POST {app.notificaciones.url}/internal/v1/notificaciones`. La ruta no pasa por el gateway.
 *
 * * Es de mejor esfuerzo: el aviso nunca puede romper la operación de negocio que lo origina. Cualquier falla
 * * (timeout, 5xx, `401`) se registra y se descarta; {@link #avisar} no lanza excepciones.
 * * Sin credencial configurada no sale ninguna petición.
 * * El `eventoCodigo` es determinista (ver {@link #eventoCodigo}): repetir el mismo aviso lo deja en
 * * `DUPLICADA` del lado de Notificaciones, así un reintento no avisa dos veces.
 * ! Nunca se escribe la credencial ni el texto del aviso en los registros; solo el tipo y el evento.
 */
@Component
public class NotificacionesClient {

    static final String CABECERA_CREDENCIAL = "X-Service-Key";

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionesClient.class);
    private static final String RUTA = "/internal/v1/notificaciones";

    private final RestClient restClient;
    private final String credencial;

    public NotificacionesClient(
            @Value("${app.notificaciones.url}") String baseUrl,
            @Value("${app.notificaciones.timeout-segundos:3}") long timeoutSegundos,
            @Value("${app.notificaciones.credencial:}") String credencial) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
        this.credencial = credencial == null ? "" : credencial;
    }

    /** Mismo evento y mismo recurso dan siempre el mismo código: es la clave de idempotencia de Notificaciones. */
    public static UUID eventoCodigo(String evento, UUID recurso) {
        return UUID.nameUUIDFromBytes(("pagos:" + evento + ":" + recurso).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param tipo `RESERVA`, `PAGO`, `EXAMEN` o `RECORDATORIO`
     * @param datos contexto que viaja con el aviso (códigos públicos, nunca ids internos); puede ser nulo
     */
    public void avisar(UUID eventoCodigo, UUID usuarioCodigo, String tipo, String titulo, String mensaje,
            Map<String, Object> datos) {
        if (credencial.isBlank()) {
            LOG.warn("No hay credencial de servicio para Notificaciones: no se envía el aviso",
                kv("module", "pagos"), kv("tipo", tipo));
            return;
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("eventoCodigo", eventoCodigo);
        cuerpo.put("usuarioCodigo", usuarioCodigo);
        cuerpo.put("tipo", tipo);
        cuerpo.put("titulo", titulo);
        cuerpo.put("mensaje", mensaje);
        if (datos != null && !datos.isEmpty()) {
            cuerpo.put("datos", datos);
        }
        try {
            restClient.post().uri(RUTA).header(CABECERA_CREDENCIAL, credencial)
                .contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            LOG.warn("No se pudo pedir un aviso a Notificaciones (tipo {})", e.getClass().getSimpleName(),
                kv("module", "pagos"), kv("tipo", tipo), kv("eventoCodigo", eventoCodigo));
        }
    }
}
