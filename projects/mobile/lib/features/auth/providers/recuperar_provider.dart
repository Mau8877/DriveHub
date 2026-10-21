import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'sesion_provider.dart';

/// Acción "pedir el enlace de recuperación" (CU04) y su estado.
///
/// * `AsyncData(false)` = todavía no se pidió; `AsyncData(true)` = se pidió (la
/// * pantalla muestra siempre el mismo mensaje, sin saber si la cuenta existe).
/// * Es autoDispose: al salir de la pantalla vuelve a empezar.
class RecuperarNotifier extends AutoDisposeNotifier<AsyncValue<bool>> {
  @override
  AsyncValue<bool> build() => const AsyncData(false);

  Future<void> solicitar(String email) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(authServiceProvider).recuperar(email);
      return true;
    });
  }
}

final recuperarProvider =
    NotifierProvider.autoDispose<RecuperarNotifier, AsyncValue<bool>>(
      RecuperarNotifier.new,
    );
