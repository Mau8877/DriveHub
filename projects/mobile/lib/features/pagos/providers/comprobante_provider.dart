import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/archivos/abridor_de_pdf.dart';
import '../services/comprobantes_service.dart';

// * Los usan los botones y los tests de comprobantes; el abridor de PDF vive en core/ porque lo comparten varias features
export '../../../core/archivos/abridor_de_pdf.dart';

/// El servicio que baja los PDF de los comprobantes; es un provider para sustituirlo en los tests.
final comprobantesServiceProvider = Provider<ComprobantesService>(
  (ref) => ComprobantesService(),
);

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
      final bytes = await ref.read(comprobantesServiceProvider).descargarPdf(arg);
      final abierto = await ref.read(abridorDePdfProvider)(bytes, 'comprobante-$arg.pdf');
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
