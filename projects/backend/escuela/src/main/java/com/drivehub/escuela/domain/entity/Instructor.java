package com.drivehub.escuela.domain.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.ValidationException;

import lombok.Builder;
import lombok.Getter;

/**
 * Ficha profesional del docente (CU09).
 *
 * * `usuarioCodigo` es el codigo_usuario de Identidad (la persona). Su CI y sus datos personales
 * * viven en el perfil de Identidad (CU08a).
 * * `escuelaCodigo` es la escuela a la que pertenece la ficha: no cambia nunca.
 * * Las reglas de la ficha viven acá y no dependen del reloj del sistema: la fecha de hoy se la pasa
 * * quien las usa. Una regla incumplida lanza `ValidationException` `ERR_SYS_01`.
 */
@Getter
@Builder(toBuilder = true)
public class Instructor {

    /** Sigla de "sin categoría": existe en el catálogo pero un instructor no la puede enseñar. */
    public static final String SIN_CATEGORIA = "SIN";

    private final UUID codigo;
    private final UUID usuarioCodigo;
    private final UUID escuelaCodigo;
    private final String nroLicencia;
    private final LocalDate licenciaVence;
    private final EstadoInstructor estado;
    private final LocalDate fechaContratacion;
    // * En el orden del catálogo de categorías
    private final Set<CategoriaLicencia> categorias;
    private final Instant creadoEn;

    /**
     * Una ficha nueva: estado ACTIVO, con las reglas del alta aplicadas.
     *
     * * La licencia vence después de hoy; la contratación no es futura (sin ella, hoy); lleva al
     * * menos una categoría y ninguna es "sin categoría".
     *
     * @param nroLicencia ya normalizado y validado con {@link NroLicencia#normalizarYValidar}
     */
    public static Instructor nuevo(UUID codigo, UUID usuarioCodigo, UUID escuelaCodigo, String nroLicencia,
            LocalDate licenciaVence, LocalDate fechaContratacion, Set<CategoriaLicencia> categorias, LocalDate hoy) {
        validarVencimiento(licenciaVence, hoy);
        return Instructor.builder().codigo(codigo).usuarioCodigo(usuarioCodigo).escuelaCodigo(escuelaCodigo)
            .nroLicencia(nroLicencia).licenciaVence(licenciaVence).estado(EstadoInstructor.ACTIVO)
            .fechaContratacion(fechaContratacion == null ? hoy : validarContratacion(fechaContratacion, hoy))
            .categorias(categorias).build();
    }

    /** La licencia tiene que vencer después de hoy (obligatoria). */
    public static void validarVencimiento(LocalDate licenciaVence, LocalDate hoy) {
        if (licenciaVence == null) {
            throw rechazo("La fecha de vencimiento de la licencia es obligatoria");
        }
        if (!licenciaVence.isAfter(hoy)) {
            throw rechazo("La licencia tiene que vencer después de hoy");
        }
    }

    /** La contratación no puede ser futura. Devuelve la misma fecha. */
    public static LocalDate validarContratacion(LocalDate fechaContratacion, LocalDate hoy) {
        if (fechaContratacion.isAfter(hoy)) {
            throw rechazo("La fecha de contratación no puede ser futura");
        }
        return fechaContratacion;
    }

    /**
     * Normaliza las siglas elegidas (mayúsculas, sin espacios): al menos una y sin repetir.
     * Que existan en el catálogo y que no sea "sin categoría" lo comprueba quien tiene el catálogo.
     */
    public static List<String> validarSiglas(Collection<String> siglas) {
        if (siglas == null || siglas.isEmpty()) {
            throw rechazo("El instructor tiene que enseñar al menos una categoría");
        }
        Set<String> normalizadas = new LinkedHashSet<>();
        for (String sigla : siglas) {
            String normalizada = sigla == null ? "" : sigla.trim().toUpperCase(Locale.ROOT);
            if (normalizada.isEmpty() || !normalizadas.add(normalizada)) {
                throw rechazo("Las categorías no pueden estar vacías ni repetirse");
            }
        }
        return List.copyOf(normalizadas);
    }

    private static ValidationException rechazo(String mensaje) {
        return new ValidationException(EscuelaErrorCodes.VALIDACION, mensaje);
    }
}
