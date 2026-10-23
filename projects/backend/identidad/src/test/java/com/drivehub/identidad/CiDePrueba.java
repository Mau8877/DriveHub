package com.drivehub.identidad;

import java.util.concurrent.atomic.AtomicLong;

/**
 * CI únicos para las cuentas de prueba (el CI es único en toda la plataforma).
 */
public final class CiDePrueba {

    private static final AtomicLong SECUENCIA = new AtomicLong(System.nanoTime() % 100_000_000L);

    private CiDePrueba() {
    }

    public static String unico() {
        return "T" + SECUENCIA.incrementAndGet();
    }
}
