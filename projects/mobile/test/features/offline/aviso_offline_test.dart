import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/offline/almacen_offline.dart';
import 'package:mobile/core/offline/nucleo_offline.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/offline/providers/offline_provider.dart';
import 'package:mobile/features/offline/widgets/aviso_offline.dart';
import 'package:dio/dio.dart';

NucleoOffline _nucleo() => NucleoOffline(
  almacen: AlmacenEnMemoria(),
  dio: () => Dio(),
  hayToken: () => false,
  sondeo: () async => false,
);

Future<void> _montar(WidgetTester tester, NucleoOffline nucleo) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [nucleoOfflineProvider.overrideWithValue(nucleo)],
      child: MaterialApp(
        theme: AppTheme.light,
        home: const Scaffold(body: AvisoOffline()),
      ),
    ),
  );
}

/// Desmonta el widget y libera el núcleo: el sondeo deja un Timer que el test no puede dejar pendiente.
Future<void> _liberar(WidgetTester tester, NucleoOffline nucleo) async {
  await tester.pumpWidget(const SizedBox());
  nucleo.dispose();
}

void main() {
  testWidgets('sin nada que decir no muestra nada', (tester) async {
    final nucleo = _nucleo();
    addTearDown(nucleo.dispose);

    await _montar(tester, nucleo);

    expect(find.textContaining('conexión'), findsNothing);
  });

  testWidgets('sin conexión advierte y promete avisar qué se cumplió y qué no', (tester) async {
    final nucleo = _nucleo();
    await _montar(tester, nucleo);

    nucleo.marcarSinConexion();
    await tester.pump();

    expect(find.textContaining('Sin conexión'), findsOneWidget);
    expect(find.textContaining('cuáles operaciones se cumplieron y cuáles no'), findsOneWidget);
    await _liberar(tester, nucleo);
  });

  testWidgets('con una acción pendiente sin conexión cuenta las pendientes', (tester) async {
    final nucleo = _nucleo();
    await nucleo.abrirSesion('u1');
    await _montar(tester, nucleo);

    nucleo.marcarSinConexion();
    await nucleo.encolar(metodo: 'POST', ruta: '/reservas', descripcion: 'Reserva de 1 clase');
    await tester.pump();

    expect(find.textContaining('1 operación pendiente'), findsOneWidget);
    await _liberar(tester, nucleo);
  });
}
