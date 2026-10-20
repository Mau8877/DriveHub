package com.drivehub.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La migración de CU04b: correo verificado en la cuenta y tipo de los tokens de correo.
 */
@SpringBootTest
class MigracionCorreoVerificadoTest extends PostgresIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void tablaUsuario_tieneCorreoVerificadoSinVerificarPorDefecto() {
        String porDefecto = jdbc.queryForObject(
            "SELECT column_default FROM information_schema.columns "
                + "WHERE table_name = 'usuario' AND column_name = 'correo_verificado'", String.class);
        String anulable = jdbc.queryForObject(
            "SELECT is_nullable FROM information_schema.columns "
                + "WHERE table_name = 'usuario' AND column_name = 'correo_verificado'", String.class);

        // * El valor por defecto es lo que deja a las cuentas existentes sin verificar
        assertThat(porDefecto).isEqualTo("false");
        assertThat(anulable).isEqualTo("NO");
    }

    @Test
    void tablaTokenRecuperacion_tieneTipoConSuRestriccionCheck() {
        List<String> columnas = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'token_recuperacion'", String.class);
        List<String> restricciones = jdbc.queryForList(
            "SELECT check_clause FROM information_schema.check_constraints WHERE check_clause LIKE '%VERIFICACION%'",
            String.class);

        assertThat(columnas).contains("tipo");
        assertThat(restricciones).hasSize(1);
        assertThat(restricciones.get(0)).contains("RECUPERACION");
    }
}
