package com.drivehub.escuela.infrastructure.client;

import java.util.function.Supplier;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.ForbiddenException;
import com.drivehub.escuela.common.exception.LockedException;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.common.exception.UnauthorizedException;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.gateway.IdentidadNoDisponibleException;

/**
 * Lo que comparten los clientes REST de Escuela hacia Identidad: el Bearer de la sesión y la
 * traducción de los errores.
 *
 * * Un 4xx de negocio de Identidad (con su sobre `{error, message}`) se relanza como la excepción
 * * equivalente con su MISMO código y mensaje, así el formulario muestra el motivo real.
 * * Un 5xx, un 429, un timeout o un error de red lanzan IdentidadNoDisponibleException: el resultado
 * * allá es incierto y el caso de uso compensa.
 */
final class TraductorDeErroresDeIdentidad {

    private TraductorDeErroresDeIdentidad() {
    }

    /** El Bearer de la persona que hizo la petición: Identidad vuelve a validar el token y el rol. */
    static String bearerDeLaSesion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            return "Bearer " + jwt.getToken().getTokenValue();
        }
        throw new IllegalStateException("No hay una sesión con token para llamar a Identidad");
    }

    /** Ejecuta la llamada y traduce cualquier falla del cliente HTTP. */
    static <T> T ejecutar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientResponseException e) {
            throw traducir(e);
        } catch (RestClientException e) {
            throw new IdentidadNoDisponibleException("No se pudo hablar con Identidad", e);
        }
    }

    static RuntimeException traducir(RestClientResponseException e) {
        int estado = e.getStatusCode().value();
        SobreError sobre = leerError(e);
        boolean noEsDeNegocio = estado >= 500 || estado == 429 || sobre == null || sobre.error() == null;
        if (noEsDeNegocio) {
            return new IdentidadNoDisponibleException("Identidad respondió " + estado, e);
        }
        String codigo = sobre.error();
        String mensaje = sobre.message() == null ? "Identidad rechazó la petición" : sobre.message();
        return switch (estado) {
            case 400 -> new ValidationException(codigo, mensaje);
            case 401 -> new UnauthorizedException(codigo, mensaje);
            case 403 -> new ForbiddenException(codigo, mensaje);
            case 404 -> new NotFoundException(codigo, mensaje);
            case 409 -> new ConflictException(codigo, mensaje);
            case 423 -> new LockedException(codigo, mensaje);
            default -> new IdentidadNoDisponibleException("Identidad respondió " + estado, e);
        };
    }

    private static SobreError leerError(RestClientResponseException e) {
        try {
            return e.getResponseBodyAs(SobreError.class);
        } catch (RuntimeException ignorada) {
            return null;
        }
    }

    // * Sobre de error estándar de Identidad; solo se leen estos campos
    private record SobreError(String error, String message) {
    }
}
