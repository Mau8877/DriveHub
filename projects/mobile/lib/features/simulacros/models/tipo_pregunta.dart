/// Cómo se responde una pregunta del banco (CU19, CU21).
enum TipoPregunta {
  opcionUnica('OPCION_UNICA'),
  opcionMultiple('OPCION_MULTIPLE'),
  verdaderoFalso('VERDADERO_FALSO');

  final String valorApi;
  const TipoPregunta(this.valorApi);

  /// Se marcan varias opciones; en los otros dos tipos, una sola.
  bool get admiteVarias => this == opcionMultiple;

  static TipoPregunta fromJson(String valor) =>
      values.firstWhere((t) => t.valorApi == valor);
}
