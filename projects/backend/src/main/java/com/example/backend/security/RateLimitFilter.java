package com.example.backend.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limit por IP sobre /api/v1/** (endpoints de negocio).
 *
 * * En memoria (ConcurrentHashMap de buckets), no distribuido -- correcto
 * * para una sola instancia de backend (el caso de este proyecto hoy). Si
 * * en algún momento se corre más de una réplica del backend, cada una
 * * tendría su propio límite independiente en vez de uno compartido; para
 * * eso hay que migrar a bucket4j_jdk11-redis (Redis ya está en el stack,
 * * pero spring-boot-starter-data-redis todavía no está en el pom.xml).
 * ! No aplica a /health, /prometheus ni /swagger-ui/** -- ver shouldNotFilter.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final int capacity;
    private final int refillPerMinute;

    public RateLimitFilter(
            @Value("${app.ratelimit.capacity}") int capacity,
            @Value("${app.ratelimit.refill-per-minute}") int refillPerMinute) {
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        Bucket bucket = buckets.computeIfAbsent(resolveClientIp(request), ip -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // * request.getRemoteAddr() alcanza hoy porque nginx es el único proxy
    // * delante del backend dentro de la red de Docker. Si en el futuro hay
    // * más de un proxy en la cadena, esto necesita leer X-Forwarded-For
    // * con cuidado (el primer valor de la lista, no el último, para no
    // * confiar en un header que el propio cliente puede falsificar).
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        // * Se escribe el JSON a mano (no ApiResponse.error(...)) porque este
        // * filtro corre a nivel servlet, antes del dispatch a Spring MVC --
        // * el @RestControllerAdvice de GlobalExceptionHandler (ver
        // * EXCEPCIONES_BACKEND.md) nunca llega a intervenir acá. Igual se
        // * respeta el mismo contrato de sobre que RESPONSES_BACKEND.md exige
        // * en todo el resto de la API.
        String body = """
                {"status":"failed","data":null,"message":"Demasiadas solicitudes, intentá de nuevo en unos segundos","timestamp":"%s","error":"ERR_SYS_02"}"""
                .formatted(Instant.now().toString());
        response.getWriter().write(body);
    }
}
