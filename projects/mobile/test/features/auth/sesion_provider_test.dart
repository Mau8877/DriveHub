import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/models/sesion_respuesta.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/auth/services/auth_service.dart';
import 'package:mobile/features/auth/services/sesion_storage.dart';

const _usuario = UsuarioSesion(
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  roles: ['ESTUDIANTE'],
);

SesionRespuesta _sesion(String access, String refresh, {int expira = 900}) => SesionRespuesta(
      accessToken: access,
      refreshToken: refresh,
      expiraEnSegundos: expira,
      usuario: _usuario,
    );

class _ServicioFalso extends AuthService {
  _ServicioFalso() : super(dio: Dio());

  int llamadasLogin = 0;
  int llamadasRenovar = 0;
  String? ultimoRefreshUsado;
  Object? errorAlRenovar;
  Object? errorAlLogin;
  Object? errorAlCerrar;
  final cerrados = <String>[];
  SesionRespuesta Function() siguiente = () => _sesion('access-nuevo', 'refresh-nuevo');

  @override
  Future<SesionRespuesta> login(String email, String password) async {
    llamadasLogin++;
    if (errorAlLogin != null) throw errorAlLogin!;
    return _sesion('access-1', 'refresh-1');
  }

  @override
  Future<void> cerrar(String refreshToken) async {
    cerrados.add(refreshToken);
    if (errorAlCerrar != null) throw errorAlCerrar!;
  }

  @override
  Future<SesionRespuesta> renovar(String refreshToken) async {
    llamadasRenovar++;
    ultimoRefreshUsado = refreshToken;
    await Future<void>.delayed(const Duration(milliseconds: 10));
    if (errorAlRenovar != null) throw errorAlRenovar!;
    return siguiente();
  }
}

class _StorageEnMemoria implements SesionStorage {
  SesionGuardada? guardada;
  int borrados = 0;

  @override
  Future<SesionGuardada?> leer() async => guardada;

  @override
  Future<void> guardar(SesionGuardada sesion) async => guardada = sesion;

  @override
  Future<void> borrar() async {
    guardada = null;
    borrados++;
  }
}

