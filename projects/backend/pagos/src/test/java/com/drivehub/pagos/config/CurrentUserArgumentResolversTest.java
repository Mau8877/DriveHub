package com.drivehub.pagos.config;

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

import com.drivehub.pagos.common.exception.PagosErrorCodes;
import com.drivehub.pagos.common.exception.UnauthorizedException;

class CurrentUserArgumentResolversTest {

    @SuppressWarnings("unused")
    static class Ejemplo {
        void metodo(@CurrentEscuelaId UUID escuela, UUID sinAnotacion, @CurrentEscuelaId String noEsUuid) {
        }
    }

    private final CurrentUserArgumentResolvers resolver = new CurrentUserArgumentResolvers();
    private final UUID escuela = UUID.randomUUID();
    private Method metodo;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        metodo = Ejemplo.class.getDeclaredMethod("metodo", UUID.class, UUID.class, String.class);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private MethodParameter parametro(int indice) {
        return new MethodParameter(metodo, indice);
    }

    private void autenticarConEscuela(String escuelaClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (escuelaClaim != null) {
            builder.claim("escuela", escuelaClaim);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(builder.build()));
    }

    @Test
    void supportsParameter_soloParaUuidConLaAnotacion() {
        assertThat(resolver.supportsParameter(parametro(0))).isTrue();
        assertThat(resolver.supportsParameter(parametro(1))).isFalse();
        assertThat(resolver.supportsParameter(parametro(2))).isFalse();
    }

    @Test
    void resolveArgument_conElClaimEscuela_loDevuelveComoUuid() {
        autenticarConEscuela(escuela.toString());

        assertThat(resolver.resolveArgument(parametro(0), null, null, null)).isEqualTo(escuela);
    }

    @Test
    void resolveArgument_sinElClaimEscuela_devuelveNull() {
        autenticarConEscuela(null);

        assertThat(resolver.resolveArgument(parametro(0), null, null, null)).isNull();
    }

    @Test
    void resolveArgument_conUnClaimQueNoEsUuid_devuelveNull() {
        autenticarConEscuela("no-es-un-uuid");

        assertThat(resolver.resolveArgument(parametro(0), null, null, null)).isNull();
    }

    @Test
    void resolveArgument_sinJwt_respondeSesionInvalida() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("alguien", "x"));

        assertThatThrownBy(() -> resolver.resolveArgument(parametro(0), null, null, null))
            .isInstanceOf(UnauthorizedException.class)
            .satisfies(e -> assertThat(((UnauthorizedException) e).getErrorCode())
                .isEqualTo(PagosErrorCodes.SESION_INVALIDA));
    }
}
