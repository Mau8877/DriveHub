import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/actualizar_clase_request.dart';
import '../models/cerrar_clase_request.dart';
import '../models/clase_dictada.dart';
import 'agenda_provider.dart';
import 'clase_dictada_provider.dart';
import 'clases_service_provider.dart';

/// Cerrar una clase y corregir una ya cerrada (CU17). No guarda datos: devuelve el resultado y deja la agenda al día.
///
/// * La agenda se refresca tanto si la llamada sale bien como si falla (sin red, un 4xx): el servidor pudo haber
///   alcanzado a cerrar o corregir antes de cortarse. Los errores se propagan: la hoja decide cómo mostrarlos.
/// * Corregir también refresca lo guardado de esa clase, para que la hoja no muestre datos viejos.
/// * No toca el saldo de horas: cerrar nunca devuelve ni descuenta una hora.
class GestionClaseNotifier extends Notifier<void> {
  @override
  void build() {}

  Future<ClaseDictada> cerrar(CerrarClaseRequest request) async {
    final ClaseDictada clase;
    try {
      clase = await ref.read(clasesServiceProvider).cerrarClase(request);
    } catch (_) {
      ref.invalidate(agendaListProvider);
      rethrow;
    }
    ref.invalidate(agendaListProvider);
    return clase;
  }

  Future<ClaseDictada> actualizar(String codigoClase, ActualizarClaseRequest request) async {
    final ClaseDictada clase;
    try {
      clase = await ref.read(clasesServiceProvider).actualizarClase(codigoClase, request);
    } catch (_) {
      _refrescar(codigoClase);
      rethrow;
    }
    _refrescar(codigoClase);
    return clase;
  }

  void _refrescar(String codigoClase) {
    ref.invalidate(agendaListProvider);
    ref.invalidate(claseDictadaDetailProvider(codigoClase));
  }
}

final gestionClaseProvider = NotifierProvider<GestionClaseNotifier, void>(GestionClaseNotifier.new);
