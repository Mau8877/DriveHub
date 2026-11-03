import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/asistencia_clase.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/screens/agenda_instructor_screen.dart';

import '../../helpers/clases_falsos.dart';

/// El recorrido completo por la pantalla de la agenda: de la clase por cerrar a la clase cerrada y editable.
void main() {
  late ClasesServiceFalso servicio;

  setUp(() => servicio = ClasesServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(theme: AppTheme.light, home: const Scaffold(body: AgendaInstructorScreen())),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('cada clase ofrece solo la acción que le corresponde', (tester) async {
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'a', cancelable: true),
      claseDeAgendaDePrueba(codigo: 'b', cerrable: true),
      claseDeAgendaDePrueba(codigo: 'c', estado: EstadoReserva.completada, claseCodigo: 'c1'),
      claseDeAgendaDePrueba(codigo: 'd', estado: EstadoReserva.noAsistio, claseCodigo: 'c2'),
      claseDeAgendaDePrueba(codigo: 'e', estado: EstadoReserva.completada),
      claseDeAgendaDePrueba(codigo: 'f', estado: EstadoReserva.cancelada),
      claseDeAgendaDePrueba(codigo: 'g', terminada: true),
    ];
    await montar(tester);

    expect(find.byKey(const Key('cancelar-a')), findsOneWidget);
    expect(find.byKey(const Key('cerrar-b')), findsOneWidget);
    expect(find.byKey(const Key('editar-c')), findsOneWidget);
    expect(find.byKey(const Key('editar-d')), findsOneWidget);
    for (final codigo in ['e', 'f', 'g']) {
      for (final accion in ['cancelar', 'cerrar', 'editar']) {
        expect(find.byKey(Key('$accion-$codigo')), findsNothing);
      }
    }
    expect(find.byKey(const Key('cerrar-a')), findsNothing);
    expect(find.byKey(const Key('cancelar-b')), findsNothing);
  });

  testWidgets('tocar «Cerrar clase» abre la hoja de cerrar de esa clase', (tester) async {
    servicio.clasesDeAgenda = [claseDeAgendaDePrueba(codigo: 'b', cerrable: true, estudianteNombre: 'Ana Alfa')];
    await montar(tester);

    await tester.tap(find.byKey(const Key('cerrar-b')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('asistencia')), findsOneWidget);
    expect(find.byKey(const Key('claseACerrar')), findsOneWidget);
    expect(find.text('Ana Alfa'), findsWidgets);
    expect(servicio.cierresPedidos, isEmpty);
  });

  testWidgets('tocar «Editar» abre la hoja de editar con los datos guardados', (tester) async {
    servicio.clasesDeAgenda = [claseDeAgendaDePrueba(codigo: 'c', estado: EstadoReserva.completada, claseCodigo: 'c9')];
    servicio.claseGuardada = claseDictadaDePrueba(codigo: 'c9', tema: 'Giros', kmInicio: 100);
    await montar(tester);

    await tester.tap(find.byKey(const Key('editar-c')));
    await tester.pumpAndSettle();

    expect(find.text('Editar clase'), findsOneWidget);
    expect(servicio.clasesDictadasPedidas, ['c9']);
    expect(tester.widget<TextFormField>(find.byKey(const Key('campoTema'))).controller?.text, 'Giros');
    expect(tester.widget<TextFormField>(find.byKey(const Key('campoKmInicio'))).controller?.text, '100');
  });

  testWidgets('cerrar una clase la deja «Completada» y cambia «Cerrar clase» por «Editar»', (tester) async {
    servicio.clasesDeAgenda = [claseDeAgendaDePrueba(codigo: 'b', cerrable: true)];
    await montar(tester);
    expect(find.text('Confirmada'), findsOneWidget);

    await tester.tap(find.byKey(const Key('cerrar-b')));
    await tester.pumpAndSettle();
    await tester.tap(
      find.descendant(of: find.byKey(const Key('asistencia')), matching: find.text('Presente')),
    );
    await tester.pumpAndSettle();
    // * el servidor, tras cerrar, devuelve la clase ya completada y con su código
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'b', estado: EstadoReserva.completada, claseCodigo: 'c1'),
    ];
    await tester.tap(find.byKey(const Key('confirmarCierre')));
    await tester.pumpAndSettle();

    expect(servicio.cierresPedidos.single.asistencia, AsistenciaClase.presente);
    expect(find.text('Clase cerrada. Quedó completada.'), findsOneWidget);
    expect(find.text('Completada'), findsOneWidget);
    expect(find.text('Confirmada'), findsNothing);
    expect(find.byKey(const Key('cerrar-b')), findsNothing);
    expect(find.byKey(const Key('editar-b')), findsOneWidget);
  });

  testWidgets('cerrar con «Ausente» la deja «No asistió»', (tester) async {
    servicio.clasesDeAgenda = [claseDeAgendaDePrueba(codigo: 'b', cerrable: true)];
    await montar(tester);

    await tester.tap(find.byKey(const Key('cerrar-b')));
    await tester.pumpAndSettle();
    await tester.tap(
      find.descendant(of: find.byKey(const Key('asistencia')), matching: find.text('Ausente')),
    );
    await tester.pumpAndSettle();
    servicio.clasesDeAgenda = [
      claseDeAgendaDePrueba(codigo: 'b', estado: EstadoReserva.noAsistio, claseCodigo: 'c1'),
    ];
    await tester.tap(find.byKey(const Key('confirmarCierre')));
    await tester.pumpAndSettle();

    expect(find.text('Clase cerrada. Quedó registrada la inasistencia.'), findsOneWidget);
    expect(find.text('No asistió'), findsOneWidget);
    expect(find.byKey(const Key('editar-b')), findsOneWidget);
  });

  testWidgets('si el servidor dice que ya no se puede cerrar, la agenda se refresca y deja de ofrecerlo', (
    tester,
  ) async {
    servicio.clasesDeAgenda = [claseDeAgendaDePrueba(codigo: 'b', cerrable: true)];
    await montar(tester);
    await tester.tap(find.byKey(const Key('cerrar-b')));
    await tester.pumpAndSettle();
    await tester.tap(
      find.descendant(of: find.byKey(const Key('asistencia')), matching: find.text('Tarde')),
    );
    await tester.pumpAndSettle();
    servicio.errorAlCerrarClase = ApiException(code: 'ERR_ACA_37', message: 'Esa clase no se puede cerrar');
    // * mientras tanto el estudiante canceló: la agenda ya la muestra cancelada
    servicio.clasesDeAgenda = [claseDeAgendaDePrueba(codigo: 'b', estado: EstadoReserva.cancelada)];

    await tester.tap(find.byKey(const Key('confirmarCierre')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('confirmarCierre')), findsNothing);
    expect(find.text('Esa clase no se puede cerrar'), findsOneWidget);
    expect(find.text('Cancelada'), findsOneWidget);
    expect(find.byKey(const Key('cerrar-b')), findsNothing);
  });
}
