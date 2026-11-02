import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/tipo_prueba.dart';
import 'package:mobile/features/clases/widgets/pruebas_de_clase_sheet.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  final conPruebas = claseDeAgendaDePrueba(
    codigo: 'r1',
    estado: EstadoReserva.completada,
    claseCodigo: 'c1',
    conPrueba: true,
    estudianteNombre: 'Eva Estudiante',
  );

  setUp(() => servicio = ClasesServiceFalso());

  Future<void> abrir(WidgetTester tester) async {
    tester.view.physicalSize = const Size(800, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          theme: AppTheme.light,
          home: Scaffold(
            body: Builder(
              builder: (context) => TextButton(
                key: const Key('abrirPruebas'),
                onPressed: () => mostrarPruebasDeClase(context, conPruebas),
                child: const Text('abrir pruebas'),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.byKey(const Key('abrirPruebas')));
    await tester.pumpAndSettle();
  }

  testWidgets('muestra el tipo, el resultado, el total y el puntaje de cada criterio', (tester) async {
    servicio.pruebasPorClase['c1'] = [pruebaClaseDePrueba(tipo: TipoPrueba.manejo, puntaje: 8)];
    await abrir(tester);

    expect(find.text('Pruebas de la clase'), findsOneWidget);
    expect(find.byKey(const Key('claseConPruebas')), findsOneWidget);
    expect(find.text('Eva Estudiante'), findsOneWidget);
    expect(servicio.pruebasPedidas, ['c1']);
    final tarjeta = find.byKey(const Key('prueba-MANEJO'));
    expect(find.descendant(of: tarjeta, matching: find.text('Manejo')), findsOneWidget);
    expect(find.descendant(of: tarjeta, matching: find.text('Aprobado')), findsOneWidget);
    expect(find.byKey(const Key('total-MANEJO')), findsOneWidget);
    expect(tester.widget<Text>(find.byKey(const Key('total-MANEJO'))).data, 'Total: 80 / 100');
    expect(find.text('Criterio A de Manejo'), findsOneWidget);
    expect(find.text('Criterio B de Manejo'), findsOneWidget);
    expect(find.text('8 / 10'), findsNWidgets(2));
  });

  testWidgets('con tres pruebas muestra tres tarjetas, con aprobada y reprobada', (tester) async {
    servicio.pruebasPorClase['c1'] = [
      pruebaClaseDePrueba(codigo: 'p1', tipo: TipoPrueba.manejo, puntaje: 9),
      pruebaClaseDePrueba(codigo: 'p2', tipo: TipoPrueba.estacionamiento, puntaje: 5),
      pruebaClaseDePrueba(codigo: 'p3', tipo: TipoPrueba.ciudad, puntaje: 7),
    ];
    await abrir(tester);

    expect(find.byKey(const Key('prueba-MANEJO')), findsOneWidget);
    expect(find.byKey(const Key('prueba-ESTACIONAMIENTO')), findsOneWidget);
    expect(find.byKey(const Key('prueba-CIUDAD')), findsOneWidget);
    expect(find.text('Reprobado'), findsOneWidget);
    expect(find.text('Aprobado'), findsNWidgets(2));
  });

  testWidgets('las observaciones solo se dibujan si existen', (tester) async {
    servicio.pruebasPorClase['c1'] = [
      pruebaClaseDePrueba(codigo: 'p1', tipo: TipoPrueba.manejo, observaciones: 'Mejorar los espejos'),
      pruebaClaseDePrueba(codigo: 'p2', tipo: TipoPrueba.ciudad),
    ];
    await abrir(tester);

    expect(find.byKey(const Key('observaciones-MANEJO')), findsOneWidget);
    expect(find.text('Mejorar los espejos'), findsOneWidget);
    expect(find.byKey(const Key('observaciones-CIUDAD')), findsNothing);
    expect(find.text('Observaciones'), findsOneWidget);
  });

  testWidgets('es de solo lectura: no ofrece editar ni borrar', (tester) async {
    servicio.pruebasPorClase['c1'] = [pruebaClaseDePrueba()];
    await abrir(tester);

    expect(find.text('Editar'), findsNothing);
    expect(find.text('Borrar'), findsNothing);
    expect(find.byType(TextField), findsNothing);
    expect(find.byKey(const Key('cerrarPruebas')), findsOneWidget);
  });

  testWidgets('sin pruebas muestra el estado vacío', (tester) async {
    await abrir(tester);

    expect(find.text('Esta clase todavía no tiene pruebas.'), findsOneWidget);
  });

  testWidgets('si falla muestra el error con «Reintentar», que vuelve a pedir', (tester) async {
    servicio.errorPruebasDeClase = DioException(requestOptions: RequestOptions(path: '/x'));
    await abrir(tester);

    expect(find.byKey(const Key('errorCargarPruebas')), findsOneWidget);

    servicio.errorPruebasDeClase = null;
    servicio.pruebasPorClase['c1'] = [pruebaClaseDePrueba()];
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('errorCargarPruebas')), findsNothing);
    expect(find.byKey(const Key('prueba-MANEJO')), findsOneWidget);
    expect(servicio.pruebasPedidas, ['c1', 'c1']);
  });

  testWidgets('«Cerrar» cierra la hoja', (tester) async {
    await abrir(tester);

    await tester.tap(find.byKey(const Key('cerrarPruebas')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('cerrarPruebas')), findsNothing);
  });
}
