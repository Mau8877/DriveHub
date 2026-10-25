import 'dart:async';
import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:open_filex/open_filex.dart';

import '../services/comprobantes_service.dart';

/// El servicio que baja los PDF de los comprobantes; es un provider para sustituirlo en los tests.
final comprobantesServiceProvider = Provider<ComprobantesService>(
  (ref) => ComprobantesService(),
);

/// Abre un archivo con el visor del teléfono; devuelve `false` si no hay ninguna app que lo abra. Es un provider
/// para sustituirlo en los tests: ninguno abre un visor de verdad.
final abridorDeArchivosProvider = Provider<Future<bool> Function(String ruta)>(
  (ref) => (ruta) async {
    final resultado = await OpenFilex.open(ruta, type: 'application/pdf');
    return resultado.type == ResultType.done;
  },
);

/// No hay ninguna app en el teléfono que abra PDF.
class SinVisorDePdf implements Exception {
  const SinVisorDePdf();
}

/// Descargar y abrir el PDF de un comprobante (CU30), por su código.
///
/// * `descargarYAbrir` no lanza: devuelve `true` si el PDF se abrió; si no, deja el error en el estado (un
///   `ApiException` con su código, [SinVisorDePdf] o uno de red) para que el botón lo muestre.
/// * Atiende una descarga a la vez: un segundo toque mientras hay una en curso no hace nada.
class ComprobanteDescargaNotifier
    extends AutoDisposeFamilyAsyncNotifier<void, String> {
  bool _enCurso = false;

  @override
  FutureOr<void> build(String arg) {}

  Future<bool> descargarYAbrir() async {
    if (_enCurso) return false;
    _enCurso = true;
    state = const AsyncLoading<void>();
    try {
      final File archivo = await ref
          .read(comprobantesServiceProvider)
          .descargarPdf(arg);
      final abierto = await ref.read(abridorDeArchivosProvider)(archivo.path);
      if (!abierto) throw const SinVisorDePdf();
      state = const AsyncData(null);
      return true;
    } catch (error, pila) {
      state = AsyncError(error, pila);
      return false;
    } finally {
      _enCurso = false;
    }
  }
}

final comprobanteDescargaProvider = AsyncNotifierProvider.autoDispose
    .family<ComprobanteDescargaNotifier, void, String>(
      ComprobanteDescargaNotifier.new,
    );
