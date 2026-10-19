package com.drivehub.identidad.infrastructure.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.port.out.GeneradorTokenOpacoPort;

/**
 * Token opaco de la sesión renovable: 32 bytes aleatorios; en la base solo
 * queda su SHA-256 en hexadecimal (64 caracteres, igual que `token_hash`).
 */
@Component
public class TokenOpacoAdapter implements GeneradorTokenOpacoPort {

    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public String generar() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hashear(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
