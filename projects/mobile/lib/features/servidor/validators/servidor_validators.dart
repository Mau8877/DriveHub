/// La ruta de la API que cuelga del servidor: el gateway la sirve bajo `/api/v1`.
const String rutaApiPorDefecto = '/api/v1';

const String _mensajeVacio = 'Escribe la dirección del servidor';
const String _mensajeInvalida = 'La dirección no es válida. Ejemplo: http://192.168.1.10:8080';
const String _mensajeEsquema = 'La dirección tiene que empezar con http:// o https://';
const String _mensajeExtras = 'La dirección no debe llevar usuario, parámetros ni #';

final RegExp _hostValido = RegExp(r'^[A-Za-z0-9_]([A-Za-z0-9_.-]*[A-Za-z0-9_])?$');

/// Lee lo que escribió la persona: el [Uri] listo para usar, o el motivo por el que no sirve.
({Uri? uri, String? error}) _interpretar(String? entrada) {
  var texto = entrada?.trim() ?? '';
  if (texto.isEmpty) return (uri: null, error: _mensajeVacio);
  // * Quien escribe `192.168.1.10:8080` no tiene que acordarse del `http://`
  if (!texto.contains('://')) texto = 'http://$texto';

  // * `Uri.tryParse` no rechaza un espacio en el host: lo codifica (`hola mundo` → `hola%20mundo`) y pasa por válido
  if (RegExp(r'\s').hasMatch(texto)) return (uri: null, error: _mensajeInvalida);

  final uri = Uri.tryParse(texto);
  if (uri == null || uri.host.isEmpty) return (uri: null, error: _mensajeInvalida);
  // * Un nombre de host o una IPv4: letras, números, puntos y guiones (una IPv6 lleva `:` y va entre corchetes)
  if (!uri.host.contains(':') && !_hostValido.hasMatch(uri.host)) return (uri: null, error: _mensajeInvalida);
  if (uri.scheme != 'http' && uri.scheme != 'https') return (uri: null, error: _mensajeEsquema);
  if (uri.userInfo.isNotEmpty || uri.hasQuery || uri.hasFragment) return (uri: null, error: _mensajeExtras);
  return (uri: uri, error: null);
}

/// El mensaje de error de la dirección, o `null` si sirve. Es el `validator` del campo.
String? validarDireccionServidor(String? valor) => _interpretar(valor).error;

/// La dirección completa de la API a partir de lo que escribió la persona, o `null` si no sirve.
///
/// * Sin ruta se agrega `/api/v1` (`http://192.168.1.10:8080` → `http://192.168.1.10:8080/api/v1`); `/api` pasa a
///   `/api/v1` (detrás de nginx); cualquier otra ruta se respeta tal cual.
/// * Sin `http://` ni `https://` se toma `http://`. Sin barra final.
String? normalizarDireccionServidor(String? entrada) {
  final uri = _interpretar(entrada).uri;
  if (uri == null) return null;

  var ruta = uri.path.replaceAll(RegExp(r'/+$'), '');
  if (ruta.isEmpty || ruta == '/api') ruta = rutaApiPorDefecto;

  // * Una dirección IPv6 va entre corchetes; `Uri.host` los quita
  final host = uri.host.contains(':') ? '[${uri.host}]' : uri.host;
  final puerto = uri.hasPort ? ':${uri.port}' : '';
  return '${uri.scheme}://$host$puerto$ruta';
}

/// La dirección tal como se le muestra a la persona: sin el `/api/v1` que la app agrega sola.
String direccionParaMostrar(String direccionApi) {
  final sinRuta = direccionApi.replaceFirst(RegExp('$rutaApiPorDefecto/?\$'), '');
  return sinRuta.replaceAll(RegExp(r'/+$'), '');
}
