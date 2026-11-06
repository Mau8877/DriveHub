package com.drivehub.identidad.infrastructure.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.port.out.HasheadorWhatsappPort;

/**
 * HMAC-SHA256 con la clave secreta `app.whatsapp.hash-secret` (CU47): da 64 caracteres hexadecimales, igual
 * que las columnas `*_hash`. Cada tipo de dato lleva su propio prefijo para que un hash de número nunca
 * pueda pasar por uno de correo o de código.
 * ! La clave no se escribe en los registros.
 */
@Component
public class HasheadorWhatsappAdapter implements HasheadorWhatsappPort {

    private static final String ALGORITMO = "HmacSHA256";

    private final byte[] clave;

    public HasheadorWhatsappAdapter(@Value("${app.whatsapp.hash-secret}") String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("Falta app.whatsapp.hash-secret (WHATSAPP_HASH_SECRET)");
        }
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String hashearTelefono(String telefono) {
        return hmac("tel:" + telefono.replaceAll("\\D", ""));
    }

    @Override
    public String hashearCorreo(String correo) {
        return hmac("correo:" + correo);
    }

    @Override
    public String hashearCodigo(String codigo) {
        return hmac("codigo:" + codigo);
    }

    private String hmac(String dato) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(clave, ALGORITMO));
            return HexFormat.of().formatHex(mac.doFinal(dato.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }
}
