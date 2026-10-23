package com.drivehub.academico.features.horarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.drivehub.academico.features.horarios.enums.EstadoPlantilla;

import lombok.RequiredArgsConstructor;

/**
 * Convierte al arrancar los horarios creados antes del refactor del CU12 (un solo `diaSemana` y un
 * booleano `activa`) a la forma nueva (`diasSemana`, `nombre` y `estado`).
 *
 * * Es idempotente: solo toca los documentos que todavía tienen `diaSemana`, así que repetirlo no cambia nada.
 * * Trabaja con el documento crudo (no con el modelo), porque el modelo ya no tiene los campos viejos.
 * * El nombre se arma con el día y la ventana, por ejemplo «Lunes 08:00 – 12:00».
 */
@Component
@RequiredArgsConstructor
public class HorariosMigracion implements ApplicationRunner {

    static final String COLECCION = "plantillas_horario";

    private static final Logger LOG = LoggerFactory.getLogger(HorariosMigracion.class);
    private static final String MODULO = "horarios";
    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrar();
    }

    /**
     * Devuelve cuántos documentos convirtió.
     */
    public int migrar() {
        List<Document> viejos = mongoTemplate.find(Query.query(Criteria.where("diaSemana").exists(true)),
            Document.class, COLECCION);
        for (Document viejo : viejos) {
            int dia = ((Number) viejo.get("diaSemana")).intValue();
            EstadoPlantilla estado = Boolean.TRUE.equals(viejo.get("activa"))
                ? EstadoPlantilla.ACTIVA : EstadoPlantilla.INACTIVA;
            Update cambios = new Update()
                .set("diasSemana", List.of(dia))
                .set("nombre", nombreDe(dia, viejo.getString("horaInicio"), viejo.getString("horaFin")))
                .set("estado", estado.name())
                .unset("diaSemana")
                .unset("activa");
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(viejo.get("_id"))), cambios, COLECCION);
        }
        if (!viejos.isEmpty()) {
            LOG.info("Horarios anteriores convertidos al formato con varios días", kv("module", MODULO),
                kv("horarios", viejos.size()));
        }
        return viejos.size();
    }

    static String nombreDe(int dia, String horaInicio, String horaFin) {
        String nombreDia = DayOfWeek.of(dia).getDisplayName(TextStyle.FULL, ESPANOL);
        String capitalizado = nombreDia.substring(0, 1).toUpperCase(ESPANOL) + nombreDia.substring(1);
        return capitalizado + " " + horaInicio + " – " + horaFin;
    }
}
