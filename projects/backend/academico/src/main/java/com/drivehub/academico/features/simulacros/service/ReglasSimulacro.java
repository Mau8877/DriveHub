package com.drivehub.academico.features.simulacros.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.simulacros.dto.ReglaRequest;
import com.drivehub.academico.features.simulacros.model.ReglaSimulacro;

/**
 * Reglas de datos de un simulacro (CU20): las mismas que validan el DTO, la pantalla y el esquema de
 * `<dh-gestion>`, más las que cruzan campos y el DTO no puede expresar.
 */
final class ReglasSimulacro {

    static final String MODULO = "simulacros";
    static final Pattern CATEGORIA = Pattern.compile("^(GENERAL|[A-Z]{1,3})$");
    static final int MAX_REGLAS = 10;
    private static final Logger LOG = LoggerFactory.getLogger(ReglasSimulacro.class);

    private ReglasSimulacro() {
    }

    static String nombre(String valor) {
        String limpio = valor == null ? null : valor.trim();
        if (limpio == null || limpio.length() < 3 || limpio.length() > 80) {
            throw invalido("El nombre debe tener entre 3 y 80 caracteres");
        }
        return limpio;
    }

    static String categoria(String valor) {
        String limpia = valor == null ? null : valor.trim();
        if (limpia == null || !CATEGORIA.matcher(limpia).matches()) {
            throw invalido("La categoría debe ser GENERAL o una sigla de hasta 3 letras mayúsculas");
        }
        return limpia;
    }

    static int duracion(Integer valor) {
        if (valor == null || valor < 5 || valor > 180) {
            throw invalido("La duración va de 5 a 180 minutos");
        }
        return valor;
    }

    static int nroPreguntas(Integer valor) {
        if (valor == null || valor < 1 || valor > 100) {
            throw invalido("El número de preguntas va de 1 a 100");
        }
        return valor;
    }

    static double puntaje(Double valor) {
        if (valor == null || valor < 1 || valor > 100) {
            throw invalido("El puntaje de aprobación va de 1 a 100");
        }
        return valor;
    }

    /**
     * Reglas válidas: de 1 a 10 temas distintos (sin distinguir mayúsculas), cada uno de 2 a 80 caracteres y de 1
     * a 100 preguntas, que sumen exactamente `nroPreguntas`.
     *
     * * El tema se compara con el de las preguntas del banco (CU19): se guarda recortado, tal como se escribe.
     */
    static List<ReglaSimulacro> reglas(List<ReglaRequest> pedidas, int nroPreguntas) {
        if (pedidas == null || pedidas.isEmpty() || pedidas.size() > MAX_REGLAS) {
            throw invalido("Agrega de 1 a " + MAX_REGLAS + " temas");
        }
        Set<String> vistos = new HashSet<>();
        List<ReglaSimulacro> reglas = new ArrayList<>();
        int suma = 0;
        for (ReglaRequest pedida : pedidas) {
            String tema = pedida.tema() == null ? null : pedida.tema().trim();
            if (tema == null || tema.length() < 2 || tema.length() > 80) {
                throw invalido("El tema debe tener entre 2 y 80 caracteres");
            }
            if (pedida.cantidad() == null || pedida.cantidad() < 1 || pedida.cantidad() > 100) {
                throw invalido("La cantidad de cada tema va de 1 a 100");
            }
            if (!vistos.add(tema.toLowerCase(Locale.ROOT))) {
                throw invalido("Un tema no puede repetirse");
            }
            suma += pedida.cantidad();
            reglas.add(new ReglaSimulacro(tema, pedida.cantidad()));
        }
        if (suma != nroPreguntas) {
            throw invalido("Las preguntas por tema suman " + suma + " y el simulacro tiene " + nroPreguntas);
        }
        return reglas;
    }

    /** Las reglas ya guardadas, como pedidas: para revalidarlas cuando solo cambia el número de preguntas. */
    static List<ReglaRequest> comoPedidas(List<ReglaSimulacro> guardadas) {
        return guardadas == null ? List.of()
            : guardadas.stream().map(r -> new ReglaRequest(r.tema(), r.cantidad())).toList();
    }

    static ValidationException invalido(String mensaje) {
        LOG.warn("Simulacro rechazado: datos inválidos",
            kv("module", MODULO), kv("errorCode", AcademicoErrorCodes.VALIDACION));
        return new ValidationException(AcademicoErrorCodes.VALIDACION, mensaje);
    }
}