void main() {
  late _ServicioFalso servicio;
  late _StorageEnMemoria storage;
  late ProviderContainer container;

  setUp(() {
    servicio = _ServicioFalso();
    storage = _StorageEnMemoria();
    container = ProviderContainer(overrides: [
      authServiceProvider.overrideWithValue(servicio),
      sesionStorageProvider.overrideWithValue(storage),
    ]);
    addTearDown(container.dispose);
  });

  SesionNotifier notifier() => container.read(sesionProvider.notifier);
  EstadoSesion estado() => container.read(sesionProvider);

  test('empieza cargando', () {
    expect(estado().fase, FaseSesion.cargando);
  });

  test('iniciarSesion guarda la sesión, el token y deja conSesion', () async {
    await notifier().iniciarSesion('a@gmail.com', 'clave');

    expect(estado().fase, FaseSesion.conSesion);
    expect(estado().usuario!.nombres, 'Ana');
    expect(notifier().obtenerToken(), 'access-1');
    expect(storage.guardada!.refreshToken, 'refresh-1');
  });

  test('un rechazo del login se relanza como ApiException y no deja sesión', () async {
    servicio.errorAlLogin = ApiException(code: 'ERR_IDE_04', message: 'mal');

    await expectLater(notifier().iniciarSesion('a@gmail.com', 'x'), throwsA(isA<ApiException>()));

    expect(estado().fase, isNot(FaseSesion.conSesion));
    expect(storage.guardada, isNull);
  });

  test('restaurar sin sesión guardada deja sinSesion', () async {
    await notifier().restaurar();

    expect(estado().fase, FaseSesion.sinSesion);
    expect(servicio.llamadasRenovar, 0);
  });

  test('restaurar con sesión guardada la renueva con el refresh guardado y guarda el nuevo', () async {
    storage.guardada = const SesionGuardada(refreshToken: 'refresh-guardado', usuario: _usuario);

    await notifier().restaurar();

    expect(servicio.ultimoRefreshUsado, 'refresh-guardado');
    expect(estado().fase, FaseSesion.conSesion);
    expect(notifier().obtenerToken(), 'access-nuevo');
    expect(storage.guardada!.refreshToken, 'refresh-nuevo');
  });

  test('cerrarSesion avisa al backend con el refresh vigente y deja sinSesion sin motivo', () async {
    await notifier().iniciarSesion('a@gmail.com', 'clave');

    await notifier().cerrarSesion();

    expect(servicio.cerrados, ['refresh-1']);
    expect(estado().fase, FaseSesion.sinSesion);
    expect(estado().motivo, isNull);
    expect(notifier().obtenerToken(), isNull);
    expect(storage.guardada, isNull);
  });

  test('cerrarSesion borra todo aunque el backend falle (ApiException o red)', () async {
    for (final error in <Object>[ApiException(code: 'ERR_SYS_02', message: 'límite'), Exception('sin red')]) {
      await notifier().iniciarSesion('a@gmail.com', 'clave');
      servicio.errorAlCerrar = error;

      await notifier().cerrarSesion();

      expect(estado().fase, FaseSesion.sinSesion);
      expect(storage.guardada, isNull);
      expect(notifier().obtenerToken(), isNull);
    }
  });

  test('cerrarSesion sin sesión guardada no llama al backend', () async {
    await notifier().cerrarSesion();

    expect(servicio.cerrados, isEmpty);
    expect(estado().fase, FaseSesion.sinSesion);
  });

  test('dos renovaciones a la vez hacen una sola llamada', () async {
    await notifier().iniciarSesion('a@gmail.com', 'clave');

    final a = notifier().renovar();
    final b = notifier().renovar();

    expect(await a, 'access-nuevo');
    expect(await b, 'access-nuevo');
    expect(servicio.llamadasRenovar, 1);
  });

  test('un rechazo al renovar borra la sesión y fija el motivo', () async {
    await notifier().iniciarSesion('a@gmail.com', 'clave');
    servicio.errorAlRenovar = ApiException(code: 'ERR_IDE_07', message: 'suspendida');

    expect(await notifier().renovar(), isNull);

    expect(estado().fase, FaseSesion.sinSesion);
    expect(estado().motivo, 'ERR_IDE_07');
    expect(notifier().obtenerToken(), isNull);
    expect(storage.guardada, isNull);
  });

  test('un código que no se explica no deja motivo', () async {
    await notifier().iniciarSesion('a@gmail.com', 'clave');
    servicio.errorAlRenovar = ApiException(code: 'ERR_SYS_00', message: 'x');

    await notifier().renovar();

    expect(estado().fase, FaseSesion.sinSesion);
    expect(estado().motivo, isNull);
  });

  test('un fallo de red al renovar no pierde la sesión', () async {
    await notifier().iniciarSesion('a@gmail.com', 'clave');
    servicio.errorAlRenovar = Exception('sin red');

    expect(await notifier().renovar(), isNull);

    expect(estado().fase, FaseSesion.conSesion);
    expect(storage.guardada, isNotNull);
  });

  testWidgets('renueva sola al 80 % de la vida del token', (tester) async {
    await notifier().iniciarSesion('a@gmail.com', 'clave');
    expect(servicio.llamadasRenovar, 0);

    await tester.pump(const Duration(seconds: 719));
    expect(servicio.llamadasRenovar, 0);

    await tester.pump(const Duration(seconds: 2));
    expect(servicio.llamadasRenovar, 1);

    // * Deja terminar la renovación en curso y apaga el temporizador siguiente antes de cerrar el test
    await tester.pump(const Duration(milliseconds: 50));
    container.dispose();
  });
}
