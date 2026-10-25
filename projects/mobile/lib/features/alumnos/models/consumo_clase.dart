/// Las horas netas de una clase del alumno con el instructor (CU29).
///
/// * [fecha] es la del consumo de horas: Académico no informa la fecha real de la clase.
class ConsumoClase {
  final String claseCodigo;
  final DateTime fecha;
  final int horas;

  const ConsumoClase({
    required this.claseCodigo,
    required this.fecha,
    required this.horas,
  });

  factory ConsumoClase.fromJson(Map<String, dynamic> json) => ConsumoClase(
    claseCodigo: json['claseCodigo'] as String,
    fecha: DateTime.parse(json['fecha'] as String).toLocal(),
    horas: (json['horas'] as num).toInt(),
  );
}
