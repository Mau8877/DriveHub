import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/registro_estudiante_request.dart';
import '../models/usuario_registrado.dart';
import '../services/registro_service.dart';

/// Servicio del registro; es un provider aparte para poder sustituirlo en tests.
final registroServiceProvider = Provider<RegistroService>((ref) => RegistroService());

/// Cómo clasifica la pantalla un error del registro para mostrarlo bajo el
/// campo que corresponde.
enum ErrorRegistro {
  correoEnUso,
  menorDeEdad,
  escuelaNoDisponible,
  validacion,
  general,
}

/// Traduce el código de negocio del backend a un [ErrorRegistro].
ErrorRegistro errorDeRegistro(Object error) {
  if (error is ApiException) {
    switch (error.code) {
      case 'ERR_IDE_01':
        return ErrorRegistro.correoEnUso;
      case 'ERR_IDE_02':
        return ErrorRegistro.menorDeEdad;
      case 'ERR_IDE_03':
        return ErrorRegistro.escuelaNoDisponible;
      case 'ERR_SYS_01':
        return ErrorRegistro.validacion;
    }
  }
  return ErrorRegistro.general;
}

/// Acción "registrar estudiante" (CU01) y su estado.
///
/// * El estado inicial es `AsyncData(null)`: no hace ninguna llamada al
/// * construirse. No guarda token ni sesión (el registro no inicia sesión).
class RegistroNotifier extends Notifier<AsyncValue<UsuarioRegistrado?>> {
  @override
  AsyncValue<UsuarioRegistrado?> build() => const AsyncData(null);

  Future<void> registrar(RegistroEstudianteRequest request) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() => ref.read(registroServiceProvider).registrar(request));
  }
}

final registroProvider =
    NotifierProvider<RegistroNotifier, AsyncValue<UsuarioRegistrado?>>(RegistroNotifier.new);
