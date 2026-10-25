import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/alumnos/providers/alumno_detalle_provider.dart';
import 'package:mobile/features/alumnos/providers/alumnos_busqueda_provider.dart';
import 'package:mobile/features/alumnos/providers/mis_alumnos_provider.dart';

import '../../helpers/alumnos_falsos.dart';

void main() {
  late HorasAlumnosServiceFalso servicio;
  late ProviderContainer container;

  setUp(() {
    servicio = HorasAlumnosServiceFalso();
    container = ProviderContainer(overrides: [horasAlumnosServiceFalso(servicio)]);
    addTearDown(container.dispose);
  });

  void hayAlumnos(int cantidad) {
    servicio.alumnos = [
      for (var i = 0; i < cantidad; i++) alumnoDePrueba(codigo: 'a$i', nombres: 'Alumno$i', apellidos: 'Apellido'),
    ];
  }

  // * Se mantiene viva con un listener: es autoDispose
  Future<AlumnosListado> cargar() {
    container.listen(misAlumnosProvider, (_, _) {});
    return container.read(misAlumnosProvider.future);
  }

  AlumnosListado actual() => container.read(misAlumnosProvider).requireValue;

  test('carga la primera página de 20, sin esperar la pausa de la búsqueda', () async {
    hayAlumnos(45);

    final listado = await cargar();

    expect(listado.items, hasLength(20));
    expect(listado.hayMas, isTrue);
    expect(servicio.listados, [(search: '', page: 1)]);
  });

  test('cargarMas agrega la siguiente página sin duplicar, y no pasa de la última', () async {
    hayAlumnos(25);
    await cargar();
    final notifier = container.read(misAlumnosProvider.notifier);

    await notifier.cargarMas();
    await notifier.cargarMas();

    expect(actual().items, hasLength(25));
    expect(actual().items.map((a) => a.estudianteCodigo).toSet(), hasLength(25));
    expect(servicio.listados.map((l) => l.page), [1, 2]);
  });

  test('no pide la misma página dos veces a la vez', () async {
    hayAlumnos(45);
    await cargar();
    final notifier = container.read(misAlumnosProvider.notifier);

    await Future.wait([notifier.cargarMas(), notifier.cargarMas()]);

    expect(servicio.listados.map((l) => l.page), [1, 2]);
  });

  testWidgets('la búsqueda espera 400 ms desde la última tecla y pide una sola vez', (tester) async {
    servicio.alumnos = [alumnoDePrueba(codigo: 'a1', nombres: 'Ana'), alumnoDePrueba(codigo: 'a2', nombres: 'Beto')];
    container.listen(misAlumnosProvider, (_, _) {});
    await tester.pump();
    servicio.listados.clear();

    final busqueda = container.read(alumnosBusquedaUiProvider.notifier);
    busqueda.escribir('a');
    await tester.pump(const Duration(milliseconds: 200));
    busqueda.escribir('an');
    await tester.pump(const Duration(milliseconds: 200));
    busqueda.escribir('ana');
    await tester.pump(const Duration(milliseconds: 399));
    expect(servicio.listados, isEmpty);

    await tester.pump(const Duration(milliseconds: 2));
    await tester.pump();

    expect(servicio.listados, [(search: 'ana', page: 1)]);
    expect(container.read(misAlumnosProvider).requireValue.items.map((a) => a.estudianteCodigo), ['a1']);
  });

  testWidgets('borrar la búsqueda pide enseguida, sin esperar', (tester) async {
    servicio.alumnos = [alumnoDePrueba()];
    container.listen(misAlumnosProvider, (_, _) {});
    container.read(alumnosBusquedaUiProvider.notifier).escribir('ana');
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pump();
    servicio.listados.clear();

    container.read(alumnosBusquedaUiProvider.notifier).escribir('');
    // * Apenas el tiempo de que Riverpod reprograme la reconstrucción: muy lejos de los 400 ms de la pausa
    await tester.pump(const Duration(milliseconds: 1));
    await tester.pump();

    expect(servicio.listados, [(search: '', page: 1)]);
  });

  test('refrescar vuelve a pedir la primera página', () async {
    hayAlumnos(3);
    await cargar();
    hayAlumnos(5);

    await container.read(misAlumnosProvider.notifier).refrescar();

    expect(actual().items, hasLength(5));
  });

  test('un error en cargarMas conserva lo ya cargado y permite reintentar', () async {
    hayAlumnos(45);
    servicio.errorListado = Exception('sin red');
    servicio.errorListadoSoloDesdeLaPagina2 = true;
    await cargar();
    final notifier = container.read(misAlumnosProvider.notifier);

    await notifier.cargarMas();

    expect(actual().items, hasLength(20));
    expect(actual().errorAlCargarMas, isNotNull);

    servicio.errorListado = null;
    await notifier.cargarMas();

    expect(actual().items, hasLength(40));
    expect(actual().errorAlCargarMas, isNull);
  });

  test('un error al cargar la primera página deja el listado en error', () async {
    servicio.errorListado = Exception('sin red');
    container.listen(misAlumnosProvider, (_, _) {});

    await expectLater(container.read(misAlumnosProvider.future), throwsException);
    expect(container.read(misAlumnosProvider).hasError, isTrue);
  });

  test('el detalle pide al alumno por su código', () async {
    servicio.detalles = {'a9': detalleDeAlumno(codigo: 'a9')};
    container.listen(alumnoDetalleProvider('a9'), (_, _) {});

    final detalle = await container.read(alumnoDetalleProvider('a9').future);

    expect(detalle.resumen.estudianteCodigo, 'a9');
    expect(servicio.pedidosDeDetalle, ['a9']);
  });
}
