package com.drivehub.academico.features.resultados.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.features.reservas.client.ServidorFalso;
import com.drivehub.academico.features.reservas.client.ServidorFalso.Respuesta;

class EscuelaClientTest {

    private static final String BEARER = "Bearer token-del-instructor";

    private ServidorFalso escuela;
    private EscuelaClient cliente;

    @BeforeEach
    void levantar() throws IOException {
        escuela = new ServidorFalso();
        cliente = new EscuelaClient(escuela.url(), 5);
    }

    @AfterEach
    void apagar() {
        escuela.close();
    }

    private static String pagina(List<UUID> alumnos, long totalItems, int totalPages) {
        String data = String.join(",", alumnos.stream().map(a -> "{\"estudianteCodigo\":\"" + a + "\"}").toList());
        return "{\"status\":\"success\",\"data\":[" + data + "],\"message\":\"ok\",\"meta\":{\"page\":1,\"pageSize\":100,"
            + "\"totalItems\":" + totalItems + ",\"totalPages\":" + totalPages + "}}";
    }

    @Test
    void alumnosDelInstructor_pideLaRutaDeEscuelaConElBearerYDevuelveLosCodigos() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        escuela.responder(p -> new Respuesta(200, pagina(List.of(a, b), 2, 1)));

        assertThat(cliente.alumnosDelInstructor(BEARER)).containsExactly(a, b);

        var peticion = escuela.peticiones().get(0);
        assertThat(peticion.metodo()).isEqualTo("GET");
        assertThat(peticion.ruta()).isEqualTo("/api/v1/asignaciones-alumno/mis-alumnos");
        assertThat(peticion.authorization()).isEqualTo(BEARER);
    }

    @Test
    void alumnosDelInstructor_sinAlumnos_devuelveUnaListaVacia() {
        escuela.responder(p -> new Respuesta(200, pagina(List.of(), 0, 0)));

        assertThat(cliente.alumnosDelInstructor(BEARER)).isEmpty();
        assertThat(escuela.cantidad()).isEqualTo(1);
    }

    @Test
    void alumnosDelInstructor_recorreLasPaginasHastaAgotarlas() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        AtomicInteger llamada = new AtomicInteger();
        escuela.responder(p -> new Respuesta(200,
            llamada.getAndIncrement() == 0 ? pagina(List.of(a), 2, 2) : pagina(List.of(b), 2, 2)));

        assertThat(cliente.alumnosDelInstructor(BEARER)).containsExactly(a, b);
        assertThat(escuela.cantidad()).isEqualTo(2);
    }

    @Test
    void alumnosDelInstructor_cortaEnElTope() {
        List<UUID> cien = java.util.stream.IntStream.range(0, 100).mapToObj(i -> UUID.randomUUID()).toList();
        escuela.responder(p -> new Respuesta(200, pagina(cien, 900, 9)));

        assertThat(cliente.alumnosDelInstructor(BEARER)).hasSize(EscuelaClient.TOPE_ALUMNOS);
        assertThat(escuela.cantidad()).isEqualTo(5);
    }

    @Test
    void alumnosDelInstructor_conUnaFalla_esServicioNoDisponible() {
        escuela.responder(p -> new Respuesta(500, "{}"));

        assertThatThrownBy(() -> cliente.alumnosDelInstructor(BEARER))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_17"));
    }

    @Test
    void alumnosDelInstructor_conUnaRespuestaIncompleta_esServicioNoDisponible() {
        escuela.responder(p -> new Respuesta(200, "{\"status\":\"success\"}"));
        assertThatThrownBy(() -> cliente.alumnosDelInstructor(BEARER)).isInstanceOf(ServicioNoDisponibleException.class);

        escuela.responder(p -> new Respuesta(200,
            "{\"data\":[{}],\"meta\":{\"page\":1,\"pageSize\":100,\"totalItems\":1,\"totalPages\":1}}"));
        assertThatThrownBy(() -> cliente.alumnosDelInstructor(BEARER)).isInstanceOf(ServicioNoDisponibleException.class);
    }
}
