package com.drivehub.gateway.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

/**
 * Cuando el servicio dueño de un recurso no responde, el cliente recibe un 503 en el sobre
 * estándar (ERR_SYS_06) en vez de un 500 con el mensaje técnico y el host interno.
 */
@RestControllerAdvice
public class ServicioNoDisponibleHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioNoDisponibleHandler.class);

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handle(ResourceAccessException ex) {
        LOG.warn("Servicio destino no responde: {}", ex.getMessage(),
            net.logstash.logback.argument.StructuredArguments.keyValue("module", "gateway"));
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", "failed");
        cuerpo.put("data", null);
        cuerpo.put("message", "El servicio no está disponible por el momento. Inténtalo de nuevo en unos minutos");
        cuerpo.put("timestamp", Instant.now().toString());
        cuerpo.put("error", "ERR_SYS_06");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(cuerpo);
    }
}
