package com.drivehub.academico.features.reservas.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.features.reservas.client.PagosClient.ResultadoConsumo;
import com.drivehub.academico.features.reservas.client.ServidorFalso.Respuesta;

class PagosClientTest {

    private static final String BEARER = "Bearer token-del-estudiante";
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CLASE = UUID.randomUUID();

    private ServidorFalso pagos;
    private PagosClient cliente;

    @BeforeEach
    void levantar() throws IOException {
        pagos = new ServidorFalso();
        cliente = new PagosClient(pagos.url(), 5);
    }

    @AfterEach
    void apagar() {
        pagos.close();
    }

    private static Respuesta ok(int estado, String data) {
        return new Respuesta(estado, "{\"status\":\"success\",\"data\":" + data + ",\"message\":\"ok\"}");
    }

    private static Respuesta error(int estado, String codigo) {
        return new Respuesta(estado,
            "{\"status\":\"failed\",\"data\":null,\"message\":\"x\",\"error\":\"" + codigo + "\"}");
    }

    // ---------------------------------------------------------------- saldo

    @Test
    void saldo_pideElSaldoPropioConElBearerDelEstudiante() {
        pagos.responder(p -> ok(200, "{\"horasDisponibles\":5}"));

        assertThat(cliente.saldo(BEARER)).isEqualTo(5);

        var peticion = pagos.peticiones().get(0);
        assertThat(peticion.metodo()).isEqualTo("GET");
        assertThat(peticion.ruta()).isEqualTo("/api/v1/billeteras-horas/mia");
        assertThat(peticion.authorization()).isEqualTo(BEARER);
    }

    @Test
    void saldo_conUnaRespuestaIncompleta_esServicioNoDisponible() {
        pagos.responder(p -> ok(200, "{}"));

        assertThatThrownBy(() -> cliente.saldo(BEARER)).isInstanceOfSatisfying(ServicioNoDisponibleException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_17"));
    }

    // ---------------------------------------------------------------- descontar

    @Test
    void descontar_mandaLaClaveYLosDatosDeLaClase_yDevuelveElResultado() {
        pagos.responder(p -> ok(201, "{\"horasDescontadas\":1,\"horasDisponibles\":4}"));

        ResultadoConsumo resultado = cliente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "reserva-abc");

        assertThat(resultado).isEqualTo(new ResultadoConsumo(1, 4));
        var peticion = pagos.peticiones().get(0);
        assertThat(peticion.metodo()).isEqualTo("POST");
        assertThat(peticion.ruta()).isEqualTo("/api/v1/billeteras-horas/consumos");
        assertThat(peticion.authorization()).isEqualTo(BEARER);
        assertThat(peticion.claveIdempotente()).isEqualTo("reserva-abc");
        assertThat(peticion.cuerpo()).contains(ESTUDIANTE.toString(), INSTRUCTOR.toString(), CLASE.toString())
            .contains("\"horas\":1");
    }

    @Test
    void descontar_conErrPag09_esSinHoras() {
        pagos.responder(p -> error(409, "ERR_PAG_09"));

        assertThatThrownBy(() -> cliente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(SinHorasException.class);
    }

    @Test
    void descontar_conOtroConflicto_esServicioNoDisponible() {
        pagos.responder(p -> error(409, "ERR_PAG_99"));

        assertThatThrownBy(() -> cliente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_conUnConflictoSinCuerpoLegible_esServicioNoDisponible() {
        pagos.responder(p -> new Respuesta(409, "no es json"));

        assertThatThrownBy(() -> cliente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_conUnaFallaDelServidor_esServicioNoDisponible() {
        pagos.responder(p -> error(500, "ERR_SYS_00"));

        assertThatThrownBy(() -> cliente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_conUnRechazoDeSesion_esServicioNoDisponible() {
        pagos.responder(p -> error(401, "ERR_SYS_04"));

        assertThatThrownBy(() -> cliente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_conUnaRespuestaIncompleta_esServicioNoDisponible() {
        pagos.responder(p -> ok(201, "{\"horasDescontadas\":1}"));

        assertThatThrownBy(() -> cliente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_sinNadieEscuchando_esServicioNoDisponible() {
        pagos.close();

        assertThatThrownBy(() -> cliente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_conUnTimeout_esServicioNoDisponible() {
        pagos.responder(p -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ok(201, "{\"horasDescontadas\":1,\"horasDisponibles\":4}");
        });
        PagosClient impaciente = new PagosClient(pagos.url(), 1);

        assertThatThrownBy(() -> impaciente.descontar(BEARER, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    // ---------------------------------------------------------------- devolver

    @Test
    void devolver_mandaLaClaveYLaClase() {
        pagos.responder(p -> ok(200, "{\"horasDevueltas\":1,\"horasDisponibles\":5}"));

        cliente.devolver(BEARER, ESTUDIANTE, CLASE, "devolucion-abc");

        var peticion = pagos.peticiones().get(0);
        assertThat(peticion.ruta()).isEqualTo("/api/v1/billeteras-horas/devoluciones");
        assertThat(peticion.authorization()).isEqualTo(BEARER);
        assertThat(peticion.claveIdempotente()).isEqualTo("devolucion-abc");
        assertThat(peticion.cuerpo()).contains(ESTUDIANTE.toString(), CLASE.toString());
    }

    @Test
    void devolver_siLaClaseNuncaDescontoNada_noEsUnError() {
        pagos.responder(p -> error(404, "ERR_PAG_16"));

        assertThatCode(() -> cliente.devolver(BEARER, ESTUDIANTE, CLASE, "k")).doesNotThrowAnyException();
    }

    @Test
    void devolver_conOtro404_esServicioNoDisponible() {
        pagos.responder(p -> error(404, "ERR_SYS_03"));

        assertThatThrownBy(() -> cliente.devolver(BEARER, ESTUDIANTE, CLASE, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void devolver_conUnaFallaDelServidor_esServicioNoDisponible() {
        pagos.responder(p -> error(503, "ERR_SYS_00"));

        assertThatThrownBy(() -> cliente.devolver(BEARER, ESTUDIANTE, CLASE, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
