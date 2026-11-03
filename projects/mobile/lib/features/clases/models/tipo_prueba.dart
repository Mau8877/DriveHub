/// El tipo de una prueba de clase (CU18), tal como lo entrega y lo recibe la API.
enum TipoPrueba {
  manejo('MANEJO', 'Manejo'),
  estacionamiento('ESTACIONAMIENTO', 'Estacionamiento'),
  ciudad('CIUDAD', 'Ciudad');

  /// El valor tal como viaja en la API.
  final String valorApi;

  /// El nombre en español para mostrar.
  final String etiqueta;

  const TipoPrueba(this.valorApi, this.etiqueta);

  /// Un valor desconocido se trata como [manejo]: el tipo base, para no romper la pantalla ante un tipo nuevo.
  factory TipoPrueba.fromJson(String valor) {
    for (final tipo in values) {
      if (tipo.valorApi == valor) return tipo;
    }
    return TipoPrueba.manejo;
  }
}
