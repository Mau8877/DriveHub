package com.drivehub.identidad.domain.port.out;

public interface PasswordHasherPort {

    String hashear(String passwordEnClaro);

    /** Si la contraseña en claro corresponde al hash guardado. */
    boolean coincide(String passwordEnClaro, String hash);
}
