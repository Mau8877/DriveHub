import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/pagos/providers/comprobante_provider.dart';
import 'package:mobile/features/pagos/widgets/boton_comprobante.dart';

import '../../helpers/comprobantes_falsos.dart';

void main() {
  late ComprobantesServiceFalso servicio;
  late AbridorFalso abridor;

  setUp(() {
    servicio = ComprobantesServiceFalso();
    abridor = AbridorFalso();
  });

  Future<void> montar(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: comprobantesFalsos(servicio, abridor),
        child: MaterialApp(
          theme: AppTheme.light,
          home: const Scaffold(body: Center(child: BotonComprobante(codigo: 'c1'))),
        ),
      ),
    );
  }

  Future<void> tocar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('botonComprobante-c1')));
    await tester.pumpAndSettle();
  }

  testWidgets('descarga el PDF del comprobante y lo abre con el visor', (tester) async {
    await montar(tester);

    await tocar(tester);

    expect(servicio.descargas, ['c1']);
    expect(abridor.abiertos.single, 'comprobante-c1.pdf');
    expect(find.byType(SnackBar), findsNothing);
  });

  testWidgets('mientras descarga muestra el spinner y se deshabilita', (tester) async {
    final liberar = Completer<void>();
    servicio.antes = () => liberar.future;
    await montar(tester);

    await tester.tap(find.byKey(const Key('botonComprobante-c1')));
    await tester.pump();

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(tester.widget<OutlinedButton>(find.byKey(const Key('botonComprobante-c1'))).onPressed, isNull);

    liberar.complete();
    await tester.pumpAndSettle();
    expect(find.byType(CircularProgressIndicator), findsNothing);
  });

  testWidgets('ERR_PAG_14 avisa que todavía se está preparando', (tester) async {
    servicio.error = ApiException(code: 'ERR_PAG_14', message: 'x');
    await montar(tester);

    await tocar(tester);

    expect(find.text('Tu comprobante todavía se está preparando, inténtalo en unos minutos.'), findsOneWidget);
    expect(abridor.abiertos, isEmpty);
  });

  testWidgets('ERR_PAG_13 muestra el mensaje del backend', (tester) async {
    servicio.error = ApiException(code: 'ERR_PAG_13', message: 'El comprobante no existe');
    await montar(tester);

    await tocar(tester);

    expect(find.text('El comprobante no existe'), findsOneWidget);
  });

  testWidgets('sin conexión avisa que no hay internet', (tester) async {
    servicio.error = DioException(requestOptions: RequestOptions(path: '/x'), type: DioExceptionType.connectionError);
    await montar(tester);

    await tocar(tester);

    expect(find.text('No hay conexión. Revisa tu internet e inténtalo de nuevo.'), findsOneWidget);
  });

  testWidgets('si no hay app para abrir PDF lo explica', (tester) async {
    abridor.hayVisor = false;
    await montar(tester);

    await tocar(tester);

    expect(find.text('No hay ninguna app para abrir PDF en tu teléfono.'), findsOneWidget);
  });

  testWidgets('después de un error se puede volver a intentar', (tester) async {
    servicio.error = ApiException(code: 'ERR_PAG_14', message: 'x');
    await montar(tester);
    await tocar(tester);

    servicio.error = null;
    await tocar(tester);

    expect(servicio.descargas, ['c1', 'c1']);
    expect(abridor.abiertos, hasLength(1));
  });

  test('el provider no descarga dos veces a la vez', () async {
    final liberar = Completer<void>();
    servicio.antes = () => liberar.future;
    final contenedor = ProviderContainer(overrides: comprobantesFalsos(servicio, abridor));
    addTearDown(contenedor.dispose);
    contenedor.listen(comprobanteDescargaProvider('c1'), (_, _) {});
    final notifier = contenedor.read(comprobanteDescargaProvider('c1').notifier);

    final primera = notifier.descargarYAbrir();
    final segunda = notifier.descargarYAbrir();
    liberar.complete();

    expect(await segunda, isFalse);
    expect(await primera, isTrue);
    expect(servicio.descargas, ['c1']);
  });
}
