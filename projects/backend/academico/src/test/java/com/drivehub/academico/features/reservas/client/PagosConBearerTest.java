package com.drivehub.academico.features.reservas.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drivehub.academico.features.reservas.client.PagosClient.ResultadoConsumo;
import com.drivehub.academico.features.reservas.client.ServidorFalso.Respuesta;

/** El cliente con Bearer da el mismo resultado que el de servicio ante los mismos errores (CU44). */
class PagosConBearerTest {

    private static final String BEARER = "Bearer token-del-estudiante";
    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID CLASE = UUID.randomUUID();

    private ServidorFalso pagos;
    private ClienteDePagos cliente;

    @BeforeEach
    void levantar() throws IOException {
        pagos = new ServidorFalso();
        cliente = new ClientesDePagos(new PagosClient(pagos.url(), 5)).conBearer(BEARER);
    }

    @AfterEach
    void apagar() {
        pagos.close();
    }

    private static Respuesta error(int estado, String codigo) {
        return new Respuesta(estado,
            "{\"status\":\"failed\",\"data\":null,\"message\":\"x\",\"error\":\"" + codigo + "\"}");
    }

    @Test
    void descontar_usaLaRutaPublicaConElBearer() {
        pagos.responder(p -> new Respuesta(201,
            "{\"status\":\"success\",\"data\":{\"horasDescontadas\":1,\"horasDisponibles\":4}}"));

        ResultadoConsumo resultado = cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "reserva-abc");

        assertThat(resultado).isEqualTo(new ResultadoConsumo(1, 4));
        var peticion = pagos.peticiones().get(0);
        assertThat(peticion.ruta()).isEqualTo("/api/v1/billeteras-horas/consumos");
        assertThat(peticion.authorization()).isEqualTo(BEARER);
        assertThat(peticion.credencialServicio()).isNull();
        assertThat(peticion.claveIdempotente()).isEqualTo("reserva-abc");
    }

    @Test
    void descontar_sinHoras_esSinHoras() {
        pagos.responder(p -> error(409, "ERR_PAG_09"));

        assertThatThrownBy(() -> cliente.descontar(ESCUELA, ESTUDIANTE, INSTRUCTOR, CLASE, 1, "k"))
            .isInstanceOf(SinHorasException.class);
    }

    @Test
    void devolver_usaLaRutaPublicaConElBearer_yUnaClaseSinConsumoNoEsError() {
        pagos.responder(p -> error(404, "ERR_PAG_16"));

        assertThatCode(() -> cliente.devolver(ESCUELA, ESTUDIANTE, CLASE, "devolucion-abc"))
            .doesNotThrowAnyException();

        var peticion = pagos.peticiones().get(0);
        assertThat(peticion.ruta()).isEqualTo("/api/v1/billeteras-horas/devoluciones");
        assertThat(peticion.authorization()).isEqualTo(BEARER);
    }
}
