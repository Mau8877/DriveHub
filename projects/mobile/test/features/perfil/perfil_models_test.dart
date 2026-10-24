import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/perfil/models/cambiar_contrasena_request.dart';
import 'package:mobile/features/perfil/models/cambiar_correo_request.dart';
import 'package:mobile/features/perfil/models/editar_perfil_request.dart';
import 'package:mobile/features/perfil/models/perfil.dart';

Map<String, dynamic> _json({
  String? telefono = '0991234567',
  String? escuelaNombre = 'Autoescuela Sol',
  String? fotoUrl,
}) => {
  'codigo': 'u1',
  'nombres': 'Ana',
  'apellidos': 'Pérez',
  'fechaNacimiento': '2000-01-31',
  'telefono': telefono,
  'email': 'ana@gmail.com',
  'correoVerificado': true,
  'roles': ['ESTUDIANTE'],
  'escuelaCodigo': escuelaNombre == null ? null : 'e1',
  'escuelaNombre': escuelaNombre,
  'fotoUrl': fotoUrl,
  'ci': 'AB12345',
  'sexo': 'FEMENINO',
  'direccion': 'Av. Arce 2345, La Paz',
  'latitud': -16.4897,
  'longitud': -68.1193,
  'categoriaSigla': 'SIN',
};

void main() {
  group('Perfil.fromJson', () {
    test('con escuela, teléfono y foto', () {
      final perfil = Perfil.fromJson(_json(fotoUrl: 'http://minio/f.png'));

      expect(perfil.codigo, 'u1');
      expect(perfil.fechaNacimiento, DateTime(2000, 1, 31));
      expect(perfil.telefono, '0991234567');
      expect(perfil.correoVerificado, isTrue);
      expect(perfil.roles, ['ESTUDIANTE']);
      expect(perfil.escuelaCodigo, 'e1');
      expect(perfil.escuelaNombre, 'Autoescuela Sol');
      expect(perfil.fotoUrl, 'http://minio/f.png');
    });

    test('sin escuela, sin teléfono y sin foto (Super admin)', () {
      final perfil = Perfil.fromJson(
        _json(telefono: null, escuelaNombre: null),
      );

      expect(perfil.telefono, isNull);
      expect(perfil.escuelaCodigo, isNull);
      expect(perfil.escuelaNombre, isNull);
      expect(perfil.fotoUrl, isNull);
    });
  });

  group('EditarPerfilRequest.toJson', () {
    test('solo incluye los campos presentes', () {
      const request = EditarPerfilRequest(nombres: 'Luisa');

      expect(request.toJson(), {'nombres': 'Luisa'});
      expect(request.estaVacio, isFalse);
    });

    test('borrar el teléfono manda null explícito', () {
      const request = EditarPerfilRequest(borrarTelefono: true);

      expect(request.toJson(), {'telefono': null});
      expect(request.toJson().containsKey('telefono'), isTrue);
    });

    test('la fecha va como yyyy-MM-dd', () {
      final request = EditarPerfilRequest(
        fechaNacimiento: DateTime(1995, 5, 3),
      );

      expect(request.toJson(), {'fechaNacimiento': '1995-05-03'});
    });

    test('sin campos está vacío y su JSON también', () {
      const request = EditarPerfilRequest();

      expect(request.estaVacio, isTrue);
      expect(request.toJson(), isEmpty);
    });
  });

  test('CambiarContrasenaRequest.toJson lleva dispositivo MOVIL', () {
    const request = CambiarContrasenaRequest(
      passwordActual: 'actual-123',
      passwordNueva: 'nueva-clave-1',
    );

    expect(request.toJson(), {
      'passwordActual': 'actual-123',
      'passwordNueva': 'nueva-clave-1',
      'dispositivo': 'MOVIL',
    });
  });

  test('CambiarCorreoRequest.toJson', () {
    const request = CambiarCorreoRequest(
      passwordActual: 'actual-123',
      correoNuevo: 'nuevo@gmail.com',
    );

    expect(request.toJson(), {
      'passwordActual': 'actual-123',
      'correoNuevo': 'nuevo@gmail.com',
    });
  });

  group('CU08a: CI, sexo, ubicación y categoría', () {
    test('Perfil.fromJson lee CI, sexo, dirección, coordenadas y categoría', () {
      final perfil = Perfil.fromJson(_json());

      expect(perfil.ci, 'AB12345');
      expect(perfil.sexo, 'FEMENINO');
      expect(perfil.direccion, 'Av. Arce 2345, La Paz');
      expect(perfil.latitud, -16.4897);
      expect(perfil.longitud, -68.1193);
      expect(perfil.categoriaSigla, 'SIN');
    });

    test('Perfil.fromJson acepta coordenadas enteras y los campos de ubicación nulos', () {
      final entero = Perfil.fromJson({..._json(), 'latitud': -17, 'longitud': -63});
      final sinUbicacion = Perfil.fromJson({
        ..._json(),
        'direccion': null,
        'latitud': null,
        'longitud': null,
        'categoriaSigla': null,
      });

      expect(entero.latitud, -17.0);
      expect(sinUbicacion.direccion, isNull);
      expect(sinUbicacion.latitud, isNull);
      expect(sinUbicacion.categoriaSigla, isNull);
    });

    test('EditarPerfilRequest manda CI y sexo, y la ubicación solo como par', () {
      const request = EditarPerfilRequest(
        ci: 'CD67890',
        sexo: 'MASCULINO',
        latitud: -17.7833,
        longitud: -63.1821,
      );

      expect(request.toJson(), {
        'ci': 'CD67890',
        'sexo': 'MASCULINO',
        'latitud': -17.7833,
        'longitud': -63.1821,
      });
      expect(request.estaVacio, isFalse);
    });

    test('una sola coordenada no se manda', () {
      const request = EditarPerfilRequest(latitud: -17.7833);

      expect(request.toJson(), isEmpty);
    });

    test('nunca manda dirección ni categoría', () {
      const request = EditarPerfilRequest(ci: 'CD67890');

      expect(request.toJson().containsKey('direccion'), isFalse);
      expect(request.toJson().containsKey('categoriaSigla'), isFalse);
    });
  });
}
