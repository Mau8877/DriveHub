package com.drivehub.pagos.config;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.drivehub.pagos.common.exception.PagosErrorCodes;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * La puerta de las rutas internas de Pagos (CU44): `/internal/**` solo la pasa quien manda la credencial de servicio
 * en el header `X-Service-Key`.
 *
 * * Estas rutas las llama un proceso de otro servicio (el orquestador de Académico) que no tiene la sesión de ningún
 * * estudiante, así que la credencial reemplaza al JWT. Nunca se registran en el gateway.
 * * La comparación es en tiempo constante (`MessageDigest.isEqual`), no con `equals`, para que no se pueda adivinar
 * * la credencial midiendo cuánto tarda la respuesta.
 * * Falla cerrado: si no hay credencial configurada, no pasa nadie (ni siquiera con un header vacío).
 * * Se escribe el sobre de error a mano: este filtro corre antes de cualquier handler de Spring MVC.
 * ! Nunca se registra el valor recibido ni el configurado; solo la ruta.
 */
public class CredencialServicioFilter extends OncePerRequestFilter {

    static final String CABECERA = "X-Service-Key";
    static final String PREFIJO = "/internal/";

    private static final Logger LOG = LoggerFactory.getLogger(CredencialServicioFilter.class);
    private static final String MENSAJE = "Credencial de servicio inválida";

    private final byte[] credencial;

    /**
     * @param credencial la credencial configurada; vacía o nula significa «no hay»: se rechaza todo
     */
    public CredencialServicioFilter(String credencial) {
        this.credencial = credencial == null ? new byte[0] : credencial.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIJO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String recibida = request.getHeader(CABECERA);
        if (credencial.length > 0 && recibida != null
                && MessageDigest.isEqual(credencial, recibida.getBytes(StandardCharsets.UTF_8))) {
            cadena.doFilter(request, response);
            return;
        }
        LOG.warn("Petición interna rechazada: credencial de servicio ausente o inválida", kv("module", "internal"),
            kv("ruta", request.getRequestURI()));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":\"failed\",\"message\":\"" + MENSAJE + "\",\"timestamp\":\""
            + Instant.now() + "\",\"error\":\"" + PagosErrorCodes.CREDENCIAL_INVALIDA + "\"}");
    }
}
