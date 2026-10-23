package com.drivehub.academico.features.horarios.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.drivehub.academico.MongoIntegrationTestBase;
import com.drivehub.academico.common.mongo.AsignarIdentificadoresCallback;
import com.drivehub.academico.common.mongo.SecuenciaService;
import com.drivehub.academico.config.MongoConfig;

@DataMongoTest
@Import({AsignarIdentificadoresCallback.class, SecuenciaService.class, MongoConfig.class})
class HorariosMigracionTest extends MongoIntegrationTestBase {

    private static final String COLECCION = HorariosMigracion.COLECCION;

    @Autowired
    private MongoTemplate mongoTemplate;

    private HorariosMigracion migracion;

    @BeforeEach
    void limpiar() {
        mongoTemplate.remove(new Query(), COLECCION);
        migracion = new HorariosMigracion(mongoTemplate);
    }

    private Document viejo(long id, int dia, boolean activa) {
        Document documento = new Document("_id", id)
            .append("codigoPlantillaHorario", UUID.randomUUID())
            .append("diaSemana", dia)
            .append("horaInicio", "08:00")
            .append("horaFin", "12:00")
            .append("duracionClaseMin", 60)
            .append("activa", activa);
        mongoTemplate.insert(documento, COLECCION);
        return documento;
    }

    private Document leer(long id) {
        return mongoTemplate.findOne(Query.query(org.springframework.data.mongodb.core.query.Criteria.where("_id")
            .is(id)), Document.class, COLECCION);
    }

    @Test
    void migrar_unHorarioViejoActivo_quedaConUnDiaNombreYEstadoActiva() {
        viejo(1, 1, true);

        assertThat(migracion.migrar()).isEqualTo(1);

        Document nuevo = leer(1);
        assertThat(nuevo.getList("diasSemana", Integer.class)).containsExactly(1);
        assertThat(nuevo.getString("nombre")).isEqualTo("Lunes 08:00 – 12:00");
        assertThat(nuevo.getString("estado")).isEqualTo("ACTIVA");
        assertThat(nuevo).doesNotContainKeys("diaSemana", "activa");
    }

    @Test
    void migrar_unHorarioViejoInactivo_quedaInactiva() {
        viejo(2, 3, false);

        migracion.migrar();

        Document nuevo = leer(2);
        assertThat(nuevo.getString("estado")).isEqualTo("INACTIVA");
        assertThat(nuevo.getString("nombre")).isEqualTo("Miércoles 08:00 – 12:00");
        assertThat(nuevo.getList("diasSemana", Integer.class)).containsExactly(3);
    }

    @Test
    void migrar_noTocaUnDocumentoYaConvertido_yRepetirloNoCambiaNada() {
        Document convertido = new Document("_id", 3L)
            .append("nombre", "Entre semana")
            .append("diasSemana", List.of(1, 3, 5))
            .append("estado", "PAUSADA");
        mongoTemplate.insert(convertido, COLECCION);
        viejo(4, 2, true);

        assertThat(migracion.migrar()).isEqualTo(1);
        assertThat(migracion.migrar()).isZero();

        Document intacto = leer(3);
        assertThat(intacto.getString("nombre")).isEqualTo("Entre semana");
        assertThat(intacto.getList("diasSemana", Integer.class)).containsExactly(1, 3, 5);
        assertThat(intacto.getString("estado")).isEqualTo("PAUSADA");
        assertThat(leer(4).getString("nombre")).isEqualTo("Martes 08:00 – 12:00");
    }

    @Test
    void migrar_sinHorariosViejos_noHaceNada() {
        assertThat(migracion.migrar()).isZero();
    }

    @Test
    void nombreDe_usaElDiaEnEspanolConMayuscula() {
        assertThat(HorariosMigracion.nombreDe(7, "09:00", "10:00")).isEqualTo("Domingo 09:00 – 10:00");
        assertThat(HorariosMigracion.nombreDe(6, "14:30", "18:30")).isEqualTo("Sábado 14:30 – 18:30");
    }
}
