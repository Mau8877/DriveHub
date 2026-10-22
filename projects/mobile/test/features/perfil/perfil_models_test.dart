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
}
