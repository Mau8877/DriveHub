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

        void usuario(@CurrentUserId UUID usuario, @CurrentUserId String noEsUuid) {
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

    // ------------------------------------------------------------------ @CurrentUserId

    private MethodParameter parametroUsuario(int indice) throws NoSuchMethodException {
        return new MethodParameter(Ejemplo.class.getDeclaredMethod("usuario", UUID.class, String.class), indice);
    }

    private void autenticarConSub(String sub) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").claim("escuela", escuela.toString())
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (sub != null) {
            builder.subject(sub);
        } else {
            builder.claim("otro", "x");
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(builder.build()));
    }

    @Test
    void currentUserId_soloParaUuidConLaAnotacion() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(parametroUsuario(0))).isTrue();
        assertThat(resolver.supportsParameter(parametroUsuario(1))).isFalse();
    }

    @Test
    void currentUserId_devuelveElSubComoUuid() throws NoSuchMethodException {
        UUID persona = UUID.randomUUID();
        autenticarConSub(persona.toString());

        assertThat(resolver.resolveArgument(parametroUsuario(0), null, null, null)).isEqualTo(persona);
    }

    @Test
    void currentUserId_conUnSubQueNoEsUuidOSinSub_esUnaSesionInvalida() {
        autenticarConSub("no-es-un-uuid");
        assertThatThrownBy(() -> resolver.resolveArgument(parametroUsuario(0), null, null, null))
            .isInstanceOfSatisfying(UnauthorizedException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(PagosErrorCodes.SESION_INVALIDA));

        autenticarConSub(null);
        assertThatThrownBy(() -> resolver.resolveArgument(parametroUsuario(0), null, null, null))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void currentUserId_sinUnJwt_esUnaSesionInvalida() {
        assertThatThrownBy(() -> resolver.resolveArgument(parametroUsuario(0), null, null, null))
            .isInstanceOf(UnauthorizedException.class);
    }
}
