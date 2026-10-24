package com.drivehub.academico.config;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.drivehub.academico.features.preguntas.enums.TipoPregunta;
import com.drivehub.academico.features.preguntas.model.OpcionPregunta;
import com.drivehub.academico.features.preguntas.model.Pregunta;
import com.drivehub.academico.features.preguntas.repository.PreguntaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Preguntas de ejemplo para desarrollo: el banco de `escuelaexample`, con los tres tipos, varios temas
 * y las tres dificultades.
 *
 * ! Solo con el perfil `dev`. Nunca se siembra en producción.
 * * El codigo de la escuela es el fijo que siembran Escuela e Identidad (EscuelaSeeder): Académico no
 * * llama a ningún servicio para saberlo. Se siembra por enunciado: si ya existe, no se toca.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class PreguntaSeeder implements ApplicationRunner {

    public static final UUID CODIGO_ESCUELA_EXAMPLE = UUID.fromString("0198f0a0-0000-7000-8000-000000000001");
    // * Persona que figura como autora de las preguntas de ejemplo (el adminexample sembrado en Identidad no
    // * tiene codigo fijo, así que es un valor inventado y reconocible)
    private static final UUID AUTOR_EJEMPLO = UUID.fromString("0198f0a0-0000-7000-8000-0000000000aa");

    private static final Logger LOG = LoggerFactory.getLogger(PreguntaSeeder.class);

    private record Ejemplo(String categoria, String tema, TipoPregunta tipo, String enunciado, int dificultad,
            String explicacion, List<OpcionPregunta> opciones) {
    }

    private static OpcionPregunta op(String id, String texto, boolean correcta) {
        return new OpcionPregunta(id, texto, correcta);
    }

    private static final List<Ejemplo> EJEMPLOS = List.of(
        new Ejemplo("GENERAL", "Señales", TipoPregunta.OPCION_UNICA,
            "¿Qué indica una señal octogonal roja con la palabra PARE?", 1,
            "Obliga a detener por completo el vehículo antes de la línea de detención.",
            List.of(op("A", "Detenerse por completo y ceder el paso", true),
                op("B", "Reducir la velocidad y continuar", false),
                op("C", "Tocar la bocina antes de cruzar", false))),
        new Ejemplo("GENERAL", "Señales", TipoPregunta.VERDADERO_FALSO,
            "Las señales preventivas tienen forma de rombo y fondo amarillo.", 1,
            "Advierten de un peligro adelante y son romboidales amarillas.",
            List.of(op("A", "Verdadero", true), op("B", "Falso", false))),
        new Ejemplo("GENERAL", "Señales", TipoPregunta.OPCION_MULTIPLE,
            "¿Cuáles de estas son señales reglamentarias?", 2,
            "Las reglamentarias prohíben u obligan; las informativas solo orientan.",
            List.of(op("A", "Prohibido estacionar", true), op("B", "Velocidad máxima", true),
                op("C", "Hospital cercano", false), op("D", "Curva peligrosa", false))),
        new Ejemplo("GENERAL", "Prioridad de paso", TipoPregunta.OPCION_UNICA,
            "En una intersección sin señales, ¿quién tiene la prioridad?", 2,
            "Sin señalización, cede el paso quien tiene al otro vehículo a su derecha.",
            List.of(op("A", "El vehículo que viene por la derecha", true),
                op("B", "El vehículo más grande", false),
                op("C", "El que llegó con más velocidad", false))),
        new Ejemplo("GENERAL", "Prioridad de paso", TipoPregunta.VERDADERO_FALSO,
            "Un vehículo de emergencia con sirena activa tiene prioridad sobre el resto.", 1,
            "Hay que facilitarle el paso orillándose con precaución.",
            List.of(op("A", "Verdadero", true), op("B", "Falso", false))),
        new Ejemplo("B", "Seguridad vial", TipoPregunta.OPCION_UNICA,
            "¿Cuál es la distancia de seguridad recomendada en carretera seca?", 2,
            "La regla de los dos segundos da margen para frenar a tiempo.",
            List.of(op("A", "Dos segundos respecto al vehículo de adelante", true),
                op("B", "Medio segundo", false), op("C", "Un auto de largo", false),
                op("D", "No hay distancia recomendada", false))),
        new Ejemplo("B", "Seguridad vial", TipoPregunta.OPCION_MULTIPLE,
            "¿Qué debes revisar antes de iniciar un viaje largo?", 3,
            "La revisión previa evita fallas y accidentes en la vía.",
            List.of(op("A", "Presión de las llantas", true), op("B", "Nivel de aceite y refrigerante", true),
                op("C", "Luces y frenos", true), op("D", "El color de la carrocería", false))),
        new Ejemplo("B", "Seguridad vial", TipoPregunta.VERDADERO_FALSO,
            "El cinturón de seguridad solo es obligatorio para el conductor.", 1,
            "Es obligatorio para todos los ocupantes del vehículo.",
            List.of(op("A", "Verdadero", false), op("B", "Falso", true))),
        new Ejemplo("B", "Mecánica básica", TipoPregunta.OPCION_UNICA,
            "¿Qué indica una luz roja de aceite encendida en el tablero?", 3,
            "Es una falla grave de lubricación: hay que detener el motor.",
            List.of(op("A", "Presión de aceite insuficiente", true),
                op("B", "Cambio de aceite próximo", false), op("C", "Combustible bajo", false))),
        new Ejemplo("A", "Motocicletas", TipoPregunta.OPCION_UNICA,
            "¿Qué equipo es obligatorio para conducir una motocicleta?", 1,
            "El casco homologado protege del daño en caso de caída.",
            List.of(op("A", "Casco homologado", true), op("B", "Guantes de cuero", false),
                op("C", "Chaleco naranja", false))),
        new Ejemplo("A", "Motocicletas", TipoPregunta.OPCION_MULTIPLE,
            "¿Qué ayuda a ser más visible para otros conductores en una moto?", 2,
            "Mientras más visible, menor el riesgo de no ser visto.",
            List.of(op("A", "Luces encendidas de día", true), op("B", "Ropa de colores claros o reflectiva", true),
                op("C", "Circular en el punto ciego de los autos", false))),
        new Ejemplo("C", "Transporte de carga", TipoPregunta.VERDADERO_FALSO,
            "La carga de un camión puede sobresalir sin señalizar si es corta.", 3,
            "Cualquier saliente debe señalizarse con bandera o luz.",
            List.of(op("A", "Verdadero", false), op("B", "Falso", true))));

    private final PreguntaRepository preguntaRepository;

    @Override
    public void run(ApplicationArguments args) {
        int creadas = 0;
        for (Ejemplo e : EJEMPLOS) {
            if (preguntaRepository.existsByEscuelaCodigoAndEnunciado(CODIGO_ESCUELA_EXAMPLE, e.enunciado())) {
                continue;
            }
            Pregunta pregunta = new Pregunta();
            pregunta.setEscuelaCodigo(CODIGO_ESCUELA_EXAMPLE);
            pregunta.setCreadaPorCodigo(AUTOR_EJEMPLO);
            pregunta.setCategoria(e.categoria());
            pregunta.setTema(e.tema());
            pregunta.setTipo(e.tipo());
            pregunta.setEnunciado(e.enunciado());
            pregunta.setOpciones(e.opciones());
            pregunta.setExplicacion(e.explicacion());
            pregunta.setDificultad(e.dificultad());
            pregunta.setActiva(true);
            preguntaRepository.save(pregunta);
            creadas++;
        }
        if (creadas > 0) {
            LOG.info("Preguntas de ejemplo creadas para desarrollo ({})", creadas);
        }
    }
}
