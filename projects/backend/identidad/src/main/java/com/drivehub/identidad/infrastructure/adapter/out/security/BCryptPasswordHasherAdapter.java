package com.drivehub.identidad.infrastructure.adapter.out.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.port.out.PasswordHasherPort;

import lombok.RequiredArgsConstructor;

/**
 * Hashea contraseñas con BCrypt (vía el PasswordEncoder de Spring Security).
 */
@Component
@RequiredArgsConstructor
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hashear(String passwordEnClaro) {
        return passwordEncoder.encode(passwordEnClaro);
    }

    @Override
    public boolean coincide(String passwordEnClaro, String hash) {
        return passwordEncoder.matches(passwordEnClaro, hash);
    }
}
