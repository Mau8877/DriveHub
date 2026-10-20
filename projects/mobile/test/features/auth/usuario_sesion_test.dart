import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/auth/models/usuario_sesion.dart';

void main() {
  const json = {
    'codigo': 'u1',
    'nombres': 'Ana',
    'apellidos': 'Pérez',
    'roles': ['ESTUDIANTE'],
    'escuelaCodigo': 'e1',
  };

  test('lee correoVerificado del JSON', () {
    final usuario = UsuarioSesion.fromJson({...json, 'correoVerificado': true});

    expect(usuario.correoVerificado, isTrue);
  });

  test('una sesión guardada antes del campo se lee como sin verificar', () {
    final usuario = UsuarioSesion.fromJson(json);

    expect(usuario.correoVerificado, isFalse);
  });

  test('toJson escribe correoVerificado y vuelve a leerse igual', () {
    const usuario = UsuarioSesion(
      codigo: 'u1',
      nombres: 'Ana',
      apellidos: 'Pérez',
      roles: ['ESTUDIANTE'],
      correoVerificado: true,
    );

    expect(usuario.toJson()['correoVerificado'], isTrue);
    expect(UsuarioSesion.fromJson(usuario.toJson()).correoVerificado, isTrue);
  });
}
