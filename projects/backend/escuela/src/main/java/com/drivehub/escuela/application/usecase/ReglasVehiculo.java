package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Year;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ValidationException;
import com.drivehub.escuela.domain.entity.Vehiculo;

/**
 * Reglas de datos de un auto que comparten registrar y editar (CU07). Los mismos límites que
 * validan el DTO de la petición, la pantalla y el esquema de la base.
 */
final class ReglasVehiculo {

    private static final Logger LOG = LoggerFactory.getLogger(ReglasVehiculo.class);
    static final String MODULO = "vehiculos";
    static final Pattern PLACA = Pattern.compile("^[A-Z0-9-]{5,10}$");
    static final int ANIO_MINIMO = 1980;

    private ReglasVehiculo() {
    }

    static String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }

    static void texto(String valor, String nombre, int maximo) {
        if (valor == null || valor.isEmpty() || valor.length() > maximo) {
            throw invalido("La " + nombre + " es obligatoria y no puede superar " + maximo + " caracteres");
        }
    }

    static void colorOpcional(String color) {
        if (color != null && color.length() > 20) {
            throw invalido("El color no puede superar 20 caracteres");
        }
    }

    static void anio(Short anio) {
        if (anio != null && (anio < ANIO_MINIMO || anio > Year.now().getValue() + 1)) {
            throw invalido("El año debe estar entre " + ANIO_MINIMO + " y " + (Year.now().getValue() + 1));
        }
    }

    static void kilometraje(Integer kilometraje) {
        if (kilometraje != null && kilometraje < 0) {
            throw invalido("El kilometraje no puede ser negativo");
        }
    }

    static ValidationException invalido(String mensaje) {
        LOG.warn("Auto rechazado: datos inválidos", kv("module", MODULO), kv("errorCode", EscuelaErrorCodes.VALIDACION));
        return new ValidationException(EscuelaErrorCodes.VALIDACION, mensaje);
    }

    static VehiculoOutput aSalida(Vehiculo v) {
        return new VehiculoOutput(v.getCodigo(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAnio(), v.getColor(),
            v.getTransmision(), v.getEstado(), v.getKilometraje(), v.getSoatVence(), v.getInspeccionVence(),
            v.getCreadoEn());
    }
}
