import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/auth/validators/registro_validators.dart';

void main() {
  final hoy = DateTime(2026, 10, 19);

  group('validarFechaNacimiento', () {
    test('rechaza a quien cumple 18 mañana', () {
      expect(validarFechaNacimiento(DateTime(2008, 10, 20), hoy: hoy), 'Debes tener al menos 18 años');
    });

    test('acepta a quien cumple 18 hoy', () {
      expect(validarFechaNacimiento(DateTime(2008, 10, 19), hoy: hoy), isNull);
    });

    test('rechaza una fecha futura', () {
      expect(
        validarFechaNacimiento(DateTime(2027, 1, 1), hoy: hoy),
        'La fecha de nacimiento debe estar en el pasado',
      );
    });

    test('es obligatoria', () {
      expect(validarFechaNacimiento(null, hoy: hoy), 'La fecha de nacimiento es obligatoria');
    });
  });

  group('validarPassword', () {
    test('rechaza 8, acepta 9 y rechaza 65 caracteres', () {
      expect(validarPassword('a' * 8), isNotNull);
      expect(validarPassword('a' * 9), isNull);
      expect(validarPassword('a' * 65), isNotNull);
    });

    test('es obligatoria', () {
      expect(validarPassword(''), 'La contraseña es obligatoria');
    });
  });

  group('validarEmail', () {
    test('rechaza un correo sin arroba', () {
      expect(validarEmail('ana.gmail.com'), 'El correo no es válido');
    });

    test('acepta un correo válido', () {
      expect(validarEmail('ana@gmail.com'), isNull);
    });

    test('es obligatorio', () {
      expect(validarEmail('  '), 'El correo es obligatorio');
    });
  });

  group('otros campos', () {
    test('el teléfono vacío es válido y uno largo no', () {
      expect(validarTelefono(''), isNull);
      expect(validarTelefono(null), isNull);
      expect(validarTelefono('1' * 21), isNotNull);
    });

    test('nombres y apellidos son obligatorios', () {
      expect(validarNombres(''), 'Los nombres son obligatorios');
      expect(validarApellidos('   '), 'Los apellidos son obligatorios');
      expect(validarNombres('Ana'), isNull);
    });

    test('el código de escuela es obligatorio', () {
      expect(validarCodigoEscuela(''), 'El código de la escuela es obligatorio');
      expect(validarCodigoEscuela('escuelaexample'), isNull);
    });
  });
}
