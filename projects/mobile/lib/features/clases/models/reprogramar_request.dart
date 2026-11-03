/// Cuerpo de `POST /reservas/{codigo}/reprogramar` (CU15): la franja libre a la que se mueve la clase.
class ReprogramarRequest {
  final String franjaCodigo;

  const ReprogramarRequest({required this.franjaCodigo});

  Map<String, dynamic> toJson() => {'franjaCodigo': franjaCodigo};
}
