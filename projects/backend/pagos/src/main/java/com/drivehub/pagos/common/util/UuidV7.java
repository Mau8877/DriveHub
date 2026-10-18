package com.drivehub.pagos.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generador de UUID versión 7 (RFC 9562): 48 bits de timestamp en ms +
 * bits aleatorios.
 *
 * * Es el formato de todos los identificadores públicos del proyecto
 * * (columnas codigo_{tabla}). Se usa v7 y no v4 porque es ordenable por
 * * tiempo: el índice único de la columna crece "al final" en vez de
 * * fragmentarse con valores aleatorios en cada insert.
 * * Java no trae generador v7 (UUID.randomUUID() es v4), por eso existe esta clase.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generar() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);

        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) (timestamp >>> (40 - 8 * i));
        }
        bytes[6] = (byte) ((bytes[6] & 0x0F) | 0x70);
        bytes[8] = (byte) ((bytes[8] & 0x3F) | 0x80);

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xFF);
        }
        return new UUID(msb, lsb);
    }
}
