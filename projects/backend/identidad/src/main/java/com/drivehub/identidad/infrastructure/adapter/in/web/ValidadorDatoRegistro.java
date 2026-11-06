package com.drivehub.identidad.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.SexoPersona;
import com.drivehub.identidad.domain.port.in.DatoRegistro;
import com.drivehub.identidad.domain.port.in.ValidarDatoRegistroUseCase;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * El formato de un dato del registro por WhatsApp (CU48), con las MISMAS anotaciones del formulario de registro
 * ({@link RegistrarEstudianteRequest}): `Validator.validateValue` las aplica a un solo campo, así que las
 * reglas no se copian y no pueden desalinearse. Lo que depende de los datos guardados o de la fecha (edad mínima,
 * correo y CI sin usar) lo resuelve después el caso de uso.
 */
@Component
public class ValidadorDatoRegistro {

    private static final String CODIGO = "ERR_SYS_01";

    private final Validator validator;
    private final ValidarDatoRegistroUseCase validarDato;

    public ValidadorDatoRegistro(Validator validator, ValidarDatoRegistroUseCase validarDato) {
        this.validator = validator;
        this.validarDato = validarDato;
    }

    public void validar(String campo, String valor) {
        String texto = valor.trim();
        switch (campo) {
            case "nombres" -> formato("nombres", texto);
            case "apellidos" -> formato("apellidos", texto);
            case "ci" -> {
                formato("ci", texto);
                validarDato.validar(new DatoRegistro(DatoRegistro.Tipo.CI, texto));
            }
            case "correo" -> {
                formato("email", texto);
                validarDato.validar(new DatoRegistro(DatoRegistro.Tipo.CORREO, texto));
            }
            case "fechaNacimiento" -> {
                formato("fechaNacimiento", fecha(texto));
                validarDato.validar(new DatoRegistro(DatoRegistro.Tipo.FECHA_NACIMIENTO, texto));
            }
            case "sexo" -> formato("sexo", sexo(texto));
            case "ubicacion" -> ubicacion(texto);
            default -> throw new ValidationException(CODIGO, "El campo «" + campo + "» no se puede validar");
        }
    }

    private void formato(String propiedad, Object valor) {
        Set<ConstraintViolation<RegistrarEstudianteRequest>> errores =
            validator.validateValue(RegistrarEstudianteRequest.class, propiedad, valor);
        errores.stream().map(ConstraintViolation::getMessage).sorted().findFirst().ifPresent(mensaje -> {
            throw new ValidationException(CODIGO, mensaje);
        });
    }

    private LocalDate fecha(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new ValidationException(CODIGO, "La fecha de nacimiento no es válida (usa año-mes-día)");
        }
    }

    private SexoPersona sexo(String texto) {
        try {
            return SexoPersona.valueOf(texto.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(CODIGO, "El sexo debe ser masculino o femenino");
        }
    }

    private void ubicacion(String texto) {
        String[] partes = texto.split(",");
        if (partes.length != 2) {
            throw new ValidationException(CODIGO, "La ubicación no es válida");
        }
        try {
            formato("latitud", new BigDecimal(partes[0].trim()));
            formato("longitud", new BigDecimal(partes[1].trim()));
        } catch (NumberFormatException e) {
            throw new ValidationException(CODIGO, "La ubicación no es válida");
        }
    }
}
