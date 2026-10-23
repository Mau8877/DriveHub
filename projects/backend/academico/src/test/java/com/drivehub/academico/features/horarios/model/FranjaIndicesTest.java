package com.drivehub.academico.features.horarios.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.common.mongo.AsignarIdentificadoresCallback;
import com.drivehub.academico.common.mongo.SecuenciaService;
import com.drivehub.academico.config.MongoConfig;

/**
 * Los índices únicos de las franjas, con un MongoDB real: el del instructor y el del vehículo, que es parcial.
 */
@DataMongoTest
@Import({AsignarIdentificadoresCallback.class, SecuenciaService.class, MongoConfig.class})
class FranjaIndicesTest extends MongoIntegrationTestBase {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 26);

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void limpiar() {
        // * remove() y no dropCollection(): borrar la colección se llevaría los índices que se están probando
        mongoTemplate.remove(new Query(), Franja.class);
    }

    private Franja franja(UUID instructor, String hora) {
        return new Franja(UUID.randomUUID(), null, instructor, FECHA, hora, "11:00");
    }

    private Franja franjaConVehiculo(UUID instructor, UUID vehiculo, String hora) {
        Franja franja = franja(instructor, hora);
        // * El vehículo no tiene setter (lo asignará CU10): se fija por reflexión solo para probar el índice
        ReflectionTestUtils.setField(franja, "vehiculoCodigo", vehiculo);
        return franja;
    }

    @Test
    void dosInstructoresDistintos_mismaFechaYHoraSinVehiculo_ambasSeGuardan() {
        mongoTemplate.insert(franja(UUID.randomUUID(), "10:00"));
        mongoTemplate.insert(franja(UUID.randomUUID(), "10:00"));

        assertThat(mongoTemplate.count(new Query(), Franja.class)).isEqualTo(2);
    }

    @Test
    void mismoInstructor_mismaFechaYHora_lanzaDuplicateKey() {
        UUID instructor = UUID.randomUUID();
        mongoTemplate.insert(franja(instructor, "10:00"));

        assertThatThrownBy(() -> mongoTemplate.insert(franja(instructor, "10:00")))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void mismoInstructor_otraHora_seGuarda() {
        UUID instructor = UUID.randomUUID();
        mongoTemplate.insert(franja(instructor, "10:00"));
        mongoTemplate.insert(franja(instructor, "11:00"));

        assertThat(mongoTemplate.count(new Query(), Franja.class)).isEqualTo(2);
    }

    @Test
    void mismoVehiculo_mismaFechaYHora_instructoresDistintos_lanzaDuplicateKey() {
        UUID vehiculo = UUID.randomUUID();
        mongoTemplate.insert(franjaConVehiculo(UUID.randomUUID(), vehiculo, "10:00"));

        assertThatThrownBy(() -> mongoTemplate.insert(franjaConVehiculo(UUID.randomUUID(), vehiculo, "10:00")))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void elIndiceDelVehiculoEsParcial() {
        Document indice = mongoTemplate.getCollection("franjas").listIndexes().into(new ArrayList<>())
            .stream().filter(i -> "uq_franja_vehiculo_fecha_hora".equals(i.getString("name"))).findFirst()
            .orElseThrow();

        assertThat(indice.get("partialFilterExpression")).isNotNull();
        assertThat(indice.getBoolean("unique")).isTrue();
    }
}
