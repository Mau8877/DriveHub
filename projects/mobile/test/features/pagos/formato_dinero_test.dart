import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/pagos/widgets/formato_dinero.dart';

void main() {
  group('formatearBs', () {
    test('miles con punto, decimales con coma y siempre dos decimales', () {
      expect(formatearBs(1250), 'Bs 1.250,00');
      expect(formatearBs(450), 'Bs 450,00');
      expect(formatearBs(1000), 'Bs 1.000,00');
      expect(formatearBs(999.9), 'Bs 999,90');
    });

    test('el cero y los centavos', () {
      expect(formatearBs(0), 'Bs 0,00');
      expect(formatearBs(0.5), 'Bs 0,50');
      expect(formatearBs(0.05), 'Bs 0,05');
    });

    test('un monto grande y uno con más decimales de los que se muestran', () {
      expect(formatearBs(1234567.891), 'Bs 1.234.567,89');
      expect(formatearBs(12345678), 'Bs 12.345.678,00');
    });

    test('un negativo lleva su signo después del símbolo', () {
      expect(formatearBs(-5), 'Bs -5,00');
      expect(formatearBs(-1500.5), 'Bs -1.500,50');
    });

    test('lo que no es un número se ve como una raya, no como "NaN"', () {
      expect(formatearBs(double.nan), '—');
      expect(formatearBs(double.infinity), '—');
    });
  });

  group('formatearUsd', () {
    test('usa el mismo formato con el símbolo US\$', () {
      expect(formatearUsd(179.6), 'US\$ 179,60');
      expect(formatearUsd(38.23), 'US\$ 38,23');
      expect(formatearUsd(1234.5), 'US\$ 1.234,50');
      expect(formatearUsd(0), 'US\$ 0,00');
    });
  });
}
