import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/evaluar_clase_request.dart';
import '../models/prueba_clase.dart';
import 'agenda_provider.dart';
import 'clases_service_provider.dart';
import 'pruebas_de_clase_provider.dart';

/// Evaluar una clase (CU18). No guarda datos: devuelve la prueba y deja la agenda y las pruebas de esa clase al día.
///
/// * La agenda se refresca tanto si la llamada sale bien como si falla (sin red, un 4xx): el servidor pudo haber
///   alcanzado a guardar la prueba antes de cortarse, y un error como «ya hay una prueba de ese tipo» significa que
///   lo que se ve está viejo. Los errores se propagan: la hoja decide cómo mostrarlos.
/// * Evaluar nunca toca la asistencia, la reserva ni el saldo de horas.
class GestionPruebaClaseNotifier extends Notifier<void> {
  @override
  void build() {}

  Future<PruebaClase> evaluar(EvaluarClaseRequest request) async {
    final PruebaClase prueba;
    try {
      prueba = await ref.read(clasesServiceProvider).evaluarClase(request);
    } catch (_) {
      _refrescar(request.claseCodigo);
      rethrow;
    }
    _refrescar(request.claseCodigo);
    return prueba;
  }

  void _refrescar(String claseCodigo) {
    ref.invalidate(agendaListProvider);
    ref.invalidate(pruebasDeClaseDetailProvider(claseCodigo));
  }
}

final gestionPruebaClaseProvider = NotifierProvider<GestionPruebaClaseNotifier, void>(GestionPruebaClaseNotifier.new);
