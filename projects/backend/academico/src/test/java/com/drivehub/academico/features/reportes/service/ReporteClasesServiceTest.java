package com.drivehub.academico.features.reportes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.drivehub.academico.common.exception.AcademicoErrorCodes;
import com.drivehub.academico.common.exception.BusinessException;
import com.drivehub.academico.common.exception.ServicioNoDisponibleException;
import com.drivehub.academico.features.reportes.dto.ReporteClaseResponse;
import com.drivehub.academico.features.reportes.repository.FiltroReporteClases;
import com.drivehub.academico.features.reportes.repository.ReporteClasesRepository;
import com.drivehub.academico.features.reservas.client.IdentidadClient;
import com.drivehub.academico.features.reservas.enums.EstadoReserva;
import com.drivehub.academico.features.reservas.model.Reserva;

@ExtendWith(MockitoExtension.class)
class ReporteClasesServiceTest {

    private static final UUID ESCUELA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final String BEARER = "Bearer x";
    private static final FiltroReporteClases SIN_FILTROS = new FiltroReporteClases(null, null, null, null);

    @Mock
    private ReporteClasesRepository repository;
    @Mock
    private IdentidadClient identidadClient;

    private ReporteClasesService service;

    @BeforeEach
    void setUp() {
        service = new ReporteClasesService(repository, identidadClient);
    }

    private static Reserva reserva() {
        Reserva r = new Reserva(ESTUDIANTE, INSTRUCTOR, 1L, LocalDate.of(2026, 10, 15), "08:00", "09:00", 1,
            Instant.parse("2026-10-01T12:00:00Z"));
        r.inicializarCodigo();
        r.setEstado(EstadoReserva.CANCELADA);
        return r;
    }

    @Test
    void listar_agregaLosNombresDeEstudianteEInstructor() {
        when(repository.listar(eq(ESCUELA), eq(SIN_FILTROS), any()))
            .thenReturn(new PageImpl<>(List.of(reserva()), PageRequest.of(0, 100), 1));
        when(identidadClient.nombresDeEstudiantes(anyList(), eq(BEARER))).thenReturn(Map.of(ESTUDIANTE, "Ana Pérez"));
        when(identidadClient.nombresDeInstructores(anyList(), eq(BEARER))).thenReturn(Map.of(INSTRUCTOR, "Luis Rojas"));

        Page<ReporteClaseResponse> pagina = service.listar(ESCUELA, BEARER, SIN_FILTROS, null, null);

        ReporteClaseResponse clase = pagina.getContent().getFirst();
        assertThat(clase.estudianteNombre()).isEqualTo("Ana Pérez");
        assertThat(clase.instructorNombre()).isEqualTo("Luis Rojas");
        assertThat(clase.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(clase.fecha()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void listar_conIdentidadCaida_devuelveLasClasesSinNombres() {
        when(repository.listar(eq(ESCUELA), eq(SIN_FILTROS), any()))
            .thenReturn(new PageImpl<>(List.of(reserva()), PageRequest.of(0, 100), 1));
        when(identidadClient.nombresDeEstudiantes(anyList(), eq(BEARER)))
            .thenThrow(new ServicioNoDisponibleException(AcademicoErrorCodes.SERVICIO_NO_DISPONIBLE, "caído"));
        when(identidadClient.nombresDeInstructores(anyList(), eq(BEARER))).thenReturn(Map.of());

        Page<ReporteClaseResponse> pagina = service.listar(ESCUELA, BEARER, SIN_FILTROS, null, null);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().getFirst().estudianteNombre()).isNull();
    }

    @Test
    void listar_limitaElTamanoDePagina() {
        when(repository.listar(eq(ESCUELA), eq(SIN_FILTROS), any())).thenReturn(Page.empty(PageRequest.of(1, 500)));

        service.listar(ESCUELA, BEARER, SIN_FILTROS, 2, 5000);

        verify(repository).listar(ESCUELA, SIN_FILTROS, PageRequest.of(1, ReporteClasesService.TAMANO_MAXIMO));
        verifyNoInteractions(identidadClient);
    }

    @Test
    void listar_conRangoInvertido_lanzaValidationException() {
        FiltroReporteClases filtro = new FiltroReporteClases(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1),
            null, null);

        assertThatThrownBy(() -> service.listar(ESCUELA, BEARER, filtro, null, null))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(AcademicoErrorCodes.RANGO_DE_FECHAS_INVALIDO);
        verifyNoInteractions(repository);
    }
}
