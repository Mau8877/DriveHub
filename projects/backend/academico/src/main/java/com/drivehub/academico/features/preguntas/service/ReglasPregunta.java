package com.drivehub.academico.features.preguntas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.preguntas.dto.OpcionRequest;
import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.preguntas.model.OpcionPregunta;

/**
 * Reglas de datos de una pregunta del banco (CU19): las mismas que validan el DTO, la pantalla y el
 * esquema de `<dh-gestion>`, más las que cruzan campos y el DTO no puede expresar.
 */
final class ReglasPregunta {

    static final String MODULO = "preguntas";
    static final Pattern CATEGORIA = Pattern.compile("^(GENERAL|[A-Z]{1,3})$");
    static final int MAX_OPCIONES = 6;
    private static final String LETRAS = "ABCDEF";
    private static final Logger LOG = LoggerFactory.getLogger(ReglasPregunta.class);

    private ReglasPregunta() {
    }

    static String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }

    /** Texto obligatorio de `minimo` a `maximo` caracteres, ya recortado. */
    static String texto(String valor, String nombre, int minimo, int maximo) {
        String limpio = limpiar(valor);
        if (limpio == null || limpio.length() < minimo || limpio.length() > maximo) {
            throw invalido("El " + nombre + " debe tener entre " + minimo + " y " + maximo + " caracteres");
        }
        return limpio;
    }

    /** La explicación es opcional: vacía queda como nula. */
    static String explicacion(String valor) {
        String limpia = limpiar(valor);
        if (limpia == null || limpia.isEmpty()) {
            return null;
        }
        if (limpia.length() > 500) {
            throw invalido("La explicación no puede superar 500 caracteres");
        }
        return limpia;
    }

    static String categoria(String valor) {
        String limpia = limpiar(valor);
        if (limpia == null || !CATEGORIA.matcher(limpia).matches()) {
            throw invalido("La categoría debe ser GENERAL o una sigla de hasta 3 letras mayúsculas");
        }
        return limpia;
    }

    static int dificultad(Integer valor) {
        if (valor == null || valor < 1 || valor > 3) {
            throw invalido("La dificultad va de 1 a 3");
        }
        return valor;
    }

    /**
     * Opciones válidas para el tipo, con sus ids (A, B, C...) asignados por orden.
     *
     * * VERDADERO_FALSO: 2 opciones y 1 correcta. OPCION_UNICA: de 2 a 6 y 1 correcta. OPCION_MULTIPLE:
     * * de 2 a 6 y al menos 1 correcta. Las opciones no se repiten.
     */
    static List<OpcionPregunta> opciones(TipoPregunta tipo, List<OpcionRequest> pedidas) {
        if (tipo == null) {
            throw invalido("El tipo de pregunta es obligatorio");
        }
        List<OpcionRequest> lista = pedidas == null ? List.of() : pedidas;
        int maximo = tipo == TipoPregunta.VERDADERO_FALSO ? 2 : MAX_OPCIONES;
        if (lista.size() < 2 || lista.size() > maximo) {
            throw invalido(tipo == TipoPregunta.VERDADERO_FALSO
                ? "Una pregunta de verdadero o falso tiene exactamente 2 opciones"
                : "Una pregunta tiene entre 2 y " + MAX_OPCIONES + " opciones");
        }

        List<OpcionPregunta> opciones = new ArrayList<>();
        List<String> vistas = new ArrayList<>();
        long correctas = 0;
        for (int i = 0; i < lista.size(); i++) {
            String texto = texto(lista.get(i).texto(), "texto de la opción", 1, 200);
            if (vistas.contains(texto.toLowerCase(Locale.ROOT))) {
                throw invalido("Las opciones no pueden repetirse");
            }
            vistas.add(texto.toLowerCase(Locale.ROOT));
            correctas += lista.get(i).correcta() ? 1 : 0;
            opciones.add(new OpcionPregunta(String.valueOf(LETRAS.charAt(i)), texto, lista.get(i).correcta()));
        }
        boolean unaSola = tipo != TipoPregunta.OPCION_MULTIPLE;
        if (unaSola && correctas != 1) {
            throw invalido("Marca exactamente una opción correcta");
        }
        if (!unaSola && correctas < 1) {
            throw invalido("Marca al menos una opción correcta");
        }
        return opciones;
    }

    /** Las opciones ya guardadas, como pedidas: para revalidarlas cuando solo cambia el tipo. */
    static List<OpcionRequest> comoPedidas(List<OpcionPregunta> guardadas) {
        return guardadas == null ? List.of()
            : guardadas.stream().map(o -> new OpcionRequest(o.texto(), o.correcta())).toList();
    }

    static ValidationException invalido(String mensaje) {
        LOG.warn("Pregunta rechazada: datos inválidos",
            kv("module", MODULO), kv("errorCode", AcademicoErrorCodes.VALIDACION));
        return new ValidationException(AcademicoErrorCodes.VALIDACION, mensaje);
    }
}
