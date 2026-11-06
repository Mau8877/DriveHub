package com.drivehub.identidad.domain.port.in;

public record ConfirmarCodigoWhatsappComando(String telefono, String correo, String codigo) {
}
