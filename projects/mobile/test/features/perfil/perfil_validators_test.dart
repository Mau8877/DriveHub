import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/auth/validators/registro_validators.dart'
    as registro;
import 'package:mobile/features/perfil/validators/perfil_validators.dart';

void main() {
  group('datos personales (mismas reglas del registro)', () {
    test('nombres y apellidos: vacío y largo', () {
      expect(validarNombres(''), 'Los nombres son obligatorios');
      expect(validarNombres('   '), 'Los nombres son obligatorios');
      expect(validarNombres('a' * 80), isNull);
      expect(validarNombres('a' * 81), contains('80'));
      expect(validarApellidos(''), 'Los apellidos son obligatorios');
      expect(validarApellidos('a' * 81), contains('80'));
    });

    test('teléfono: opcional y hasta 20 caracteres', () {
      expect(validarTelefono(''), isNull);
      expect(validarTelefono('1' * 20), isNull);
      expect(validarTelefono('1' * 21), contains('20'));
    });

    test('fecha de nacimiento: pasado y 18 años cumplidos', () {
      final hoy = DateTime(2026, 10, 21);
      expect(validarFechaNacimiento(null, hoy: hoy), isNotNull);
      expect(
        validarFechaNacimiento(DateTime(2026, 10, 21), hoy: hoy),
        contains('pasado'),
      );
      expect(
        validarFechaNacimiento(DateTime(2008, 10, 22), hoy: hoy),
        'Debes tener al menos 18 años',
      );
      expect(validarFechaNacimiento(DateTime(2008, 10, 21), hoy: hoy), isNull);
    });

    test('coinciden con las del registro', () {
      for (final valor in ['', 'a' * 80, 'a' * 81]) {
        expect(validarNombres(valor), registro.validarNombres(valor));
        expect(validarApellidos(valor), registro.validarApellidos(valor));
      }
      expect(validarTelefono('1' * 21), registro.validarTelefono('1' * 21));
    });
  });

  group('contraseña', () {
    test('la actual es obligatoria', () {
      expect(validarContrasenaActual(''), isNotNull);
      expect(validarContrasenaActual('x'), isNull);
    });

    test('la nueva: 9 a 64 caracteres', () {
      expect(contrasenaNueva('corta'), contains('entre 9 y 64'));
      expect(contrasenaNueva('a' * 9), isNull);
      expect(contrasenaNueva('a' * 64), isNull);
      expect(contrasenaNueva('a' * 65), contains('entre 9 y 64'));
    });

    test('la confirmación debe coincidir', () {
      expect(confirmacion('', 'nueva-clave-1'), isNotNull);
      expect(
        confirmacion('otra-clave-12', 'nueva-clave-1'),
        'Las contraseñas no coinciden',
      );
      expect(confirmacion('nueva-clave-1', 'nueva-clave-1'), isNull);
    });

    test('la nueva no puede ser igual a la actual', () {
      expect(
        distintaDeLaActual('misma-clave-1', 'misma-clave-1'),
        contains('distinta'),
      );
      expect(distintaDeLaActual('otra-clave-12', 'misma-clave-1'), isNull);
      expect(distintaDeLaActual('', ''), isNull);
    });
  });

  group('correo nuevo', () {
    test('formato y largo', () {
      expect(validarCorreoNuevo(''), 'El correo es obligatorio');
      expect(validarCorreoNuevo('no-es-correo'), 'El correo no es válido');
      expect(validarCorreoNuevo('nuevo@gmail.com'), isNull);
      expect(validarCorreoNuevo('${'a' * 120}@gmail.com'), contains('120'));
    });
  });

  group('mensajes de los códigos de negocio', () {
    test('cada código del perfil tiene su mensaje en español', () {
      expect(mensajeDeCodigo('ERR_IDE_01'), 'Ese correo ya está en uso');
      expect(mensajeDeCodigo('ERR_IDE_02'), 'Debes tener al menos 18 años');
      expect(
        mensajeDeCodigo('ERR_IDE_14'),
        'La contraseña actual no es correcta',
      );
      expect(mensajeDeCodigo('ERR_IDE_15'), contains('distinta'));
      expect(mensajeDeCodigo('ERR_IDE_16'), contains('imagen'));
      expect(mensajeDeCodigo('ERR_IDE_17'), contains('5 MB'));
      expect(mensajeDeCodigo('ERR_IDE_18'), 'Ese ya es tu correo');
    });

    test('un código desconocido da el mensaje genérico', () {
      expect(mensajeDeCodigo('ERR_XX_99'), mensajeGenerico);
      expect(mensajeDeCodigo(null), mensajeGenerico);
    });
  });

  group('CI, sexo y ubicación (mismas reglas del registro)', () {
    test('CI: de 5 a 15 caracteres con letras, números y guion', () {
      expect(validarCi('AB-12345'), isNull);
      expect(validarCi(''), 'El CI es obligatorio');
      expect(validarCi('1234'), contains('entre 5 y 15'));
      expect(validarCi('AB 1234'), contains('entre 5 y 15'));
    });

    test('sexo obligatorio', () {
      expect(validarSexo(''), 'Elige tu sexo');
      expect(validarSexo('MASCULINO'), isNull);
    });

    test('ubicación: dos coordenadas y en rango', () {
      expect(validarUbicacion(null, null), 'Elige tu ubicación en el mapa');
      expect(validarUbicacion(-16.5, -68.1), isNull);
      expect(validarUbicacion(95, 0), 'La ubicación no es válida');
    });

    test('el CI repetido tiene su mensaje en español', () {
      expect(mensajeDeCodigo('ERR_IDE_19'), 'Este CI ya está registrado');
    });
  });
}
