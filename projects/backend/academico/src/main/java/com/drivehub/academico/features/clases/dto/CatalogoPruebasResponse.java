package com.drivehub.academico.features.clases.dto;

import java.util.List;

import com.drivehub.academico.features.clases.service.CatalogoDePruebas;
import com.drivehub.academico.features.clases.service.CatalogoDePruebas.TipoDelCatalogo;

/**
 * Los criterios de cada tipo de prueba de clase (CU18), para que la app no los copie.
 */
public record CatalogoPruebasResponse(List<TipoDelCatalogo> tipos) {

    public static CatalogoPruebasResponse actual() {
        return new CatalogoPruebasResponse(CatalogoDePruebas.tipos());
    }
}
