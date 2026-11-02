import '../../horarios/models/fechas.dart';
import 'tipo_prueba.dart';

/// Si la prueba de clase quedó aprobada o reprobada (CU18).
enum ResultadoPrueba {
  aprobado('APROBADO', 'Aprobado'),
  reprobado('REPROBADO', 'Reprobado');

  /// El valor tal como viaja en la API.
  final String valorApi;

  /// El nombre en español para mostrar.
  final String etiqueta;

  const ResultadoPrueba(this.valorApi, this.etiqueta);

  /// Un valor desconocido se trata como [reprobado]: nunca se muestra como aprobada una prueba que no se entiende.
  factory ResultadoPrueba.fromJson(String valor) {
    for (final resultado in values) {
      if (resultado.valorApi == valor) return resultado;
    }
    return ResultadoPrueba.reprobado;
  }
}

/// Un criterio ya puntuado de una prueba guardada.
class CriterioEvaluado {
  final String codigo;
  final String descripcion;
  final double puntaje;
  final double maximo;

  const CriterioEvaluado({
    required this.codigo,
    required this.descripcion,
    required this.puntaje,
    required this.maximo,
  });

  factory CriterioEvaluado.fromJson(Map<String, dynamic> json) {
    return CriterioEvaluado(
      codigo: json['codigo'] as String,
      descripcion: json['descripcion'] as String,
      puntaje: (json['puntaje'] as num).toDouble(),
      maximo: (json['maximo'] as num).toDouble(),
    );
  }
}

/// Una prueba de clase ya guardada (CU18): no se corrige ni se borra.
class PruebaClase {
  final String codigo;
  final String claseCodigo;
  final TipoPrueba tipo;
  final List<CriterioEvaluado> criterios;

  /// Sobre 100, con dos decimales.
  final double puntajeTotal;
  final ResultadoPrueba resultado;
  final String? observaciones;
  final DateTime fecha;

  const PruebaClase({
    required this.codigo,
    required this.claseCodigo,
    required this.tipo,
    required this.criterios,
    required this.puntajeTotal,
    required this.resultado,
    this.observaciones,
    required this.fecha,
  });

  bool get aprobada => resultado == ResultadoPrueba.aprobado;

  factory PruebaClase.fromJson(Map<String, dynamic> json) {
    return PruebaClase(
      codigo: json['codigo'] as String,
      claseCodigo: json['claseCodigo'] as String,
      tipo: TipoPrueba.fromJson(json['tipo'] as String),
      criterios: (json['criterios'] as List<dynamic>)
          .map((e) => CriterioEvaluado.fromJson(e as Map<String, dynamic>))
          .toList(),
      puntajeTotal: (json['puntajeTotal'] as num).toDouble(),
      resultado: ResultadoPrueba.fromJson(json['resultado'] as String),
      observaciones: json['observaciones'] as String?,
      fecha: fechaDesdeApi(json['fecha'] as String),
    );
  }
}
