import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/providers/franja_para_reprogramar_provider.dart';

import '../../helpers/clases_app.dart';
import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  final hoy = DateTime.now();
  final enCinco = DateTime(hoy.year, hoy.month, hoy.day + 5);

  setUp(() {
    servicio = ClasesServiceFalso()
      ..proximas = [reservaDePrueba(codigo: 'a', fecha: enCinco, cancelacionATiempo: true)]
      ..franjas = [
        franjaDisponibleDePrueba(codigo: 'n1', horaInicio: '15:00', horaFin: '16:00'),
        franjaDisponibleDePrueba(codigo: 'n2', horaInicio: '16:00', horaFin: '17:00', instructorNombre: 'Beto Beta'),
      ];
    pagos = PagosServiceFalso()..saldo = 5;
  });

  Future<MontajeDeClases> abrirLaHoja(WidgetTester tester) async {
    final montaje = await montarClases(tester, servicio: servicio, pagos: pagos);
    await tester.tap(find.byKey(const Key('reprogramar-a')));
    await tester.pumpAndSettle();
    return montaje;
  }

  bool confirmarHabilitado(WidgetTester tester) =>
      tester.widget<FilledButton>(find.byKey(const Key('confirmarReprogramacion'))).onPressed != null;

  testWidgets('la hoja muestra la clase actual y las franjas libres, con «Reprogramar» apagado', (tester) async {
    await abrirLaHoja(tester);

    expect(find.text('Reprogramar clase'), findsOneWidget);
    expect(find.byKey(const Key('claseActual')), findsOneWidget);
    expect(find.byKey(const Key('franjaDestino-n1')), findsOneWidget);
    expect(find.byKey(const Key('franjaDestino-n2')), findsOneWidget);
    expect(confirmarHabilitado(tester), isFalse);
    expect(servicio.reprogramacionesPedidas, isEmpty);
  });

  testWidgets('elegir una franja habilita «Reprogramar», y volver a tocarla lo apaga', (tester) async {
    final montaje = await abrirLaHoja(tester);

    await tester.tap(find.byKey(const Key('franjaDestino-n1')));
    await tester.pumpAndSettle();
    expect(confirmarHabilitado(tester), isTrue);
    expect(montaje.container.read(franjaParaReprogramarUiProvider)?.codigo, 'n1');

    await tester.tap(find.byKey(const Key('franjaDestino-n1')));
    await tester.pumpAndSettle();
    expect(confirmarHabilitado(tester), isFalse);
  });

  testWidgets('se elige una sola franja: elegir otra reemplaza a la anterior', (tester) async {
    final montaje = await abrirLaHoja(tester);

    await tester.tap(find.byKey(const Key('franjaDestino-n1')));
    await tester.tap(find.byKey(const Key('franjaDestino-n2')));
    await tester.pumpAndSettle();

    expect(montaje.container.read(franjaParaReprogramarUiProvider)?.codigo, 'n2');
  });

  testWidgets('confirmar mueve la clase, cierra la hoja, avisa y deja la selección vacía', (tester) async {
    final montaje = await abrirLaHoja(tester);
    await tester.tap(find.byKey(const Key('franjaDestino-n2')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('confirmarReprogramacion')));
    await tester.pumpAndSettle();

    expect(servicio.reprogramacionesPedidas.single, (reserva: 'a', franja: 'n2'));
    expect(find.text('Reprogramar clase'), findsNothing);
    expect(find.textContaining('Clase reprogramada para'), findsOneWidget);
    expect(montaje.container.read(franjaParaReprogramarUiProvider), isNull);
  });

  testWidgets('«Volver» cierra la hoja sin mover nada y sin dejar una franja elegida', (tester) async {
    final montaje = await abrirLaHoja(tester);
    await tester.tap(find.byKey(const Key('franjaDestino-n1')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('volverDeReprogramar')));
    await tester.pumpAndSettle();

    expect(find.text('Reprogramar clase'), findsNothing);
    expect(servicio.reprogramacionesPedidas, isEmpty);
    expect(montaje.container.read(franjaParaReprogramarUiProvider), isNull);
  });

  testWidgets('sin franjas libres muestra el aviso vacío y no deja confirmar', (tester) async {
    servicio.franjas = [];
    await abrirLaHoja(tester);

    expect(find.text('No hay horarios libres con estos filtros'), findsOneWidget);
    expect(confirmarHabilitado(tester), isFalse);
  });

  testWidgets('si falla la lista de franjas muestra el error con «Reintentar»', (tester) async {
    servicio.errorFranjas = ApiException(code: 'ERR_ACA_17', message: 'No pudimos obtener los datos');
    await abrirLaHoja(tester);

    expect(find.byKey(const Key('errorFranjasReprogramar')), findsOneWidget);
    expect(find.text('Reintentar'), findsOneWidget);
  });

  testWidgets('un doble toque en «Reprogramar» manda una sola petición', (tester) async {
    final espera = Completer<void>();
    servicio.antesDeReprogramar = () => espera.future;
    await abrirLaHoja(tester);
    await tester.tap(find.byKey(const Key('franjaDestino-n1')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('confirmarReprogramacion')));
    await tester.pump();
    await tester.tap(find.byKey(const Key('confirmarReprogramacion')), warnIfMissed: false);
    await tester.pump();

    expect(servicio.reprogramacionesPedidas, hasLength(1));
    expect(find.byType(CircularProgressIndicator), findsWidgets);

    espera.complete();
    await tester.pumpAndSettle();
    expect(servicio.reprogramacionesPedidas, hasLength(1));
  });

  group('errores', () {
    testWidgets('si otro estudiante tomó la franja (ERR_ACA_18) la hoja sigue abierta, vacía la elección y refresca', (
      tester,
    ) async {
      servicio.errorAlReprogramar = ApiException(code: 'ERR_ACA_18', message: 'La franja ya no está disponible');
      final montaje = await abrirLaHoja(tester);
      await tester.tap(find.byKey(const Key('franjaDestino-n1')));
      await tester.pumpAndSettle();
      final pedidosAntes = servicio.pedidosDeFranjas.length;

      await tester.tap(find.byKey(const Key('confirmarReprogramacion')));
      await tester.pumpAndSettle();

      expect(find.text('Reprogramar clase'), findsOneWidget);
      expect(find.text('Otro estudiante ya reservó esa franja. Elige otra.'), findsOneWidget);
      expect(montaje.container.read(franjaParaReprogramarUiProvider), isNull);
      expect(servicio.pedidosDeFranjas.length, greaterThan(pedidosAntes));
    });

    testWidgets('fuera de plazo (ERR_ACA_34) cierra la hoja con el mensaje del servidor', (tester) async {
      servicio.errorAlReprogramar = ApiException(code: 'ERR_ACA_34', message: 'Solo puedes reprogramar con 12 horas');
      await abrirLaHoja(tester);
      await tester.tap(find.byKey(const Key('franjaDestino-n1')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('confirmarReprogramacion')));
      await tester.pumpAndSettle();

      expect(find.text('Reprogramar clase'), findsNothing);
      expect(find.text('Solo puedes reprogramar con 12 horas'), findsOneWidget);
    });

    testWidgets('una franja que ya empezó o con otra clase a esa hora (ERR_ACA_20) se explica y la hoja sigue', (
      tester,
    ) async {
      servicio.errorAlReprogramar = ApiException(code: 'ERR_ACA_20', message: 'Ya tienes una clase reservada a esa hora');
      await abrirLaHoja(tester);
      await tester.tap(find.byKey(const Key('franjaDestino-n1')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('confirmarReprogramacion')));
      await tester.pumpAndSettle();

      expect(find.text('Reprogramar clase'), findsOneWidget);
      expect(find.text('Ya tienes una clase reservada a esa hora'), findsOneWidget);
    });

    testWidgets('sin conexión conserva la elección para reintentar y no da nada por hecho', (tester) async {
      servicio.errorAlReprogramar = DioException(requestOptions: RequestOptions(path: '/reservas/a/reprogramar'));
      final montaje = await abrirLaHoja(tester);
      await tester.tap(find.byKey(const Key('franjaDestino-n1')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('confirmarReprogramacion')));
      await tester.pumpAndSettle();

      expect(find.text('Reprogramar clase'), findsOneWidget);
      expect(find.byKey(const Key('errorReprogramar')), findsOneWidget);
      expect(montaje.container.read(franjaParaReprogramarUiProvider)?.codigo, 'n1');
      expect(confirmarHabilitado(tester), isTrue);
    });
  });
}
