import 'tipo_prueba.dart';

/// El puntaje que el instructor le puso a un criterio de la prueba (CU18).
class PuntajeDeCriterio {
  final String codigo;
  final double puntaje;

  const PuntajeDeCriterio({required this.codigo, required this.puntaje});

  Map<String, dynamic> toJson() => {'codigo': codigo, 'puntaje': puntaje};
}

/// Lo que el instructor envía al evaluar una clase ya cerrada (CU18): el tipo y el puntaje de cada criterio.
///
/// * Las observaciones en blanco no viajan: el backend las toma como «sin dato».
class EvaluarClaseRequest {
  final String claseCodigo;
  final TipoPrueba tipo;
  final List<PuntajeDeCriterio> criterios;
  final String? observaciones;

  const EvaluarClaseRequest({
    required this.claseCodigo,
    required this.tipo,
    required this.criterios,
    this.observaciones,
  });

  Map<String, dynamic> toJson() {
    final texto = observaciones?.trim();
    return {
      'claseCodigo': claseCodigo,
      'tipo': tipo.valorApi,
      'criterios': criterios.map((c) => c.toJson()).toList(),
      if (texto != null && texto.isNotEmpty) 'observaciones': texto,
    };
  }
}
