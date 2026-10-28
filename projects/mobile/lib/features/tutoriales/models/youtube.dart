/// El identificador de 11 caracteres de un video de YouTube, o `null` si [url] no es un enlace de YouTube.
///
/// * Acepta los mismos enlaces que el backend (`watch?v=`, `youtu.be/`, `shorts/` y `embed/`, siempre por https).
final _patronYoutube = RegExp(
  r'^https://(?:(?:www\.|m\.)?youtube\.com/(?:watch\?(?:[^#]*&)?v=|shorts/|embed/)|youtu\.be/)([A-Za-z0-9_-]{11})(?:[&?#/].*)?$',
);

String? idDeYoutube(String url) =>
    _patronYoutube.firstMatch(url.trim())?.group(1);
