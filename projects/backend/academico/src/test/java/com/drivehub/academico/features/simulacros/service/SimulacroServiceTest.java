package com.drivehub.academico.features.simulacros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.features.simulacros.dto.CrearSimulacroRequest;
import com.drivehub.academico.features.simulacros.dto.EditarSimulacroRequest;
import com.drivehub.academico.features.simulacros.dto.ReglaRequest;
import com.drivehub.academico.features.simulacros.dto.SimulacroResponse;
import com.drivehub.academico.features.simulacros.model.ReglaSimulacro;
import com.drivehub.academico.features.simulacros.model.SimulacroConfig;
import com.drivehub.academico.features.simulacros.repository.FiltroSimulacros;
import com.drivehub.academico.features.simulacros.repository.SimulacroConfigRepository;

@ExtendWith(MockitoExtension.class)
class SimulacroServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();

    @Mock
    private SimulacroConfigRepository repository;

    @InjectMocks
    private SimulacroService service;

    private static ReglaRequest regla(String tema, int cantidad) {
        return new ReglaRequest(tema, cantidad);
    }

    private static CrearSimulacroRequest crear(Integer nroPreguntas, List<ReglaRequest> reglas) {
        return new CrearSimulacroRequest("  Simulacro básico ", "GENERAL", 20, nroPreguntas, 70.0, reglas);
    }

    private static SimulacroConfig existente(boolean activo) {
        SimulacroConfig s = new SimulacroConfig();
        s.setEscuelaCodigo(ESCUELA);
        s.setNombre("Simulacro básico");
        s.setCategoria("GENERAL");
        s.setDuracionMin(20);
        s.setNroPreguntas(5);
        s.setPuntajeAprobacion(70);
        s.setReglas(List.of(new ReglaSimulacro("Señales", 3), new ReglaSimulacro("Prioridad", 2)));
        s.setActivo(activo);
        s.inicializarCodigo();
        return s;
    }

    private void esperaCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(codigo));
    }

    private void conSimulacroGuardado(SimulacroConfig simulacro) {
        when(repository.findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull(ESCUELA, CODIGO))
            .thenReturn(Optional.of(simulacro));
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_conDatosValidos_guardaActivoConEscuelaAutorYTextosRecortados() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SimulacroResponse respuesta = service.crear(ESCUELA, AUTOR,
            crear(5, List.of(regla(" Señales ", 3), regla("Prioridad", 2))));

        ArgumentCaptor<SimulacroConfig> guardado = ArgumentCaptor.forClass(SimulacroConfig.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getEscuelaCodigo()).isEqualTo(ESCUELA);
        assertThat(guardado.getValue().getCreadoPorCodigo()).isEqualTo(AUTOR);
        assertThat(guardado.getValue().isActivo()).isTrue();
        assertThat(respuesta.nombre()).isEqualTo("Simulacro básico");
        assertThat(respuesta.reglas()).extracting(SimulacroResponse.ReglaResponse::tema)
            .containsExactly("Señales", "Prioridad");
        assertThat(respuesta.nroPreguntas()).isEqualTo(5);
    }

    @Test
    void crear_conReglasQueNoSumanElNumeroDePreguntas_lanzaErrSys01SinGuardar() {
        esperaCodigo(() -> service.crear(ESCUELA, AUTOR, crear(6, List.of(regla("Señales", 3), regla("Prioridad", 2)))),
            "ERR_SYS_01");
        verify(repository, never()).save(any());
    }

    @Test
    void crear_conReglasInvalidas_lanzaErrSys01() {
        List<List<ReglaRequest>> malas = List.of(
            List.of(regla("Señales", 5), regla("señales", 0)),
            List.of(regla("Señales", 3), regla("SEÑALES", 2)),
            List.of(regla("S", 5)),
            List.of(regla("Señales", 101)),
            List.of(new ReglaRequest("Señales", null)));
        for (List<ReglaRequest> reglas : malas) {
            esperaCodigo(() -> service.crear(ESCUELA, AUTOR, crear(5, reglas)), "ERR_SYS_01");
        }
        esperaCodigo(() -> service.crear(ESCUELA, AUTOR, crear(5, List.of())), "ERR_SYS_01");
        verify(repository, never()).save(any());
    }

    @Test
    void crear_conCamposInvalidos_lanzaErrSys01SinGuardar() {
        List<ReglaRequest> bien = List.of(regla("Señales", 5));
        CrearSimulacroRequest[] malas = {
            new CrearSimulacroRequest("Ab", "GENERAL", 20, 5, 70.0, bien),
            new CrearSimulacroRequest("Simulacro", "general", 20, 5, 70.0, bien),
            new CrearSimulacroRequest("Simulacro", "GENERAL", 4, 5, 70.0, bien),
            new CrearSimulacroRequest("Simulacro", "GENERAL", 181, 5, 70.0, bien),
            new CrearSimulacroRequest("Simulacro", "GENERAL", 20, 0, 70.0, bien),
            new CrearSimulacroRequest("Simulacro", "GENERAL", 20, 5, 0.5, bien),
            new CrearSimulacroRequest("Simulacro", "GENERAL", 20, 5, 100.5, bien),
            new CrearSimulacroRequest("Simulacro", "GENERAL", null, 5, 70.0, bien),
        };
        for (CrearSimulacroRequest mala : malas) {
            esperaCodigo(() -> service.crear(ESCUELA, AUTOR, mala), "ERR_SYS_01");
        }
        verify(repository, never()).save(any());
    }

    @Test
    void crear_conNombreRepetidoEnLaEscuela_lanzaConflictErrAca23SinGuardar() {
        when(repository.existsByEscuelaCodigoAndNombreIgnoreCaseAndDeletedAtIsNull(ESCUELA, "Simulacro básico"))
            .thenReturn(true);

        esperaCodigo(() -> service.crear(ESCUELA, AUTOR, crear(5, List.of(regla("Señales", 5)))),
            AcademicoErrorCodes.SIMULACRO_NOMBRE_DUPLICADO);
        verify(repository, never()).save(any());
    }

    // ------------------------------------------------------------------ listar y obtener

    @Test
    void listar_sinParametros_usaPagina1De20_conTamanoGrande_loLimitaA100() {
        when(repository.listar(any(), any(), any())).thenReturn(new PageImpl<>(List.of(existente(true))));
        FiltroSimulacros filtro = new FiltroSimulacros(null, null, null);

        service.listar(ESCUELA, filtro, null, null);
        service.listar(ESCUELA, filtro, 3, 5000);

        verify(repository).listar(ESCUELA, filtro, PageRequest.of(0, 20));
        verify(repository).listar(ESCUELA, filtro, PageRequest.of(2, 100));
    }

    @Test
    void obtener_conSimulacroInexistenteODeOtraEscuela_lanzaErrAca21() {
        when(repository.findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull(ESCUELA, CODIGO))
            .thenReturn(Optional.empty());

        esperaCodigo(() -> service.obtener(ESCUELA, CODIGO), AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO);
    }

    // ------------------------------------------------------------------ editar

    @Test
    void editar_soloCambiaLoEnviadoYDejaLoDemas() {
        conSimulacroGuardado(existente(true));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SimulacroResponse respuesta = service.editar(ESCUELA, CODIGO,
            new EditarSimulacroRequest(null, "B", 45, null, 80.0, null));

        assertThat(respuesta.categoria()).isEqualTo("B");
        assertThat(respuesta.duracionMin()).isEqualTo(45);
        assertThat(respuesta.puntajeAprobacion()).isEqualTo(80.0);
        assertThat(respuesta.nombre()).isEqualTo("Simulacro básico");
        assertThat(respuesta.reglas()).hasSize(2);
        assertThat(respuesta.nroPreguntas()).isEqualTo(5);
    }

    @Test
    void editar_cambiandoSoloElNumero_revalidaLasReglasGuardadas() {
        conSimulacroGuardado(existente(true));

        esperaCodigo(() -> service.editar(ESCUELA, CODIGO,
            new EditarSimulacroRequest(null, null, null, 8, null, null)), "ERR_SYS_01");
        verify(repository, never()).save(any());
    }

    @Test
    void editar_conNumeroYReglasQueCuadran_reemplazaLasReglas() {
        conSimulacroGuardado(existente(true));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SimulacroResponse respuesta = service.editar(ESCUELA, CODIGO, new EditarSimulacroRequest(null, null, null, 8,
            null, List.of(regla("Señales", 5), regla("Mecánica", 3))));

        assertThat(respuesta.nroPreguntas()).isEqualTo(8);
        assertThat(respuesta.reglas()).extracting(SimulacroResponse.ReglaResponse::tema)
            .containsExactly("Señales", "Mecánica");
    }

    @Test
    void editar_conNombreDeOtroSimulacro_lanzaConflictPeroConElMismoNombreCambiandoMayusculasNo() {
        conSimulacroGuardado(existente(true));
        when(repository.existsByEscuelaCodigoAndNombreIgnoreCaseAndDeletedAtIsNull(ESCUELA, "Otro")).thenReturn(true);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        esperaCodigo(() -> service.editar(ESCUELA, CODIGO,
            new EditarSimulacroRequest("Otro", null, null, null, null, null)),
            AcademicoErrorCodes.SIMULACRO_NOMBRE_DUPLICADO);
        assertThat(service.editar(ESCUELA, CODIGO,
            new EditarSimulacroRequest("SIMULACRO BÁSICO", null, null, null, null, null)).nombre())
            .isEqualTo("SIMULACRO BÁSICO");
    }

    @Test
    void editar_conSimulacroInexistente_lanzaErrAca21() {
        when(repository.findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull(ESCUELA, CODIGO))
            .thenReturn(Optional.empty());

        esperaCodigo(() -> service.editar(ESCUELA, CODIGO,
            new EditarSimulacroRequest("Nuevo", null, null, null, null, null)),
            AcademicoErrorCodes.SIMULACRO_NO_ENCONTRADO);
    }

    // ------------------------------------------------------------------ activar y desactivar

    @Test
    void cambiarActivo_desactivaUnoActivoYActivaUnoDesactivado() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        conSimulacroGuardado(existente(true));
        assertThat(service.cambiarActivo(ESCUELA, CODIGO, false).activo()).isFalse();
        conSimulacroGuardado(existente(false));
        assertThat(service.cambiarActivo(ESCUELA, CODIGO, true).activo()).isTrue();
    }

    @Test
    void cambiarActivo_alEstadoQueYaTiene_lanzaConflictErrAca22SinGuardar() {
        conSimulacroGuardado(existente(true));
        esperaCodigo(() -> service.cambiarActivo(ESCUELA, CODIGO, true),
            AcademicoErrorCodes.SIMULACRO_YA_EN_ESE_ESTADO);

        conSimulacroGuardado(existente(false));
        esperaCodigo(() -> service.cambiarActivo(ESCUELA, CODIGO, false),
            AcademicoErrorCodes.SIMULACRO_YA_EN_ESE_ESTADO);
        verify(repository, never()).save(any());
    }
}
