import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/perfil/validators/foto_validators.dart';

void main() {
  group('tipoDeImagen', () {
    test(
      'reconoce JPG, PNG y WebP por la extensión, sin importar mayúsculas',
      () {
        expect(tipoDeImagen('a.jpg'), 'image/jpeg');
        expect(tipoDeImagen('a.JPEG'), 'image/jpeg');
        expect(tipoDeImagen('a.png'), 'image/png');
        expect(tipoDeImagen('a.webp'), 'image/webp');
      },
    );

    test('rechaza otros formatos o nombres sin extensión', () {
      expect(tipoDeImagen('a.gif'), isNull);
      expect(tipoDeImagen('a.exe'), isNull);
      expect(tipoDeImagen('sinextension'), isNull);
    });
  });

  group('validarFoto', () {
    String? validar({
      String nombre = 'f.png',
      int bytes = 10,
      int ancho = 100,
      int alto = 100,
    }) => validarFoto(nombre: nombre, bytes: bytes, ancho: ancho, alto: alto);

    test('acepta una foto dentro de los límites', () {
      expect(validar(), isNull);
    });

    test('rechaza un formato no admitido', () {
      expect(validar(nombre: 'f.gif'), mensajeFotoFormato);
    });

    test('5 MB justo pasa y 5 MB + 1 byte no', () {
      expect(validar(bytes: fotoMaxBytes), isNull);
      expect(validar(bytes: fotoMaxBytes + 1), mensajeFotoPeso);
    });

    test('4000×4000 pasa y más de 4000 en un lado no', () {
      expect(validar(ancho: 4000, alto: 4000), isNull);
      expect(validar(ancho: 4001, alto: 10), mensajeFotoDimensiones);
      expect(validar(ancho: 10, alto: 4001), mensajeFotoDimensiones);
    });
  });
}
