import 'package:flutter_riverpod/flutter_riverpod.dart';

/// El texto de búsqueda de «Mis alumnos» (CU29). Estado puro de UI: no guarda datos del backend.
class AlumnosBusquedaNotifier extends Notifier<String> {
  @override
  String build() => '';

  void escribir(String texto) => state = texto;
}

final alumnosBusquedaUiProvider =
    NotifierProvider<AlumnosBusquedaNotifier, String>(
      AlumnosBusquedaNotifier.new,
    );
