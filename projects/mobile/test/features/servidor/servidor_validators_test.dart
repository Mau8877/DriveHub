import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/servidor/validators/servidor_validators.dart';

void main() {
  group('normalizarDireccionServidor', () {
    test('agrega /api/v1 cuando solo se escribe el servidor', () {
      expect(normalizarDireccionServidor('http://192.168.1.10:8080'), 'http://192.168.1.10:8080/api/v1');
    });

    test('sin http:// ni https:// toma http://', () {
      expect(normalizarDireccionServidor('192.168.1.10:8080'), 'http://192.168.1.10:8080/api/v1');
      expect(normalizarDireccionServidor('localhost'), 'http://localhost/api/v1');
      expect(normalizarDireccionServidor('10.0.2.2:8080'), 'http://10.0.2.2:8080/api/v1');
    });

    test('respeta https', () {
      expect(normalizarDireccionServidor('https://api.miescuela.com'), 'https://api.miescuela.com/api/v1');
    });

    test('quita los espacios y la barra final', () {
      expect(normalizarDireccionServidor('  http://host:8080/  '), 'http://host:8080/api/v1');
      expect(normalizarDireccionServidor('http://host:8080///'), 'http://host:8080/api/v1');
    });

    test('/api pasa a /api/v1 (detrás de nginx)', () {
      expect(normalizarDireccionServidor('http://host/api'), 'http://host/api/v1');
      expect(normalizarDireccionServidor('http://host/api/'), 'http://host/api/v1');
    });

    test('una dirección que ya trae /api/v1 se deja igual', () {
      expect(normalizarDireccionServidor('http://host:8080/api/v1'), 'http://host:8080/api/v1');
      expect(normalizarDireccionServidor('http://host:8080/api/v1/'), 'http://host:8080/api/v1');
    });

    test('cualquier otra ruta se respeta tal cual', () {
      expect(normalizarDireccionServidor('http://host/drivehub/api/v1'), 'http://host/drivehub/api/v1');
    });

    test('una dirección IPv6 conserva los corchetes', () {
      expect(normalizarDireccionServidor('[::1]:8080'), 'http://[::1]:8080/api/v1');
    });

    test('lo que no es una dirección da null', () {
      for (final mala in ['', '   ', null, 'ftp://host', 'http://', 'http://usuario:clave@host', 'http://host?x=1', 'http://host#a', 'hola mundo']) {
        expect(normalizarDireccionServidor(mala), isNull, reason: 'debería rechazar "$mala"');
      }
    });

    test('un espacio o un carácter raro en el host se rechaza, no se codifica', () {
      for (final mala in ['hola mundo', 'http://host :8080', 'http://ho st/api', 'http://ho%20st', 'http://host!', 'http://-host', 'http://host-']) {
        expect(normalizarDireccionServidor(mala), isNull, reason: 'debería rechazar "$mala"');
        expect(validarDireccionServidor(mala), isNotNull, reason: 'el validador también "$mala"');
      }
    });

    test('un nombre de host con guiones, números y subdominios sí se acepta', () {
      expect(normalizarDireccionServidor('mi-escuela.drivehub.app'), 'http://mi-escuela.drivehub.app/api/v1');
      expect(normalizarDireccionServidor('drivehub_gateway:8080'), 'http://drivehub_gateway:8080/api/v1');
    });

    test('es idempotente: normalizar lo ya normalizado no cambia nada', () {
      const normal = 'http://192.168.1.10:8080/api/v1';

      expect(normalizarDireccionServidor(normal), normal);
    });
  });

  group('validarDireccionServidor', () {
    test('una dirección válida no tiene error', () {
      expect(validarDireccionServidor('http://192.168.1.10:8080'), isNull);
      expect(validarDireccionServidor('192.168.1.10:8080'), isNull);
      expect(validarDireccionServidor('https://api.miescuela.com'), isNull);
    });

    test('vacía pide escribirla', () {
      expect(validarDireccionServidor(''), 'Escribe la dirección del servidor');
      expect(validarDireccionServidor('   '), 'Escribe la dirección del servidor');
      expect(validarDireccionServidor(null), 'Escribe la dirección del servidor');
    });

    test('con otro protocolo explica que tiene que ser http o https', () {
      expect(validarDireccionServidor('ftp://host'), 'La dirección tiene que empezar con http:// o https://');
    });

    test('sin servidor da un ejemplo', () {
      expect(validarDireccionServidor('http://'), contains('Ejemplo: http://192.168.1.10:8080'));
    });

    test('con usuario, parámetros o # lo rechaza', () {
      const mensaje = 'La dirección no debe llevar usuario, parámetros ni #';

      expect(validarDireccionServidor('http://u:p@host'), mensaje);
      expect(validarDireccionServidor('http://host?a=1'), mensaje);
      expect(validarDireccionServidor('http://host#x'), mensaje);
    });

    test('valida lo mismo que normaliza: si el validador deja pasar, normalizar da una dirección', () {
      for (final entrada in ['10.0.2.2:8080', 'http://host/api', 'https://x.com', 'localhost', '[::1]:9']) {
        expect(validarDireccionServidor(entrada), isNull);
        expect(normalizarDireccionServidor(entrada), isNotNull);
      }
    });
  });

  group('direccionParaMostrar', () {
    test('quita el /api/v1 que la app agrega sola', () {
      expect(direccionParaMostrar('http://192.168.1.10:8080/api/v1'), 'http://192.168.1.10:8080');
      expect(direccionParaMostrar('http://192.168.1.10:8080/api/v1/'), 'http://192.168.1.10:8080');
    });

    test('una ruta distinta se muestra completa', () {
      expect(direccionParaMostrar('http://host/drivehub'), 'http://host/drivehub');
    });

    test('mostrar y volver a normalizar da la misma dirección', () {
      const original = 'http://10.0.2.2:8080/api/v1';

      expect(normalizarDireccionServidor(direccionParaMostrar(original)), original);
    });
  });
}
