package com.drivehub.escuela.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.drivehub.escuela.application.dto.CategoriaLicenciaOutput;
import com.drivehub.escuela.application.dto.PaginaOutput;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.CategoriaLicencia;
import com.drivehub.escuela.domain.repository.CategoriaLicenciaRepository;
import com.drivehub.escuela.domain.repository.Pagina;

/**
 * La autorización por rol NO se prueba acá (un caso de uso sin Spring no ejecuta @PreAuthorize):
 * se ejerce en la integración.
 */
@ExtendWith(MockitoExtension.class)
class CategoriasLicenciaUseCaseTest {

    @Mock
    private CategoriaLicenciaRepository repositorio;

    private static CategoriaLicencia categoria(String sigla, String descripcion) {
        return CategoriaLicencia.builder().codigo(UUID.randomUUID()).sigla(sigla).descripcion(descripcion).build();
    }

    @Test
    void listar_sinParametros_usaPagina1YVeintePorPagina() {
        when(repositorio.listar(1, 20)).thenReturn(new Pagina<>(List.of(categoria("P", "Particular")), 1));

        PaginaOutput<CategoriaLicenciaOutput> resultado = new ListarCategoriasLicenciaUseCase(repositorio)
            .ejecutar(null, null);

        assertThat(resultado.pagina()).isEqualTo(1);
        assertThat(resultado.tamano()).isEqualTo(20);
        assertThat(resultado.totalPaginas()).isEqualTo(1);
        assertThat(resultado.elementos()).containsExactly(new CategoriaLicenciaOutput("P", "Particular"));
    }

    @Test
    void listar_conTamanoMayorAlMaximo_loLimitaACien() {
        when(repositorio.listar(2, 100)).thenReturn(new Pagina<>(List.of(), 150));

        PaginaOutput<CategoriaLicenciaOutput> resultado = new ListarCategoriasLicenciaUseCase(repositorio)
            .ejecutar(2, 5000);

        verify(repositorio).listar(2, 100);
        assertThat(resultado.tamano()).isEqualTo(100);
        assertThat(resultado.totalPaginas()).isEqualTo(2);
    }

    @Test
    void listar_conPaginaOTamanoInvalidos_usaLosValoresPorDefecto() {
        when(repositorio.listar(1, 20)).thenReturn(new Pagina<>(List.of(), 0));

        PaginaOutput<CategoriaLicenciaOutput> resultado = new ListarCategoriasLicenciaUseCase(repositorio)
            .ejecutar(0, -3);

        assertThat(resultado.pagina()).isEqualTo(1);
        assertThat(resultado.tamano()).isEqualTo(20);
        assertThat(resultado.totalPaginas()).isZero();
    }

    @Test
    void obtener_conSiglaEnMinuscula_laPasaTalCualAlRepositorio() {
        when(repositorio.buscarPorSigla("sin")).thenReturn(Optional.of(categoria("SIN", "Sin categoría")));

        CategoriaLicenciaOutput resultado = new ObtenerCategoriaLicenciaUseCase(repositorio).ejecutar("sin");

        assertThat(resultado).isEqualTo(new CategoriaLicenciaOutput("SIN", "Sin categoría"));
    }

    @Test
    void obtener_conSiglaInexistente_lanzaErrEsc07() {
        when(repositorio.buscarPorSigla("ZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ObtenerCategoriaLicenciaUseCase(repositorio).ejecutar("ZZZ"))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(EscuelaErrorCodes.CATEGORIA_NO_ENCONTRADA));
    }
}
