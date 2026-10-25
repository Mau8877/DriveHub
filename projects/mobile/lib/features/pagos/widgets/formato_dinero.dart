/// Cómo se muestra el dinero en la app (CU26): `Bs 1.250,00` y `US$ 179,60` (miles con punto, decimales con
/// coma, siempre dos decimales), igual en cualquier teléfono.
///
/// * Solo para MOSTRAR: la app no suma ni compara dinero; el backend manda.
/// * Sin `intl`: son dos monedas y un formato fijo, no hace falta traer la librería.
String formatearBs(double monto) => _conSimbolo('Bs', monto);

String formatearUsd(double monto) => _conSimbolo('US\$', monto);

String _conSimbolo(String simbolo, double monto) {
  if (monto.isNaN || monto.isInfinite) return '—';
  final negativo = monto < 0;
  final texto = monto.abs().toStringAsFixed(2);
  final partes = texto.split('.');
  final entero = partes[0];
  final miles = StringBuffer();
  for (var i = 0; i < entero.length; i++) {
    if (i > 0 && (entero.length - i) % 3 == 0) miles.write('.');
    miles.write(entero[i]);
  }
  return '$simbolo ${negativo ? '-' : ''}$miles,${partes[1]}';
}
