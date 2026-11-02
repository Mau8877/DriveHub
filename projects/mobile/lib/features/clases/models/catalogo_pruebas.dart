import 'tipo_prueba.dart';

/// Un criterio con el que se puntúa una prueba de clase (CU18). Lo entrega el servidor: la app no copia las claves.
class CriterioDelCatalogo {
  final String codigo;
  final String descripcion;
  final double maximo;

  const CriterioDelCatalogo({required this.codigo, required this.descripcion, required this.maximo});

  factory CriterioDelCatalogo.fromJson(Map<String, dynamic> json) {
    return CriterioDelCatalogo(
      codigo: json['codigo'] as String,
      descripcion: json['descripcion'] as String,
      maximo: (json['maximo'] as num).toDouble(),
    );
  }
}

/// Un tipo de prueba con sus criterios, en el orden en que se muestran.
class TipoDelCatalogo {
  final TipoPrueba tipo;
  final String etiqueta;
  final List<CriterioDelCatalogo> criterios;

  const TipoDelCatalogo({required this.tipo, required this.etiqueta, required this.criterios});

  factory TipoDelCatalogo.fromJson(Map<String, dynamic> json) {
    return TipoDelCatalogo(
      tipo: TipoPrueba.fromJson(json['tipo'] as String),
      etiqueta: json['etiqueta'] as String,
      criterios: (json['criterios'] as List<dynamic>)
          .map((e) => CriterioDelCatalogo.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// Los criterios de todos los tipos de prueba de clase (CU18).
class CatalogoPruebas {
  final List<TipoDelCatalogo> tipos;

  const CatalogoPruebas({required this.tipos});

  factory CatalogoPruebas.fromJson(Map<String, dynamic> json) {
    return CatalogoPruebas(
      tipos: (json['tipos'] as List<dynamic>).map((e) => TipoDelCatalogo.fromJson(e as Map<String, dynamic>)).toList(),
    );
  }

  /// El tipo pedido, o `null` si el servidor no lo trajo.
  TipoDelCatalogo? de(TipoPrueba tipo) {
    for (final t in tipos) {
      if (t.tipo == tipo) return t;
    }
    return null;
  }
}
