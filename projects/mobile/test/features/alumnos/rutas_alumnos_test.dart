import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';

import '../../helpers/alumnos_falsos.dart';
import '../../helpers/horarios_falsos.dart';
import '../../helpers/pagos_falsos.dart';
import '../../helpers/sesion_falsa.dart';

void main() {
  const instructor = UsuarioSesion(codigo: 'i1', nombres: 'Luis', apellidos: 'Rojas', roles: ['INSTRUCTOR']);
  const administrador = UsuarioSesion(codigo: 'a1', nombres: 'Eva', apellidos: 'Soto', roles: ['ADMIN_ESCUELA']);

  late HorasAlumnosServiceFalso servicio;

  setUp(() {
    servicio = HorasAlumnosServiceFalso()..alumnos = [alumnoDePrueba(codigo: 'a1')];
  });

  Future<ProviderContainer> montar(WidgetTester tester, EstadoSesion estado) async {
    final container = ProviderContainer(
      overrides: [
        sesionFalsa(SesionFalsa(estado)),
        horasAlumnosServiceFalso(servicio),
        horariosServiceFalso(HorariosServiceFalso()),
        pagosServiceFalso(PagosServiceFalso()),
      ],
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(routerConfig: container.read(routerProvider)),
      ),
    );
    await tester.pumpAndSettle();
    return container;
  }

  String ubicacion(ProviderContainer c) => c.read(routerProvider).state.uri.path;

  testWidgets('el instructor entra a /students/:codigo y ve el detalle', (tester) async {
    final container = await montar(tester, const EstadoSesion.conSesion(instructor));

    container.read(routerProvider).go('/students/a1');
    await tester.pumpAndSettle();

    expect(ubicacion(container), '/students/a1');
    expect(find.text('Tiempo conmigo'), findsOneWidget);
    expect(servicio.pedidosDeDetalle, ['a1']);
  });

  testWidgets('desde «Alumnos» del instructor se llega al detalle de un alumno', (tester) async {
    final container = await montar(tester, const EstadoSesion.conSesion(instructor));

    await tester.tap(find.descendant(of: find.byType(NavigationBar), matching: find.text('Alumnos')));
    await tester.pumpAndSettle();
    expect(find.text('Ana Pérez'), findsOneWidget);

    await tester.tap(find.byKey(const Key('alumno-a1')));
    await tester.pumpAndSettle();

    expect(ubicacion(container), '/students/a1');
  });

  testWidgets('un estudiante no entra al detalle: vuelve al inicio', (tester) async {
    final container = await montar(tester, const EstadoSesion.conSesion(usuarioDePrueba));

    container.read(routerProvider).go('/students/a1');
    await tester.pumpAndSettle();

    expect(ubicacion(container), '/inicio');
    expect(servicio.pedidosDeDetalle, isEmpty);
  });

  testWidgets('un administrador no entra al detalle: vuelve al inicio', (tester) async {
    final container = await montar(tester, const EstadoSesion.conSesion(administrador));

    container.read(routerProvider).go('/students/a1');
    await tester.pumpAndSettle();

    expect(ubicacion(container), '/inicio');
  });

  testWidgets('sin sesión la ruta termina en el login', (tester) async {
    final container = await montar(tester, const EstadoSesion.sinSesion());

    container.read(routerProvider).go('/students/a1');
    await tester.pumpAndSettle();

    expect(ubicacion(container), '/login');
  });
}
