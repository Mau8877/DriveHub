package com.drivehub.escuela.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.drivehub.escuela.PostgresIntegrationTestBase;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.repository.AsignacionDeAuto;
import com.drivehub.escuela.domain.repository.AsignacionHistorial;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.FiltroAsignaciones;
import com.drivehub.escuela.domain.repository.Pagina;

/**
 * CU10: el repositorio de asignaciones de autos contra un Postgres real (Flyway incluido).
 *
 * * Cada prueba usa su propia escuela (UUID nuevo): la base se comparte.
 */
@SpringBootTest
class AsignacionVehiculoRepositoryImplTest extends PostgresIntegrationTestBase {

    @Autowired
    private AsignacionVehiculoRepository asignaciones;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID vehiculo(UUID escuela, String placa) {
        UUID codigo = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO vehiculo (codigo_vehiculo, escuela_codigo, placa, marca, modelo, transmision)
            VALUES (?, ?, ?, 'Toyota', 'Yaris', 'MANUAL')
            """, codigo, escuela, placa);
        return codigo;
    }

    private UUID instructor(UUID escuela, UUID usuario) {
        UUID codigo = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO instructor (codigo_instructor, usuario_codigo, escuela_codigo, nro_licencia)
            VALUES (?, ?, ?, ?)
            """, codigo, usuario, escuela, "LIC-" + UUID.randomUUID().toString().substring(0, 8));
        return codigo;
    }

    private static String placa() {
        return "P-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
    }

    private static AsignacionVehiculo nueva(UUID vehiculo, UUID instructor, LocalDate inicio) {
        return AsignacionVehiculo.builder().codigo(UUID.randomUUID()).vehiculoCodigo(vehiculo)
            .instructorCodigo(instructor).fechaInicio(inicio).build();
    }

    @Test
    void abrir_dejaUnaVigenteYUnaSegundaDelMismoAutoSeRechazaConErrEsc16() {
        UUID escuela = UUID.randomUUID();
        UUID auto = vehiculo(escuela, placa());
        UUID ana = instructor(escuela, UUID.randomUUID());
        UUID luis = instructor(escuela, UUID.randomUUID());

        asignaciones.abrir(escuela, nueva(auto, ana, LocalDate.of(2026, 10, 1)));

        assertThat(asignaciones.buscarVigente(escuela, auto)).hasValueSatisfying(v -> {
            assertThat(v.getInstructorCodigo()).isEqualTo(ana);
            assertThat(v.getFechaFin()).isNull();
        });
        assertThatThrownBy(() -> asignaciones.abrir(escuela, nueva(auto, luis, LocalDate.of(2026, 10, 2))))
            .isInstanceOfSatisfying(ConflictException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(EscuelaErrorCodes.VEHICULO_YA_ASIGNADO));
    }

    @Test
    void cerrar_ponLaFechaDeFinYElAutoQuedaSinVigente() {
        UUID escuela = UUID.randomUUID();
        UUID auto = vehiculo(escuela, placa());
        asignaciones.abrir(escuela, nueva(auto, instructor(escuela, UUID.randomUUID()), LocalDate.of(2026, 10, 1)));

        assertThat(asignaciones.cerrar(escuela, auto, LocalDate.of(2026, 10, 10)))
            .hasValueSatisfying(c -> assertThat(c.getFechaFin()).isEqualTo(LocalDate.of(2026, 10, 10)));

        assertThat(asignaciones.buscarVigente(escuela, auto)).isEmpty();
        assertThat(asignaciones.ultimaFechaFin(escuela, auto)).contains(LocalDate.of(2026, 10, 10));
        assertThat(asignaciones.cerrar(escuela, auto, LocalDate.of(2026, 10, 11))).isEmpty();
    }

    @Test
    void cerrarYAbrir_cambiaDeInstructorYElHistorialQuedaCompleto() {
        UUID escuela = UUID.randomUUID();
        UUID auto = vehiculo(escuela, placa());
        UUID usuarioAna = UUID.randomUUID();
        UUID usuarioLuis = UUID.randomUUID();
        UUID ana = instructor(escuela, usuarioAna);
        UUID luis = instructor(escuela, usuarioLuis);
        asignaciones.abrir(escuela, nueva(auto, ana, LocalDate.of(2026, 10, 1)));

        asignaciones.cerrarYAbrir(escuela, auto, LocalDate.of(2026, 10, 15), nueva(auto, luis, LocalDate.of(2026, 10, 15)));

        assertThat(asignaciones.buscarVigente(escuela, auto))
            .hasValueSatisfying(v -> assertThat(v.getInstructorCodigo()).isEqualTo(luis));
        Pagina<AsignacionHistorial> historial = asignaciones.historial(escuela, auto, 1, 20);
        assertThat(historial.total()).isEqualTo(2);
        assertThat(historial.elementos().get(0).asignacion().getInstructorCodigo()).isEqualTo(luis);
        assertThat(historial.elementos().get(0).instructorUsuarioCodigo()).isEqualTo(usuarioLuis);
        assertThat(historial.elementos().get(1).asignacion().getFechaFin()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(asignaciones.historial(escuela, auto, 2, 1).elementos()).hasSize(1);
    }

    @Test
    void listar_traeUnRenglonPorAutoConYSinInstructorYAislaPorEscuela() {
        UUID escuela = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID asignado = vehiculo(escuela, placa());
        UUID libre = vehiculo(escuela, placa());
        vehiculo(UUID.randomUUID(), placa());
        asignaciones.abrir(escuela, nueva(asignado, instructor(escuela, usuario), LocalDate.of(2026, 10, 1)));

        Pagina<AsignacionDeAuto> todos = asignaciones.listar(escuela, new FiltroAsignaciones(null, null, null), 1, 20);

        assertThat(todos.total()).isEqualTo(2);
        assertThat(todos.elementos()).extracting(f -> f.vehiculo().getCodigo()).containsExactlyInAnyOrder(asignado, libre);
        AsignacionDeAuto fila = todos.elementos().stream()
            .filter(f -> f.vehiculo().getCodigo().equals(asignado)).findFirst().orElseThrow();
        assertThat(fila.vigente()).isNotNull();
        assertThat(fila.instructorUsuarioCodigo()).isEqualTo(usuario);
        assertThat(todos.elementos().stream().filter(f -> f.vehiculo().getCodigo().equals(libre)).findFirst()
            .orElseThrow().vigente()).isNull();
    }

    @Test
    void listar_filtraPorAsignacionYBuscaPorPlacaOPorInstructor() {
        UUID escuela = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID asignado = vehiculo(escuela, "ZZZ-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        UUID libre = vehiculo(escuela, "AAA-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        asignaciones.abrir(escuela, nueva(asignado, instructor(escuela, usuario), LocalDate.of(2026, 10, 1)));

        assertThat(asignaciones.listar(escuela, new FiltroAsignaciones(null, null, FiltroAsignaciones.Asignacion.ASIGNADO),
            1, 20).elementos()).extracting(f -> f.vehiculo().getCodigo()).containsExactly(asignado);
        assertThat(asignaciones.listar(escuela, new FiltroAsignaciones(null, null,
            FiltroAsignaciones.Asignacion.SIN_ASIGNAR), 1, 20).elementos())
            .extracting(f -> f.vehiculo().getCodigo()).containsExactly(libre);
        assertThat(asignaciones.listar(escuela, new FiltroAsignaciones("aaa-", null, null), 1, 20).elementos())
            .extracting(f -> f.vehiculo().getCodigo()).containsExactly(libre);
        // * Un texto que no coincide con ninguna placa, pero sí con el nombre de un instructor: trae su auto
        assertThat(asignaciones.listar(escuela, new FiltroAsignaciones("ana", List.of(usuario), null), 1, 20).elementos())
            .extracting(f -> f.vehiculo().getCodigo()).containsExactly(asignado);
        assertThat(asignaciones.listar(escuela, new FiltroAsignaciones("100%", null, null), 1, 20).total()).isZero();
    }

    @Test
    void consultasDeOtraEscuela_noVenNada() {
        UUID escuela = UUID.randomUUID();
        UUID auto = vehiculo(escuela, placa());
        asignaciones.abrir(escuela, nueva(auto, instructor(escuela, UUID.randomUUID()), LocalDate.of(2026, 10, 1)));
        UUID otra = UUID.randomUUID();

        assertThat(asignaciones.buscarVigente(otra, auto)).isEmpty();
        assertThat(asignaciones.ultimaFechaFin(otra, auto)).isEmpty();
        assertThat(asignaciones.historial(otra, auto, 1, 20).total()).isZero();
        assertThat(asignaciones.cerrar(otra, auto, LocalDate.of(2026, 10, 2))).isEmpty();
    }
}
