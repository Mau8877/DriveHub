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

class PagosServicioClientTest {

    private static final String CREDENCIAL = "secreto-de-prueba";
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CLASE = UUID.randomUUID();

    private ServidorFalso pagos;
    private PagosServicioClient cliente;

    @BeforeEach
    void levantar() throws IOException {
        pagos = new ServidorFalso();
        cliente = new PagosServicioClient(pagos.url(), 5, CREDENCIAL);
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

    // ---------------------------------------------------------------- descontar

    @Test
    void descontar_usaLaRutaInternaConLaCredencial_ySinAuthorization() {
        pagos.responder(p -> ok(201, "{\"horasDescontadas\":1,\"horasDisponibles\":4}"));

        ResultadoConsumo resultado = cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "reserva-abc");

        assertThat(resultado).isEqualTo(new ResultadoConsumo(1, 4));
        var peticion = pagos.peticiones().get(0);
        assertThat(peticion.metodo()).isEqualTo("POST");
        assertThat(peticion.ruta()).isEqualTo("/internal/v1/billeteras-horas/consumos");
        assertThat(peticion.credencialServicio()).isEqualTo(CREDENCIAL);
        assertThat(peticion.authorization()).isNull();
        assertThat(peticion.claveIdempotente()).isEqualTo("reserva-abc");
        assertThat(peticion.cuerpo()).contains(ESCUELA.toString(), ESTUDIANTE.toString(), INSTRUCTOR.toString(),
            CLASE.toString()).contains("\"horas\":1");
    }

    @Test
    void descontar_conErrPag09_esSinHoras() {
        pagos.responder(p -> error(409, "ERR_PAG_09"));

        assertThatThrownBy(() -> cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(SinHorasException.class);
    }

    @Test
    void descontar_conOtroConflicto_esServicioNoDisponible() {
        pagos.responder(p -> error(409, "ERR_PAG_99"));

        assertThatThrownBy(() -> cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_siPagosRechazaLaCredencial_esServicioNoDisponible() {
        pagos.responder(p -> error(401, "ERR_PAG_19"));

        assertThatThrownBy(() -> cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOfSatisfying(ServicioNoDisponibleException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACA_17"));
    }

    @Test
    void descontar_conUnaFallaDelServidor_esServicioNoDisponible() {
        pagos.responder(p -> error(500, "ERR_SYS_00"));

        assertThatThrownBy(() -> cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_conUnaRespuestaIncompleta_esServicioNoDisponible() {
        pagos.responder(p -> ok(201, "{\"horasDescontadas\":1}"));

        assertThatThrownBy(() -> cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void descontar_sinNadieEscuchando_esServicioNoDisponible() {
        pagos.close();

        assertThatThrownBy(() -> cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    // ---------------------------------------------------------------- devolver

    @Test
    void devolver_usaLaRutaInternaConLaCredencialYLaClave() {
        pagos.responder(p -> ok(200, "{\"horasDevueltas\":1,\"horasDisponibles\":5}"));

        cliente.devolver(ESCUELA, ESTUDIANTE, CLASE, "devolucion-abc");

        var peticion = pagos.peticiones().get(0);
        assertThat(peticion.ruta()).isEqualTo("/internal/v1/billeteras-horas/devoluciones");
        assertThat(peticion.credencialServicio()).isEqualTo(CREDENCIAL);
        assertThat(peticion.authorization()).isNull();
        assertThat(peticion.claveIdempotente()).isEqualTo("devolucion-abc");
        assertThat(peticion.cuerpo()).contains(ESCUELA.toString(), ESTUDIANTE.toString(), CLASE.toString());
    }

    @Test
    void devolver_siLaClaseNuncaDescontoNada_noEsUnError() {
        pagos.responder(p -> error(404, "ERR_PAG_16"));

        assertThatCode(() -> cliente.devolver(ESCUELA, ESTUDIANTE, CLASE, "k")).doesNotThrowAnyException();
    }

    @Test
    void devolver_conOtro404_esServicioNoDisponible() {
        pagos.responder(p -> error(404, "ERR_SYS_03"));

        assertThatThrownBy(() -> cliente.devolver(ESCUELA, ESTUDIANTE, CLASE, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void devolver_siPagosRechazaLaCredencial_esServicioNoDisponible() {
        pagos.responder(p -> error(401, "ERR_PAG_19"));

        assertThatThrownBy(() -> cliente.devolver(ESCUELA, ESTUDIANTE, CLASE, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
    }

    // ---------------------------------------------------------------- sin credencial

    @Test
    void sinCredencial_noSaleNingunaPeticion() {
        PagosServicioClient sinCredencial = new PagosServicioClient(pagos.url(), 5, "");

        assertThatThrownBy(() -> sinCredencial.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
        assertThatThrownBy(() -> sinCredencial.devolver(ESCUELA, ESTUDIANTE, CLASE, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(pagos.cantidad()).isZero();
    }

    @Test
    void sinCredencialNula_tampocoSaleNingunaPeticion() {
        PagosServicioClient nula = new PagosServicioClient(pagos.url(), 5, null);

        assertThatThrownBy(() -> nula.devolver(ESCUELA, ESTUDIANTE, CLASE, "k"))
            .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(pagos.cantidad()).isZero();
    }
}
