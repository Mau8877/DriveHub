/// Un intento en el historial del estudiante (CU21), sin las preguntas.
class IntentoResumen {
  final String codigo;
  final String simulacroNombre;
  final String estado;
  final DateTime iniciadoEn;
  final int aciertos;
  final int total;
  final double puntaje;
  final bool aprobado;

  const IntentoResumen({
    required this.codigo,
    required this.simulacroNombre,
    required this.estado,
    required this.iniciadoEn,
    required this.aciertos,
    required this.total,
    required this.puntaje,
    required this.aprobado,
  });

  bool get expirado => estado == 'EXPIRADO';

  factory IntentoResumen.fromJson(Map<String, dynamic> json) {
    return IntentoResumen(
      codigo: json['codigo'] as String,
      simulacroNombre: json['simulacroNombre'] as String,
      estado: json['estado'] as String,
      iniciadoEn: DateTime.parse(json['iniciadoEn'] as String).toLocal(),
      aciertos: json['aciertos'] as int,
      total: json['total'] as int,
      puntaje: (json['puntaje'] as num).toDouble(),
      aprobado: json['aprobado'] as bool,
    );
  }
}
