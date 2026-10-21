package com.drivehub.pagos.config;

import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * El claim `roles` del JWT se vuelve autoridades `ROLE_{nombre}`: cada servicio
 * arma el principal desde el token, sin llamar a Identidad en cada petición.
 */
public class JwtRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter delegado = new JwtAuthenticationConverter();

    public JwtRolesConverter() {
        delegado.setJwtGrantedAuthoritiesConverter(this::autoridades);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return delegado.convert(jwt);
    }

    private Collection<GrantedAuthority> autoridades(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream().<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
    }
}
