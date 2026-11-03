package com.drivehub.academico.features.examenes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.drivehub.academico.common.enums.ResultadoEvaluacion;
import com.drivehub.academico.common.notificaciones.NotificacionesClient;
import com.drivehub.academico.features.examenes.model.ExamenConduccion;

import lombok.RequiredArgsConstructor;

/**
 * Avisa al estudiante lo que pasa con su examen de conducción (CU36): programado, reprogramado, cancelado, sin
 * asistencia y resultado.
 *
 * * Se llama DESPUÉS de guardar el cambio. Nunca propaga un error: el aviso es de mejor esfuerzo y no puede deshacer
 * * lo que el administrador acaba de hacer.
 * * El `eventoCodigo` es determinista por (evento, examen, fecha programada): repetir el mismo aviso no lo duplica,
 * * y una reprogramación a otra fecha sí avisa de nuevo.
 */
@Component
@RequiredArgsConstructor
public class AvisosExamen {

    private static final Logger LOG = LoggerFactory.getLogger(AvisosExamen.class);
    private static final String TIPO = "EXAMEN";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    private final NotificacionesClient notificaciones;
    private final Clock clock;

    public void programado(ExamenConduccion examen) {
        avisar("examen.programado", examen, "Examen de conducción programado",
            "Tu examen de conducción, categoría " + examen.getCategoria() + ", es el " + cuando(examen) + ".");
    }

    public void reprogramado(ExamenConduccion examen) {
        avisar("examen.reprogramado", examen, "Examen de conducción reprogramado",
            "Tu examen de conducción, categoría " + examen.getCategoria() + ", se movió al " + cuando(examen) + ".");
    }

    public void cancelado(ExamenConduccion examen) {
        avisar("examen.cancelado", examen, "Examen de conducción cancelado",
            "Tu examen de conducción del " + cuando(examen) + " fue cancelado.");
    }

    public void sinAsistencia(ExamenConduccion examen) {
        avisar("examen.no-asistio", examen, "Examen de conducción sin asistencia",
            "No asististe a tu examen de conducción del " + cuando(examen) + ".");
    }

    public void conResultado(ExamenConduccion examen) {
        boolean aprobado = examen.getResultado() == ResultadoEvaluacion.APROBADO;
        avisar("examen.resultado", examen, aprobado ? "¡Aprobaste tu examen!" : "Resultado de tu examen",
            (aprobado ? "Aprobaste" : "No aprobaste") + " tu examen de conducción, categoría "
                + examen.getCategoria() + ", con " + examen.getPuntaje() + " puntos.");
    }

    private String cuando(ExamenConduccion examen) {
        return FORMATO.format(examen.getFechaProgramada().atZone(clock.getZone()));
    }

    private void avisar(String evento, ExamenConduccion examen, String titulo, String mensaje) {
        try {
            String clave = examen.getCodigoExamenConduccion() + ":" + examen.getFechaProgramada();
            UUID recurso = UUID.nameUUIDFromBytes(clave.getBytes(StandardCharsets.UTF_8));
            notificaciones.avisar(NotificacionesClient.eventoCodigo(evento, recurso), examen.getEstudianteCodigo(),
                TIPO, titulo, mensaje,
                Map.of("examenCodigo", examen.getCodigoExamenConduccion().toString(), "evento", evento));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo armar un aviso de examen (tipo {})", e.getClass().getSimpleName(),
                kv("module", "examenes"), kv("examenCodigo", examen.getCodigoExamenConduccion()),
                kv("evento", evento));
        }
    }
}
