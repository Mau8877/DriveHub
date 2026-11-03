package com.drivehub.academico.features.clases.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.ValidationException;
import com.drivehub.academico.features.clases.dto.ActualizarClaseRequest;
import com.drivehub.academico.features.clases.enums.Asistencia;
import com.drivehub.academico.features.clases.model.Clase;

class ReglasDeClaseTest {

    private static Clase clase(Asistencia asistencia, Integer kmInicio, Integer kmFin) {
        Clase clase = new Clase(1L, UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 26), asistencia);
        clase.setKmInicio(kmInicio);
        clase.setKmFin(kmFin);
        return clase;
    }

    private static void esDatosInvalidos(Runnable accion, String mensaje) {
        assertThatThrownBy(accion::run)
            .isInstanceOfSatisfying(ValidationException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(AcademicoErrorCodes.DATOS_DE_CLASE_INVALIDOS);
                assertThat(e.getMessage()).isEqualTo(mensaje);
            });
    }

    // ---------------------------------------------------------------- cierre

    @Test
    void validarCierre_ausenteConTemaOKilometrosFalla() {
        esDatosInvalidos(() -> ReglasDeClase.validarCierre(Asistencia.AUSENTE, "Tema", null, null),
            ReglasDeClase.SOLO_OBSERVACIONES);
        esDatosInvalidos(() -> ReglasDeClase.validarCierre(Asistencia.AUSENTE, null, 100, null),
            ReglasDeClase.SOLO_OBSERVACIONES);
        esDatosInvalidos(() -> ReglasDeClase.validarCierre(Asistencia.AUSENTE, null, null, 100),
            ReglasDeClase.SOLO_OBSERVACIONES);
    }

    @Test
    void validarCierre_ausenteSinTemaNiKilometrosPasaYUnTemaEnBlancoNoCuenta() {
        assertThatCode(() -> ReglasDeClase.validarCierre(Asistencia.AUSENTE, null, null, null))
            .doesNotThrowAnyException();
        assertThatCode(() -> ReglasDeClase.validarCierre(Asistencia.AUSENTE, "   ", null, null))
            .doesNotThrowAnyException();
    }

    @Test
    void validarCierre_elKilometrajeFinalNoPuedeSerMenorQueElInicial() {
        esDatosInvalidos(() -> ReglasDeClase.validarCierre(Asistencia.PRESENTE, null, 200, 199),
            ReglasDeClase.KILOMETRAJE_FINAL_MENOR);
    }

    @Test
    void validarCierre_unSoloKilometrajeOIgualesPasan() {
        assertThatCode(() -> ReglasDeClase.validarCierre(Asistencia.PRESENTE, "Tema", 100, null))
            .doesNotThrowAnyException();
        assertThatCode(() -> ReglasDeClase.validarCierre(Asistencia.TARDE, null, null, 50)).doesNotThrowAnyException();
        assertThatCode(() -> ReglasDeClase.validarCierre(Asistencia.PRESENTE, null, 100, 100))
            .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------- corrección

    @Test
    void validarCorreccion_combinaLoEnviadoConLoGuardado() {
        Clase guardada = clase(Asistencia.PRESENTE, 100, 150);

        esDatosInvalidos(() -> ReglasDeClase.validarCorreccion(guardada,
            new ActualizarClaseRequest(null, null, null, 90)), ReglasDeClase.KILOMETRAJE_FINAL_MENOR);
        esDatosInvalidos(() -> ReglasDeClase.validarCorreccion(guardada,
            new ActualizarClaseRequest(null, null, 160, null)), ReglasDeClase.KILOMETRAJE_FINAL_MENOR);
        assertThatCode(() -> ReglasDeClase.validarCorreccion(guardada,
            new ActualizarClaseRequest(null, null, 120, 200))).doesNotThrowAnyException();
        assertThatCode(() -> ReglasDeClase.validarCorreccion(guardada,
            new ActualizarClaseRequest("Nuevo tema", null, null, null))).doesNotThrowAnyException();
    }

    @Test
    void validarCorreccion_sinKilometrajesGuardadosSoloMiraLoEnviado() {
        Clase guardada = clase(Asistencia.PRESENTE, null, null);

        assertThatCode(() -> ReglasDeClase.validarCorreccion(guardada,
            new ActualizarClaseRequest(null, null, null, 10))).doesNotThrowAnyException();
        esDatosInvalidos(() -> ReglasDeClase.validarCorreccion(guardada,
            new ActualizarClaseRequest(null, null, 20, 10)), ReglasDeClase.KILOMETRAJE_FINAL_MENOR);
    }

    @Test
    void validarCorreccion_unaClaseAusenteSoloAdmiteObservaciones() {
        Clase ausente = clase(Asistencia.AUSENTE, null, null);

        esDatosInvalidos(() -> ReglasDeClase.validarCorreccion(ausente,
            new ActualizarClaseRequest("Tema", null, null, null)), ReglasDeClase.SOLO_OBSERVACIONES);
        esDatosInvalidos(() -> ReglasDeClase.validarCorreccion(ausente,
            new ActualizarClaseRequest(null, null, 10, null)), ReglasDeClase.SOLO_OBSERVACIONES);
        assertThatCode(() -> ReglasDeClase.validarCorreccion(ausente,
            new ActualizarClaseRequest(null, "No avisó", null, null))).doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------- limpiar

    @Test
    void limpiar_recortaYDevuelveNullConBlancosOConNull() {
        assertThat(ReglasDeClase.limpiar("  Giros  ")).isEqualTo("Giros");
        assertThat(ReglasDeClase.limpiar("   ")).isNull();
        assertThat(ReglasDeClase.limpiar("")).isNull();
        assertThat(ReglasDeClase.limpiar(null)).isNull();
    }
}
