import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/shared/widgets/selector_ubicacion.dart';

import '../../helpers/teselas_falsas.dart';

Widget _montar({
  double? latitud,
  double? longitud,
  String? direccion,
  bool habilitado = true,
  required AlCambiarUbicacion onCambio,
  ObtenerUbicacionActual? ubicacionActual,
}) {
  return MaterialApp(
    theme: AppTheme.light,
    home: Scaffold(
      body: SingleChildScrollView(
        child: SelectorUbicacion(
          latitud: latitud,
          longitud: longitud,
          direccion: direccion,
          habilitado: habilitado,
          onCambio: onCambio,
          obtenerUbicacionActual: ubicacionActual,
          tileProvider: TeselasFalsas(),
        ),
      ),
    ),
  );
}

void main() {
  testWidgets('sin punto muestra el mapa, la atribución de OpenStreetMap y ningún marcador', (tester) async {
    await tester.pumpWidget(_montar(onCambio: (_, _) {}));

    expect(find.byKey(const Key('mapaUbicacion')), findsOneWidget);
    expect(find.text('© OpenStreetMap'), findsOneWidget);
    expect(find.byKey(const Key('marcadorUbicacion')), findsNothing);
    expect(find.text('Toca el mapa para marcar tu ubicación.'), findsOneWidget);
  });

  testWidgets('con punto muestra el marcador y la ayuda para cambiarlo', (tester) async {
    await tester.pumpWidget(_montar(latitud: -17.7833, longitud: -63.1821, onCambio: (_, _) {}));

    expect(find.byKey(const Key('marcadorUbicacion')), findsOneWidget);
    expect(find.text('Toca otro lugar para cambiarla.'), findsOneWidget);
  });

  testWidgets('un toque en el mapa llama a onCambio con 6 decimales', (tester) async {
    final puntos = <(double, double)>[];
    await tester.pumpWidget(_montar(onCambio: (lat, lon) => puntos.add((lat, lon))));

    await tester.tap(find.byKey(const Key('mapaUbicacion')));
    // * flutter_map espera un instante por si el toque es el primero de un doble toque
    await tester.pump(const Duration(milliseconds: 500));

    expect(puntos, hasLength(1));
    // * Tocar el centro devuelve (casi) el centro inicial: La Paz
    expect(puntos.single.$1, closeTo(-16.4897, 0.01));
    expect(puntos.single.$2, closeTo(-68.1193, 0.01));
    expect((puntos.single.$1 * 1e6).roundToDouble() / 1e6, puntos.single.$1);
  });

  testWidgets('deshabilitado no llama a onCambio ni deja usar "Mi ubicación"', (tester) async {
    final puntos = <(double, double)>[];
    await tester.pumpWidget(_montar(habilitado: false, onCambio: (lat, lon) => puntos.add((lat, lon))));

    await tester.tap(find.byKey(const Key('mapaUbicacion')));
    await tester.pump(const Duration(milliseconds: 500));

    expect(puntos, isEmpty);
    expect(tester.widget<OutlinedButton>(find.byKey(const Key('botonMiUbicacion'))).onPressed, isNull);
  });

  testWidgets('muestra la dirección guardada cuando existe', (tester) async {
    await tester.pumpWidget(_montar(direccion: 'Av. Arce 2345, La Paz', onCambio: (_, _) {}));

    expect(find.byKey(const Key('direccionUbicacion')), findsOneWidget);
    expect(find.text('Av. Arce 2345, La Paz'), findsOneWidget);
  });

  testWidgets('sin dirección no muestra el bloque de dirección', (tester) async {
    await tester.pumpWidget(_montar(onCambio: (_, _) {}));

    expect(find.byKey(const Key('direccionUbicacion')), findsNothing);
  });

  testWidgets('"Mi ubicación" usa la ubicación del dispositivo', (tester) async {
    final puntos = <(double, double)>[];
    await tester.pumpWidget(_montar(
      onCambio: (lat, lon) => puntos.add((lat, lon)),
      ubicacionActual: () async => const LatLng(-16.5001234567, -68.1502),
    ));

    await tester.tap(find.byKey(const Key('botonMiUbicacion')));
    await tester.pump();
    await tester.pump();

    expect(puntos, [(-16.500123, -68.1502)]);
  });

  testWidgets('"Mi ubicación" sin permiso avisa y no llama a onCambio', (tester) async {
    final puntos = <(double, double)>[];
    await tester.pumpWidget(_montar(
      onCambio: (lat, lon) => puntos.add((lat, lon)),
      ubicacionActual: () async => null,
    ));

    await tester.tap(find.byKey(const Key('botonMiUbicacion')));
    await tester.pump();
    await tester.pump();

    expect(puntos, isEmpty);
    expect(find.text('No pudimos obtener tu ubicación. Toca el mapa para elegirla.'), findsOneWidget);
  });

  testWidgets('"Mi ubicación" que lanza un error también avisa sin romper', (tester) async {
    await tester.pumpWidget(_montar(
      onCambio: (_, _) {},
      ubicacionActual: () async => throw StateError('GPS apagado'),
    ));

    await tester.tap(find.byKey(const Key('botonMiUbicacion')));
    await tester.pump();
    await tester.pump();

    expect(find.text('No pudimos obtener tu ubicación. Toca el mapa para elegirla.'), findsOneWidget);
  });
}
