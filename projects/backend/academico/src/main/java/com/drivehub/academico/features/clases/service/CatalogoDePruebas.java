package com.drivehub.academico.features.clases.service;

import java.util.List;

import com.drivehub.academico.features.clases.enums.TipoPrueba;

/**
 * Los criterios con que se evalúa cada tipo de prueba de clase (CU18). Son los mismos para todos los instructores y
 * viven en código, no en la base: cambiarlos es un cambio de código y las pruebas ya guardadas conservan su copia.
 */
public final class CatalogoDePruebas {

    /** El máximo de puntos de cada criterio. */
    public static final double MAXIMO_POR_CRITERIO = 10;

    private static final List<TipoDelCatalogo> TIPOS = List.of(
        tipo(TipoPrueba.MANEJO, "Manejo",
            criterio("espejos", "Posición y uso de espejos"),
            criterio("volante", "Control del volante"),
            criterio("pedales_y_cambios", "Uso de pedales y cambios"),
            criterio("senales", "Respeto a las señales"),
            criterio("distancia_y_velocidad", "Distancia y velocidad")),
        tipo(TipoPrueba.ESTACIONAMIENTO, "Estacionamiento",
            criterio("aproximacion", "Aproximación y posición"),
            criterio("entrada", "Maniobra de entrada"),
            criterio("espejos_y_observacion", "Uso de espejos y observación"),
            criterio("control_del_vehiculo", "Control del vehículo"),
            criterio("posicion_final", "Posición final")),
        tipo(TipoPrueba.CIUDAD, "Ciudad",
            criterio("incorporacion", "Incorporación al tráfico"),
            criterio("giros", "Giros e intersecciones"),
            criterio("peatones_y_senales", "Respeto a peatones y señales"),
            criterio("anticipacion", "Anticipación y distancia"),
            criterio("conduccion_defensiva", "Conducción defensiva")));

    private CatalogoDePruebas() {
    }

    /** Un criterio del catálogo: su clave legible, lo que se muestra al instructor y su máximo. */
    public record CriterioDelCatalogo(String codigo, String descripcion, double maximo) {
    }

    /** Un tipo de prueba con su etiqueta y sus criterios, en el orden en que se muestran. */
    public record TipoDelCatalogo(TipoPrueba tipo, String etiqueta, List<CriterioDelCatalogo> criterios) {
    }

    /** Los tres tipos, en el orden `MANEJO`, `ESTACIONAMIENTO`, `CIUDAD`. */
    public static List<TipoDelCatalogo> tipos() {
        return TIPOS;
    }

    public static TipoDelCatalogo de(TipoPrueba tipo) {
        return TIPOS.stream().filter(t -> t.tipo() == tipo).findFirst().orElseThrow();
    }

    private static TipoDelCatalogo tipo(TipoPrueba tipo, String etiqueta, CriterioDelCatalogo... criterios) {
        return new TipoDelCatalogo(tipo, etiqueta, List.of(criterios));
    }

    private static CriterioDelCatalogo criterio(String codigo, String descripcion) {
        return new CriterioDelCatalogo(codigo, descripcion, MAXIMO_POR_CRITERIO);
    }
}
