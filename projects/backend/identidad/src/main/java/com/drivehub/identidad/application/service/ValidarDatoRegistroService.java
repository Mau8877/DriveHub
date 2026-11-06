package com.drivehub.identidad.application.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.drivehub.identidad.domain.model.Ci;
import com.drivehub.identidad.domain.port.in.DatoRegistro;
import com.drivehub.identidad.domain.port.in.ValidarDatoRegistroUseCase;

import lombok.RequiredArgsConstructor;

/**
 * CU48: las reglas de un dato del registro que dependen de los datos guardados o de la fecha. Son las de
 * {@link AltaCuenta}, la misma que usa el registro normal: no hay una copia.
 */
@Service
@RequiredArgsConstructor
public class ValidarDatoRegistroService implements ValidarDatoRegistroUseCase {

    private final AltaCuenta altaCuenta;

    @Override
    public void validar(DatoRegistro dato) {
        switch (dato.tipo()) {
            case FECHA_NACIMIENTO -> altaCuenta.verificarEdadMinima(dato.comoFecha());
            case CORREO -> altaCuenta.verificarCorreoLibre(dato.valor().trim().toLowerCase(Locale.ROOT));
            case CI -> altaCuenta.verificarCiLibre(Ci.normalizar(dato.valor()));
        }
    }
}
