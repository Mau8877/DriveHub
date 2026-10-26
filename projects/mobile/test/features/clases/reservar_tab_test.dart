import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/providers/franjas_disponibles_provider.dart';
import 'package:mobile/features/clases/providers/franjas_seleccionadas_provider.dart';
import 'package:mobile/features/horarios/models/fechas.dart';

import '../../helpers/clases_app.dart';
import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  setUp(() {
    servicio = ClasesServiceFalso();
    pagos = PagosServiceFalso()..saldo = 5;
  });

  /// Monta la sección con dos franjas libres y entra a «Reservar».
  Future<MontajeDeClases> enReservar(WidgetTester tester) async {
    servicio.franjas = [
      franjaDisponibleDePrueba(codigo: 'a', horaInicio: '08:00', horaFin: '09:00'),
      franjaDisponibleDePrueba(codigo: 'b', horaInicio: '09:00', horaFin: '10:00', instructorCodigo: 'i2', instructorNombre: 'Beto Beta'),
      franjaDisponibleDePrueba(codigo: 'c', horaInicio: '15:00', horaFin: '16:00'),
    ];
    servicio.instructores = [instructorDePrueba(codigo: 'i1', nombre: 'Ivan Alfa'), instructorDePrueba(codigo: 'i2', nombre: 'Beto Beta')];
    final montaje = await montarClases(tester, servicio: servicio, pagos: pagos);
    // * Sin clases por venir abre directo en «Reservar»
    return montaje;
  }

  testWidgets('lista las franjas libres con su hora y su instructor', (tester) async {
    await enReservar(tester);

    expect(find.byKey(const Key('franja-a')), findsOneWidget);
    expect(find.text('08:00 – 09:00'), findsOneWidget);
    expect(find.text('Beto Beta'), findsWidgets);
    expect(find.text('15:00 – 16:00'), findsOneWidget);
  });

  testWidgets('elegir un día, un momento y un instructor pide las franjas con esos filtros', (tester) async {
    final montaje = await enReservar(tester);
    final manana = sumarDias(soloFecha(DateTime.now()), 1);

    await tester.tap(find.byKey(Key('dia-${fechaParaApi(manana)}')));
    await tester.pumpAndSettle();
    expect(servicio.pedidosDeFranjas.last.fecha, manana);

    await tester.tap(find.byKey(const Key('momento-tarde')));
    await tester.pumpAndSettle();
    expect(servicio.pedidosDeFranjas.last.momento, MomentoDia.tarde);

    await tester.tap(find.byKey(const Key('instructor-i2')));
    await tester.pumpAndSettle();
    expect(servicio.pedidosDeFranjas.last.instructor, 'i2');
    expect(montaje.container.read(reservarInstructorUiProvider), 'i2');

    await tester.tap(find.byKey(const Key('instructor-todos')));
    await tester.pumpAndSettle();
    expect(servicio.pedidosDeFranjas.last.instructor, isNull);
  });

  testWidgets('tocar una franja la elige, el indicador baja y aparece «Confirmar (N)»', (tester) async {
    await enReservar(tester);
    expect(find.byKey(const Key('barraConfirmar')), findsNothing);

    await tester.tap(find.byKey(const Key('franja-a')));
    await tester.pumpAndSettle();

    expect(find.text('Confirmar (1)'), findsOneWidget);
    expect(find.byKey(const Key('horasVistaPrevia')), findsOneWidget);
    expect(find.text('1 clase elegida'), findsOneWidget);

    await tester.tap(find.byKey(const Key('franja-c')));
    await tester.pumpAndSettle();
    expect(find.text('Confirmar (2)'), findsOneWidget);
    expect(find.text('2 clases elegidas'), findsOneWidget);
  });

  testWidgets('volver a tocar una franja elegida la quita; sin elegidas la barra desaparece', (tester) async {
    await enReservar(tester);
    await tester.tap(find.byKey(const Key('franja-a')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('franja-a')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('barraConfirmar')), findsNothing);
    expect(find.byKey(const Key('horasSaldo')), findsOneWidget);
  });

  testWidgets('«Quitar todas» vacía la selección', (tester) async {
    final montaje = await enReservar(tester);
    await tester.tap(find.byKey(const Key('franja-a')));
    await tester.tap(find.byKey(const Key('franja-c')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('quitarTodas')));
    await tester.pumpAndSettle();

    expect(montaje.container.read(franjasSeleccionadasUiProvider), isEmpty);
    expect(find.byKey(const Key('barraConfirmar')), findsNothing);
  });

  testWidgets('no deja elegir más franjas que horas y explica por qué', (tester) async {
    pagos.saldo = 2;
    final montaje = await enReservar(tester);

    await tester.tap(find.byKey(const Key('franja-a')));
    await tester.tap(find.byKey(const Key('franja-b')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('franja-c')));
    await tester.pump();

    expect(find.text('Ya elegiste todas las clases que te alcanzan.'), findsOneWidget);
    expect(montaje.container.read(franjasSeleccionadasUiProvider).keys, ['a', 'b']);
  });

  testWidgets('sin horas no deja elegir ninguna y ofrece comprar', (tester) async {
    pagos.saldo = 0;
    final montaje = await enReservar(tester);

    await tester.tap(find.byKey(const Key('franja-a')));
    await tester.pump();

    expect(find.text('No tienes horas: compra un plan para poder reservar.'), findsOneWidget);
    expect(montaje.container.read(franjasSeleccionadasUiProvider), isEmpty);
    expect(find.text('Comprar horas'), findsOneWidget);
  });

  testWidgets('las elegidas se conservan al cambiar de filtro, aunque ya no estén en la lista', (tester) async {
    final montaje = await enReservar(tester);
    await tester.tap(find.byKey(const Key('franja-a')));
    await tester.pumpAndSettle();

    servicio.franjas = [franjaDisponibleDePrueba(codigo: 'z', horaInicio: '19:00', horaFin: '20:00')];
    await tester.tap(find.byKey(const Key('momento-noche')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('franja-a')), findsNothing);
    expect(find.byKey(const Key('franja-z')), findsOneWidget);
    expect(find.text('Confirmar (1)'), findsOneWidget);
    expect(montaje.container.read(franjasSeleccionadasUiProvider).keys, ['a']);
  });

  testWidgets('sin franjas con esos filtros lo dice y «Quitar filtros» los limpia', (tester) async {
    final montaje = await enReservar(tester);
    servicio.franjas = [];
    await tester.tap(find.byKey(const Key('momento-noche')));
    await tester.pumpAndSettle();

    expect(find.text('No hay horarios libres con estos filtros'), findsOneWidget);

    servicio.franjas = [franjaDisponibleDePrueba()];
    await tester.tap(find.byKey(const Key('quitarFiltros')));
    await tester.pumpAndSettle();

    expect(montaje.container.read(reservarMomentoUiProvider), MomentoDia.todos);
    expect(find.byKey(const Key('franja-f1')), findsOneWidget);
  });

  testWidgets('un día sin franjas y sin filtros no ofrece quitar filtros', (tester) async {
    await enReservar(tester);
    servicio.franjas = [];
    final manana = sumarDias(soloFecha(DateTime.now()), 1);
    await tester.tap(find.byKey(Key('dia-${fechaParaApi(manana)}')));
    await tester.pumpAndSettle();

    expect(find.text('No hay horarios libres con estos filtros'), findsOneWidget);
    expect(find.byKey(const Key('quitarFiltros')), findsNothing);
  });

  testWidgets('un error de red muestra el aviso y «Reintentar» vuelve a pedir', (tester) async {
    servicio.errorFranjas = Exception('sin red');
    await enReservar(tester);

    expect(find.byKey(const Key('errorFranjas')), findsOneWidget);
    expect(find.textContaining('Revisa tu conexión'), findsOneWidget);

    servicio.errorFranjas = null;
    servicio.franjas = [franjaDisponibleDePrueba(codigo: 'a')];
    await tester.tap(find.text('Reintentar'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('franja-a')), findsOneWidget);
  });

  testWidgets('un error del backend (Identidad caída) muestra su mensaje', (tester) async {
    servicio.errorFranjas = ApiException(code: 'ERR_ACA_17', message: 'No pudimos obtener los datos de los instructores.');
    await enReservar(tester);

    expect(find.text('No pudimos obtener los datos de los instructores.'), findsOneWidget);
  });

  testWidgets('si no cargan los instructores, igual se ven las franjas', (tester) async {
    servicio.errorInstructores = Exception('sin red');
    servicio.franjas = [franjaDisponibleDePrueba(codigo: 'a')];
    await montarClases(tester, servicio: servicio, pagos: pagos);

    expect(find.byKey(const Key('franja-a')), findsOneWidget);
    expect(find.byKey(const Key('instructores')), findsNothing);
  });

  testWidgets('una franja del día de hoy que ya empezó no se muestra', (tester) async {
    servicio.franjas = [
      franjaDisponibleDePrueba(codigo: 'vieja', fecha: DateTime(2020, 1, 1)),
      franjaDisponibleDePrueba(codigo: 'futura'),
    ];
    await montarClases(tester, servicio: servicio, pagos: pagos);

    expect(find.byKey(const Key('franja-vieja')), findsNothing);
    expect(find.byKey(const Key('franja-futura')), findsOneWidget);
  });
}
