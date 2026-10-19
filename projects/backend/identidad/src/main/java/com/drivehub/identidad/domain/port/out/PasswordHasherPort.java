package com.drivehub.identidad.domain.port.out;

public interface PasswordHasherPort {

    String hashear(String passwordEnClaro);
}
