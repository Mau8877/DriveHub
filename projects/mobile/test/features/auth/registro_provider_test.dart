import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/models/registro_estudiante_request.dart';
import 'package:mobile/features/auth/models/usuario_registrado.dart';
import 'package:mobile/features/auth/providers/registro_provider.dart';
import 'package:mobile/features/auth/services/registro_service.dart';

class _ServicioFalso extends RegistroService {
  final Object? error;
  // * Un Dio vacío: el real (ApiClient) necesita dotenv cargado.
  _ServicioFalso({this.error}) : super(dio: Dio());

  @override
  Future<UsuarioRegistrado> registrar(RegistroEstudianteRequest request) async {
    if (error != null) throw error!;
    return const UsuarioRegistrado(
      codigo: 'u1',
      email: 'ana@gmail.com',
      nombres: 'Ana',
      apellidos: 'Pérez',
    );
  }
}

void main() {
  final request = RegistroEstudianteRequest(
    nombres: 'Ana',
    apellidos: 'Pérez',
    fechaNacimiento: DateTime(2000, 1, 1),
    email: 'ana@gmail.com',
    password: '123456789',
    escuelaSlug: 'escuelaexample',
  );

  ProviderContainer crear(RegistroService servicio) {
    final container = ProviderContainer(
      overrides: [registroServiceProvider.overrideWithValue(servicio)],
    );
    addTearDown(container.dispose);
    return container;
  }

  test('el estado inicial no tiene datos ni hace llamadas', () {
    final container = crear(_ServicioFalso(error: StateError('no debería llamarse')));

    expect(container.read(registroProvider), const AsyncData<UsuarioRegistrado?>(null));
  });

  test('registrar con éxito deja AsyncData con el usuario', () async {
    final container = crear(_ServicioFalso());

    await container.read(registroProvider.notifier).registrar(request);

    final estado = container.read(registroProvider);
    expect(estado.hasValue, isTrue);
    expect(estado.value!.codigo, 'u1');
  });

  test('un error de negocio deja AsyncError con la excepción original', () async {
    final container = crear(_ServicioFalso(error: ApiException(code: 'ERR_IDE_01', message: 'en uso')));

    await container.read(registroProvider.notifier).registrar(request);

    final estado = container.read(registroProvider);
    expect(estado.hasError, isTrue);
    expect(errorDeRegistro(estado.error!), ErrorRegistro.correoEnUso);
  });

  group('errorDeRegistro', () {
    test('clasifica cada código del backend', () {
      expect(errorDeRegistro(ApiException(code: 'ERR_IDE_01', message: '')), ErrorRegistro.correoEnUso);
      expect(errorDeRegistro(ApiException(code: 'ERR_IDE_02', message: '')), ErrorRegistro.menorDeEdad);
      expect(
        errorDeRegistro(ApiException(code: 'ERR_IDE_03', message: '')),
        ErrorRegistro.escuelaNoDisponible,
      );
      expect(errorDeRegistro(ApiException(code: 'ERR_SYS_01', message: '')), ErrorRegistro.validacion);
    });

    test('cualquier otro error es general', () {
      expect(errorDeRegistro(ApiException(code: 'ERR_SYS_00', message: '')), ErrorRegistro.general);
      expect(errorDeRegistro(Exception('sin red')), ErrorRegistro.general);
    });
  });
}
