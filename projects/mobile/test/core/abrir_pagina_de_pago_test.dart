import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/abrir_pagina_de_pago.dart';

void main() {
  late List<Uri> abiertas;

  Future<bool> lanzarOk(Uri direccion) async {
    abiertas.add(direccion);
    return true;
  }

  setUp(() => abiertas = []);

  test('abre una dirección https', () async {
    final abrio = await abrirPaginaDePago('https://checkout.stripe.com/c/pay/cs_test_1', lanzar: lanzarOk);

    expect(abrio, isTrue);
    expect(abiertas.single.toString(), 'https://checkout.stripe.com/c/pay/cs_test_1');
  });

  test('rechaza lo que no es https, sin intentar abrirlo', () async {
    for (final url in [
      'http://checkout.stripe.com/pago',
      'javascript:alert(1)',
      'drivehub://app/payments/return',
      'file:///etc/passwd',
      'https://',
      'no es una dirección',
      '',
      '   ',
    ]) {
      expect(await abrirPaginaDePago(url, lanzar: lanzarOk), isFalse, reason: url);
    }
    expect(abiertas, isEmpty);
  });

  test('devuelve false si la plataforma no pudo abrirla', () async {
    expect(await abrirPaginaDePago('https://pago.test/x', lanzar: (_) async => false), isFalse);
  });

  test('un error de la plataforma no sale como excepción', () async {
    final abrio = await abrirPaginaDePago('https://pago.test/x', lanzar: (_) async => throw StateError('sin navegador'));

    expect(abrio, isFalse);
  });

  test('ignora los espacios de los extremos', () async {
    expect(await abrirPaginaDePago('  https://pago.test/x  ', lanzar: lanzarOk), isTrue);
    expect(abiertas.single.host, 'pago.test');
  });
}
