package com.drivehub.academico.features.clases.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.enums.EstadoClase;
import com.drivehub.academico.features.clases.model.Clase;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ClaseDtoTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void abrir() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static CerrarClaseRequest cierre(UUID reserva, Asistencia asistencia, String tema, String observaciones,
            Integer kmInicio, Integer kmFin) {
        return new CerrarClaseRequest(reserva, asistencia, tema, observaciones, kmInicio, kmFin);
    }

    private static Set<String> campos(Set<? extends ConstraintViolation<?>> violaciones) {
        return violaciones.stream().map(v -> v.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void cerrarClaseRequest_conSoloLaAsistenciaYLaReservaEsValido() {
        assertThat(validador.validate(cierre(UUID.randomUUID(), Asistencia.PRESENTE, null, null, null, null)))
            .isEmpty();
    }

    @Test
    void cerrarClaseRequest_exigeLaReservaYLaAsistencia() {
        var violaciones = validador.validate(cierre(null, null, null, null, null, null));

        assertThat(campos(violaciones)).containsExactlyInAnyOrder("reservaCodigo", "asistencia");
    }

    @Test
    void cerrarClaseRequest_limitesDeTemaYObservaciones() {
        UUID reserva = UUID.randomUUID();

        assertThat(validador.validate(cierre(reserva, Asistencia.TARDE, "a".repeat(120), "b".repeat(500), 0, 9_999_999)))
            .isEmpty();
        assertThat(campos(validador.validate(cierre(reserva, Asistencia.TARDE, "a".repeat(121), null, null, null))))
            .containsExactly("tema");
        assertThat(campos(validador.validate(cierre(reserva, Asistencia.TARDE, null, "b".repeat(501), null, null))))
            .containsExactly("observaciones");
    }

    @Test
    void cerrarClaseRequest_limitesDelKilometraje() {
        UUID reserva = UUID.randomUUID();

        assertThat(campos(validador.validate(cierre(reserva, Asistencia.PRESENTE, null, null, -1, null))))
            .containsExactly("kmInicio");
        assertThat(campos(validador.validate(cierre(reserva, Asistencia.PRESENTE, null, null, null, 10_000_000))))
            .containsExactly("kmFin");
        assertThat(validador.validate(cierre(reserva, Asistencia.PRESENTE, null, null, 0, 9_999_999))).isEmpty();
    }

    @Test
    void actualizarClaseRequest_todoEsOpcionalYTieneLosMismosLimites() {
        assertThat(validador.validate(new ActualizarClaseRequest(null, null, null, null))).isEmpty();
        assertThat(campos(validador.validate(new ActualizarClaseRequest("a".repeat(121), "b".repeat(501), -1,
            10_000_000)))).containsExactlyInAnyOrder("tema", "observaciones", "kmInicio", "kmFin");
        assertThat(validador.validate(new ActualizarClaseRequest("a".repeat(120), "b".repeat(500), 0, 9_999_999)))
            .isEmpty();
    }

    @Test
    void claseResponse_copiaLosCamposYNoExponeIdsInternos() {
        Clase clase = new Clase(77L, UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 26),
            Asistencia.TARDE);
        clase.inicializarCodigo();
        clase.setTema("Estacionamiento");
        clase.setObservaciones("Llegó 10 minutos tarde");
        clase.setKmInicio(100);
        clase.setKmFin(130);

        ClaseResponse respuesta = ClaseResponse.de(clase, "Eva Estudiante");

        assertThat(respuesta.codigo()).isEqualTo(clase.getCodigoClase());
        assertThat(respuesta.fecha()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(respuesta.estudianteCodigo()).isEqualTo(clase.getEstudianteCodigo());
        assertThat(respuesta.estudianteNombre()).isEqualTo("Eva Estudiante");
        assertThat(respuesta.asistencia()).isEqualTo(Asistencia.TARDE);
        assertThat(respuesta.tema()).isEqualTo("Estacionamiento");
        assertThat(respuesta.observaciones()).isEqualTo("Llegó 10 minutos tarde");
        assertThat(respuesta.kmInicio()).isEqualTo(100);
        assertThat(respuesta.kmFin()).isEqualTo(130);
        assertThat(respuesta.estado()).isEqualTo(EstadoClase.CERRADA);
        assertThat(java.util.Arrays.stream(ClaseResponse.class.getRecordComponents()).map(c -> c.getName()))
            .doesNotContain("id", "reservaId", "vehiculoCodigo");
    }
}
