import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/services/sesion_storage.dart';

void main() {
  const usuario = UsuarioSesion(
    codigo: 'u1',
    nombres: 'Ana',
    apellidos: 'Pérez',
    roles: ['ESTUDIANTE'],
    escuelaCodigo: 'e1',
  );

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() => FlutterSecureStorage.setMockInitialValues({}));

  test('guarda y lee lo mismo', () async {
    final storage = SesionStorageSegura();

    await storage.guardar(const SesionGuardada(refreshToken: 'refresh', usuario: usuario));
    final leida = await storage.leer();

    expect(leida!.refreshToken, 'refresh');
    expect(leida.usuario.nombres, 'Ana');
    expect(leida.usuario.roles, ['ESTUDIANTE']);
  });

  test('borrar deja null', () async {
    final storage = SesionStorageSegura();
    await storage.guardar(const SesionGuardada(refreshToken: 'refresh', usuario: usuario));

    await storage.borrar();

    expect(await storage.leer(), isNull);
  });

  test('sin nada guardado devuelve null', () async {
    expect(await SesionStorageSegura().leer(), isNull);
  });

  test('un valor corrupto devuelve null y se descarta', () async {
    FlutterSecureStorage.setMockInitialValues({'drivehub.sesion': 'esto no es json'});
    final storage = SesionStorageSegura();

    expect(await storage.leer(), isNull);
    expect(await storage.leer(), isNull);
  });
}
