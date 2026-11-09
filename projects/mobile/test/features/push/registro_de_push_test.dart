import 'dart:async';

import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/push/services/registro_de_push.dart';

import '../../helpers/push_falsos.dart';

void main() {
  late MensajeriaFalsa mensajeria;
  late DispositivosFalso dispositivos;
  late DispositivoStorageEnMemoria storage;
  late RegistroDePush registro;

  setUp(() {
    // * El fake hereda el constructor real, que crea el cliente HTTP y lee el .env
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    mensajeria = MensajeriaFalsa();
    dispositivos = DispositivosFalso();
    storage = DispositivoStorageEnMemoria();
    registro = RegistroDePush(mensajeria: mensajeria, dispositivos: dispositivos, storage: storage);
    addTearDown(registro.liberar);
  });

  group('activar', () {
    test('pide el permiso, obtiene el token, lo registra y guarda el código del dispositivo', () async {
      await registro.activar('u1');

      expect(mensajeria.permisosPedidos, 1);
      expect(dispositivos.registrados, ['token-1']);
      expect(storage.codigo, 'dispositivo-1');
      expect(registro.estaRegistradoPara('u1'), true);
    });

    test('con la persona ya registrada no vuelve a pedir permiso ni a registrar (se llama en cada cambio de sesión)', () async {
      await registro.activar('u1');
      await registro.activar('u1');
      await registro.activar('u1');

      expect(mensajeria.permisosPedidos, 1);
      expect(dispositivos.registrados, hasLength(1));
    });

    test('dos activaciones a la vez registran una sola vez', () async {
      final salida = Completer<void>();
      dispositivos.antesDeRegistrar = () => salida.future;

      final a = registro.activar('u1');
      final b = registro.activar('u1');
      salida.complete();
      await Future.wait([a, b]);

      expect(dispositivos.registrados, hasLength(1));
    });

    test('si la persona niega el permiso no registra nada y no vuelve a insistir', () async {
      mensajeria.permiso = false;

      await registro.activar('u1');
      await registro.activar('u1');

      expect(dispositivos.registrados, isEmpty);
      expect(registro.estaRegistradoPara('u1'), false);
      expect(mensajeria.permisosPedidos, 1);
      expect(mensajeria.tokensPedidos, 0);
    });

    test('sin token de Firebase no registra y no falla', () async {
      mensajeria.token = null;

      await registro.activar('u1');

      expect(dispositivos.registrados, isEmpty);
      expect(registro.estaRegistradoPara('u1'), false);
    });

    test('un token vacío tampoco se registra', () async {
      mensajeria.token = '';

      await registro.activar('u1');

      expect(dispositivos.registrados, isEmpty);
    });

    test('si Firebase no está disponible (sin Google Play Services) no lanza y el resto de la app sigue', () async {
      mensajeria.errorAlPedirPermiso = Exception('Google Play Services no disponible');

      await expectLater(registro.activar('u1'), completes);

      expect(dispositivos.registrados, isEmpty);
    });

    test('si el registro falla (sin red) no lanza, no queda como registrado y se reintenta en el siguiente cambio de sesión', () async {
      dispositivos.errorAlRegistrar = Exception('sin red');
      await expectLater(registro.activar('u1'), completes);
      expect(registro.estaRegistradoPara('u1'), false);
      expect(storage.codigo, isNull);

      dispositivos.errorAlRegistrar = null;
      await registro.activar('u1');

      expect(dispositivos.registrados, ['token-1']);
      expect(registro.estaRegistradoPara('u1'), true);
    });

    test('con otra persona en el mismo teléfono se registra de nuevo', () async {
      await registro.activar('u1');
      await registro.activar('u2');

      expect(dispositivos.registrados, hasLength(2));
      expect(registro.estaRegistradoPara('u2'), true);
      expect(registro.estaRegistradoPara('u1'), false);
    });
  });

  group('cuando Firebase cambia el token', () {
    test('registra el token nuevo', () async {
      await registro.activar('u1');

      mensajeria.renovarToken('token-2');
      await pumpEventQueue();

      expect(dispositivos.registrados, ['token-1', 'token-2']);
    });

    test('sin nadie con sesión no registra nada', () async {
      mensajeria.renovarToken('token-2');
      await pumpEventQueue();

      expect(dispositivos.registrados, isEmpty);
    });

    test('un fallo al registrar el token nuevo no rompe nada', () async {
      await registro.activar('u1');
      dispositivos.errorAlRegistrar = Exception('sin red');

      mensajeria.renovarToken('token-2');
      await pumpEventQueue();

      expect(dispositivos.registrados, ['token-1']);
    });

    test('solo se sigue el token una vez, aunque se active varias veces', () async {
      await registro.activar('u1');
      await registro.activar('u2');

      mensajeria.renovarToken('token-2');
      await pumpEventQueue();

      // * Una suscripción: un registro por token nuevo (más los dos de las personas)
      expect(dispositivos.registrados.where((t) => t == 'token-2'), hasLength(1));
    });
  });

  group('desactivar (cerrar sesión)', () {
    test('quita el dispositivo por su código, borra lo guardado y deja de seguir el token', () async {
      await registro.activar('u1');

      await registro.desactivar();

      expect(dispositivos.quitados, ['dispositivo-1']);
      expect(storage.codigo, isNull);
      expect(registro.estaRegistradoPara('u1'), false);
      expect(mensajeria.hayAlguienSiguiendoElToken, false);
    });

    test('tras cerrar sesión, un token nuevo ya no se registra', () async {
      await registro.activar('u1');
      await registro.desactivar();

      mensajeria.renovarToken('token-2');
      await pumpEventQueue();

      expect(dispositivos.registrados, ['token-1']);
    });

    test('si no había nada registrado, no llama a quitar', () async {
      await registro.desactivar();

      expect(dispositivos.quitados, isEmpty);
    });

    test('sin red no lanza y olvida lo local igual', () async {
      await registro.activar('u1');
      dispositivos.errorAlQuitar = Exception('sin red');

      await expectLater(registro.desactivar(), completes);

      expect(storage.codigo, isNull);
      expect(registro.estaRegistradoPara('u1'), false);
    });

    test('espera a un registro que está en curso, así no deja un dispositivo huérfano', () async {
      final salida = Completer<void>();
      dispositivos.antesDeRegistrar = () => salida.future;
      final activando = registro.activar('u1');
      await pumpEventQueue();

      final desactivando = registro.desactivar();
      salida.complete();
      await Future.wait([activando, desactivando]);

      expect(dispositivos.quitados, ['dispositivo-1']);
      expect(storage.codigo, isNull);
    });

    test('se puede volver a activar después de cerrar sesión', () async {
      await registro.activar('u1');
      await registro.desactivar();

      await registro.activar('u1');

      expect(dispositivos.registrados, ['token-1', 'token-1']);
      expect(registro.estaRegistradoPara('u1'), true);
    });
  });

}
