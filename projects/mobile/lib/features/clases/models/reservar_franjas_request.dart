/// Cuerpo de `POST /reservas` (CU14): las franjas que el estudiante quiere reservar.
class ReservarFranjasRequest {
  final List<String> franjaCodigos;

  const ReservarFranjasRequest({required this.franjaCodigos});

  Map<String, dynamic> toJson() => {'franjaCodigos': franjaCodigos};
}
