package com.drivehub.academico.features.reservas.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.features.reservas.client.ServidorFalso.Respuesta;

class IdentidadClientTest {

    private static final String BEARER = "Bearer token-del-estudiante";

    private ServidorFalso identidad;
    private IdentidadClient cliente;

    @BeforeEach
    void levantar() throws IOException {
        identidad = new ServidorFalso();
        cliente = new IdentidadClient(identidad.url(), 5);
    }

    @AfterEach
    void apagar() {
        identidad.close();
    }

    private static Respuesta ok(String data) {
        return new Respuesta(200, "{\"status\":\"success\",\"data\":" + data + ",\"message\":\"ok\"}");
    }

    @Test
    void nombresDeInstructores_pideLosNombresConElBearer_yDevuelveUnMapa() {
        UUID a = UUID.randomUUID();
        identidad.responder(p -> ok("[{\"codigo\":\"" + a + "\",\"nombreCompleto\":\" Ivan Alfa \"}]"));

        Map<UUID, String> nombres = cliente.nombresDeInstructores(List.of(a), BEARER);

        assertThat(nombres).containsEntry(a, "Ivan Alfa");
        var peticion = identidad.peticiones().get(0);
        assertThat(peticion.metodo()).isEqualTo("POST");
        assertThat(peticion.ruta()).isEqualTo("/api/v1/usuarios/instructores/nombres");
        assertThat(peticion.authorization()).isEqualTo(BEARER);
        assertThat(peticion.cuerpo()).contains(a.toString());
    }

    @Test
    void nombresDeInstructores_losQueIdentidadNoDevuelveNoEstanEnElMapa() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        identidad.responder(p -> ok("[{\"codigo\":\"" + a + "\",\"nombreCompleto\":\"Ivan Alfa\"}]"));

        assertThat(cliente.nombresDeInstructores(List.of(a, b), BEARER)).containsOnlyKeys(a);
    }

    @Test
    void nombresDeInstructores_quitaRepetidos_yPideEnLotesDeCien() {
        List<UUID> codigos = IntStream.range(0, 250).mapToObj(i -> UUID.randomUUID()).toList();
        identidad.responder(p -> ok("[]"));

        cliente.nombresDeInstructores(codigos, BEARER);
        assertThat(identidad.cantidad()).isEqualTo(3);

        cliente.nombresDeInstructores(List.of(codigos.get(0), codigos.get(0), codigos.get(0)), BEARER);
        assertThat(identidad.cantidad()).isEqualTo(4);
    }

    @Test
    void nombresDeInstructores_sinCodigos_noLlamaANadie() {
        assertThat(cliente.nombresDeInstructores(List.of(), BEARER)).isEmpty();
        assertThat(identidad.cantidad()).isZero();
    }

    @Test
    void nombresDeInstructores_conUnaFalla_esServicioNoDisponible() {
        identidad.responder(p -> new Respuesta(500, "{}"));

        assertThatThrownBy(() -> cliente.nombresDeInstructores(List.of(UUID.randomUUID()), BEARER))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_17"));
    }

    @Test
    void nombresDeInstructores_conUnRechazoDeSesion_esServicioNoDisponible() {
        identidad.responder(p -> new Respuesta(401, "{}"));

        assertThatThrownBy(() -> cliente.nombresDeInstructores(List.of(UUID.randomUUID()), BEARER))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void nombresDeInstructores_conUnaRespuestaSinDatos_esServicioNoDisponible() {
        identidad.responder(p -> new Respuesta(200, "{\"status\":\"success\"}"));

        assertThatThrownBy(() -> cliente.nombresDeInstructores(List.of(UUID.randomUUID()), BEARER))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void nombresDeInstructores_sinNadieEscuchando_esServicioNoDisponible() {
        identidad.close();

        assertThatThrownBy(() -> cliente.nombresDeInstructores(List.of(UUID.randomUUID()), BEARER))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
