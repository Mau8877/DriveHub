package com.drivehub.academico.features.clases.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.drivehub.academico.features.clases.enums.TipoPrueba;
import com.drivehub.academico.features.clases.service.CatalogoDePruebas.CriterioDelCatalogo;
import com.drivehub.academico.features.clases.service.CatalogoDePruebas.TipoDelCatalogo;

class CatalogoDePruebasTest {

    @Test
    void tipos_sonTresEnElOrdenDelEnum() {
        assertThat(CatalogoDePruebas.tipos()).extracting(TipoDelCatalogo::tipo)
            .containsExactly(TipoPrueba.MANEJO, TipoPrueba.ESTACIONAMIENTO, TipoPrueba.CIUDAD);
        assertThat(CatalogoDePruebas.tipos()).extracting(TipoDelCatalogo::etiqueta)
            .containsExactly("Manejo", "Estacionamiento", "Ciudad");
    }

    @Test
    void cadaTipo_tieneCincoCriteriosDeMaximoDiezConDescripcion() {
        for (TipoDelCatalogo tipo : CatalogoDePruebas.tipos()) {
            assertThat(tipo.criterios()).hasSize(5);
            assertThat(tipo.criterios()).allSatisfy(c -> {
                assertThat(c.maximo()).isEqualTo(10);
                assertThat(c.descripcion()).isNotBlank();
                assertThat(c.codigo()).matches("[a-z_]+");
            });
        }
    }

    @Test
    void lasClavesDeUnTipoNoSeRepiten() {
        for (TipoDelCatalogo tipo : CatalogoDePruebas.tipos()) {
            List<String> claves = tipo.criterios().stream().map(CriterioDelCatalogo::codigo).toList();
            assertThat(claves).doesNotHaveDuplicates();
        }
    }

    @Test
    void de_devuelveElTipoPedido() {
        for (TipoPrueba tipo : TipoPrueba.values()) {
            assertThat(CatalogoDePruebas.de(tipo).tipo()).isEqualTo(tipo);
        }
    }
}
