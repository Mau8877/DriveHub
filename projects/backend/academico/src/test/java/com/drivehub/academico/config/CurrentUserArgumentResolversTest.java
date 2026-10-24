package com.drivehub.academico.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.drivehub.academico.common.exception.UnauthorizedException;

class CurrentUserArgumentResolversTest {

    @SuppressWarnings("unused")
    static class Ejemplo {
        void metodo(@CurrentUserId UUID usuario, @CurrentEscuelaId UUID escuela, UUID sinAnotacion,
                @CurrentUserId String noEsUuid) {
        }
    }

    private final CurrentUserArgumentResolvers resolver = new CurrentUserArgumentResolvers();
    private final UUID usuario = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();
    private Method metodo;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        metodo = Ejemplo.class.getDeclaredMethod("metodo", UUID.class, UUID.class, UUID.class, String.class);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private MethodParameter parametro(int indice) {
        return new MethodParameter(metodo, indice);
    }

    private void autenticarConToken(String subject, String escuelaClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").subject(subject)
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (escuelaClaim != null) {
            builder.claim("escuela", escuelaClaim);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(builder.build()));
    }

    @Test
    void supportsParameter_soloParaUuidConLaAnotacion() {
        assertThat(resolver.supportsParameter(parametro(0))).isTrue();
        assertThat(resolver.supportsParameter(parametro(1))).isTrue();
        assertThat(resolver.supportsParameter(parametro(2))).isFalse();
        assertThat(resolver.supportsParameter(parametro(3))).isFalse();
    }

    @Test
    void resolveArgument_currentUserId_devuelveElSubComoUuid() {
        autenticarConToken(usuario.toString(), escuela.toString());

        Object resuelto = resolver.resolveArgument(parametro(0), null, null, null);

        assertThat(resuelto).isEqualTo(usuario);
    }

    @Test
    void resolveArgument_currentEscuelaId_devuelveElClaimEscuela() {
        autenticarConToken(usuario.toString(), escuela.toString());

        Object resuelto = resolver.resolveArgument(parametro(1), null, null, null);

        assertThat(resuelto).isEqualTo(escuela);
    }

    @Test
    void resolveArgument_currentEscuelaId_sinElClaimDevuelveNull() {
        autenticarConToken(usuario.toString(), null);

        Object resuelto = resolver.resolveArgument(parametro(1), null, null, null);

        assertThat(resuelto).isNull();
    }

    @Test
    void resolveArgument_sinAutenticacion_lanzaUnauthorizedConErrSys04() {
        assertThatThrownBy(() -> resolver.resolveArgument(parametro(0), null, null, null))
            .isInstanceOfSatisfying(UnauthorizedException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_04"));
    }

    @Test
    void resolveArgument_autenticacionQueNoEsJwt_lanzaUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alguien", "x"));

        assertThatThrownBy(() -> resolver.resolveArgument(parametro(0), null, null, null))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void resolveArgument_subQueNoEsUuid_lanzaUnauthorized() {
        autenticarConToken("no-es-un-uuid", null);

        assertThatThrownBy(() -> resolver.resolveArgument(parametro(0), null, null, null))
            .isInstanceOf(UnauthorizedException.class);
    }
}
