package com.drivehub.identidad.domain.port.out;

public interface GeneradorTokenOpacoPort {

    /** Token aleatorio e impredecible para la sesión renovable. */
    String generar();

    /** Hash que se guarda en la base (nunca el token en claro). */
    String hashear(String token);
}
