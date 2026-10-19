package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.drivehub.identidad.infrastructure.adapter.out.security.JwtEmisorAdapter;

import lombok.RequiredArgsConstructor;

/**
 * Claves públicas con las que se verifican los JWT (JWKS). Es un endpoint de
 * infraestructura, como /health: sin sobre estándar y fuera de /api/v1.
 * El gateway y los demás servicios lo consultan directo, por la red interna.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtEmisorAdapter emisor;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(emisor.jwkSetPublico().toJSONObject(true));
    }
}
