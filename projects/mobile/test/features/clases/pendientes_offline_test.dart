import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/offline/almacen_offline.dart';
import 'package:mobile/core/offline/modelos_offline.dart';
import 'package:mobile/core/offline/nucleo_offline.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/franja_disponible.dart';
import 'package:mobile/features/clases/models/reserva.dart';
import 'package:mobile/features/clases/models/vista_reservas.dart';
import 'package:mobile/features/clases/providers/franjas_disponibles_provider.dart';
import 'package:mobile/features/clases/widgets/clase_agenda_card.dart';
import 'package:mobile/features/clases/widgets/franja_card.dart';
import 'package:mobile/features/clases/widgets/lista_de_agenda.dart';
import 'package:mobile/features/clases/widgets/lista_de_reservas.dart';
import 'package:mobile/features/clases/widgets/pendientes_offline.dart';
import 'package:mobile/features/clases/widgets/reserva_card.dart';
import 'package:mobile/features/offline/providers/offline_provider.dart';
import 'package:mobile/shared/widgets/estado_vacio.dart';

import '../../helpers/clases_falsos.dart';

AccionPendiente _accion(String efecto, List<String> objetivos, {Map<String, dynamic>? datos}) => AccionPendiente(
  id: '$efecto-${objetivos.join()}',
  metodo: 'POST',
  ruta: '/x',
  descripcion: efecto,
  creadaEn: DateTime(2026),
  efecto: efecto,
  objetivos: objetivos,
  datos: datos,
);

void main() {
  group('EstadoOffline: consultas de lo pendiente', () {
    test('pendienteSobre devuelve la más reciente que toca ese código', () {
      final estado = EstadoOffline(
        pendientes: [_accion('reprogramar', ['r1', 'f9']), _accion('cancelar', ['r1'])],
      );

      expect(estado.pendienteSobre('r1')!.efecto, 'cancelar');
      expect(estado.pendienteSobre('f9')!.efecto, 'reprogramar');
      expect(estado.pendienteSobre('otra'), isNull);
    });

    test('pendientesConEfecto filtra por efecto en orden', () {
      final estado = EstadoOffline(
        pendientes: [_accion('reservar', ['a']), _accion('cancelar', ['r1']), _accion('reservar', ['b'])],
      );

      expect(estado.pendientesConEfecto('reservar').map((a) => a.objetivos.single), ['a', 'b']);
    });

    test('la acción pendiente se guarda y se lee con su efecto, objetivos y datos', () {
      final a = _accion('reservar', ['f1'], datos: {'franjas': [{'franjaCodigo': 'f1'}]});

      final leida = AccionPendiente.fromJson(a.toJson());

      expect(leida.efecto, 'reservar');
      expect(leida.objetivos, ['f1']);
      expect(leida.datos, {'franjas': [{'franjaCodigo': 'f1'}]});
    });

    test('una acción guardada por una versión anterior (sin efecto) se sigue leyendo', () {
      final leida = AccionPendiente.fromJson({
        'id': '1',
        'metodo': 'POST',
        'ruta': '/clases',
        'descripcion': 'Cierre',
        'creadaEn': '2026-01-01T00:00:00.000',
      });

      expect(leida.efecto, isNull);
      expect(leida.objetivos, isEmpty);
    });
  });

  group('reservasPendientes', () {
    test('arma una reserva provisional por cada franja reservada sin conexión', () {
      final franja = FranjaDisponible(
        codigo: 'f1',
        fecha: DateTime(2099, 11, 2),
        horaInicio: '09:00',
        horaFin: '10:00',
        instructorCodigo: 'i1',
        instructorNombre: 'Luis Gómez',
      );
      final estado = EstadoOffline(
        pendientes: [_accion('reservar', ['f1'], datos: {'franjas': [datosDeFranja(franja)]})],
      );

      final reservas = reservasPendientes(estado);

      expect(reservas, hasLength(1));
      expect(reservas.single.codigo, 'f1');
      expect(reservas.single.horaInicio, '09:00');
      expect(reservas.single.instructorNombre, 'Luis Gómez');
      expect(reservas.single.fecha, DateTime(2099, 11, 2));
    });

    test('una misma franja no se repite si hay dos acciones sobre ella', () {
      final datos = {
        'franjas': [
          {
            'franjaCodigo': 'f1',
            'fecha': '2099-11-02',
            'horaInicio': '09:00',
            'horaFin': '10:00',
            'instructorCodigo': 'i1',
            'instructorNombre': 'Luis',
          },
        ],
      };
      final estado = EstadoOffline(pendientes: [_accion('reservar', ['f1'], datos: datos), _accion('reservar', ['f1'], datos: datos)]);

      expect(reservasPendientes(estado), hasLength(1));
    });
  });

  group('tarjetas', () {
    Future<void> montar(WidgetTester tester, Widget hijo) => tester.pumpWidget(
      MaterialApp(theme: AppTheme.light, home: Scaffold(body: hijo)),
    );

    testWidgets('ReservaCard muestra la insignia de la operación pendiente en vez del estado', (tester) async {
      await montar(
        tester,
        ReservaCard(reserva: reservaDePrueba(), conEstado: true, etiquetaPendiente: 'Cancelación pendiente'),
      );

      expect(find.text('Cancelación pendiente'), findsOneWidget);
      expect(find.text('Confirmada'), findsNothing);
    });

    testWidgets('ClaseAgendaCard con una operación pendiente apaga cancelar, cerrar y evaluar', (tester) async {
      await montar(
        tester,
        ClaseAgendaCard(
          clase: claseDeAgendaDePrueba(cancelable: true, cerrable: true, claseCodigo: 'c1', evaluable: true),
          alCancelar: () {},
          alCerrar: () {},
          alEditar: () {},
          alEvaluar: () {},
          etiquetaPendiente: 'Cierre pendiente',
        ),
      );

      expect(find.text('Cierre pendiente'), findsOneWidget);
      expect(find.byKey(const Key('cancelar-r1')), findsNothing);
      expect(find.byKey(const Key('cerrar-r1')), findsNothing);
      expect(find.byKey(const Key('editar-r1')), findsNothing);
      expect(find.byKey(const Key('evaluar-r1')), findsNothing);
    });

    testWidgets('ClaseAgendaCard sin operación pendiente conserva sus botones', (tester) async {
      await montar(
        tester,
        ClaseAgendaCard(clase: claseDeAgendaDePrueba(cancelable: true), alCancelar: () {}),
      );

      expect(find.byKey(const Key('cancelar-r1')), findsOneWidget);
    });

    testWidgets('FranjaCard con una reserva pendiente muestra la insignia', (tester) async {
      await montar(
        tester,
        FranjaCard(
          franja: franjaDisponibleDePrueba(),
          elegida: false,
          deshabilitada: false,
          alTocar: () {},
          etiquetaPendiente: 'Reserva pendiente',
        ),
      );

      expect(find.text('Reserva pendiente'), findsOneWidget);
      expect(find.byIcon(Icons.radio_button_unchecked), findsNothing);
    });
  });

  group('listas', () {
    NucleoOffline nucleoCon(List<AccionPendiente> acciones) {
      final nucleo = NucleoOffline(
        almacen: AlmacenEnMemoria(),
        dio: () => Dio(),
        hayToken: () => false,
        sondeo: () async => false,
      );
      return nucleo;
    }

    Future<NucleoOffline> montarLista(
      WidgetTester tester, {
      required List<AccionPendiente> pendientes,
      required Widget lista,
      List<Override> overrides = const [],
    }) async {
      final nucleo = nucleoCon(pendientes);
      await nucleo.abrirSesion('u1');
      for (final a in pendientes) {
        await nucleo.encolar(
          metodo: a.metodo,
          ruta: a.ruta,
          descripcion: a.descripcion,
          efecto: a.efecto,
          objetivos: a.objetivos,
          datos: a.datos,
        );
      }
      await tester.pumpWidget(
        ProviderScope(
          overrides: [nucleoOfflineProvider.overrideWithValue(nucleo), ...overrides],
          child: MaterialApp(
            theme: AppTheme.light,
            home: Scaffold(body: SingleChildScrollView(child: lista)),
          ),
        ),
      );
      await tester.pump();
      return nucleo;
    }

    testWidgets('«Mis clases»: una cancelación pendiente marca la clase y quita sus botones', (tester) async {
      await montarLista(
        tester,
        pendientes: [_accion('cancelar', ['r1'])],
        overrides: [
          reservasListProvider.overrideWith((ref, vista) async => [reservaDePrueba(codigo: 'r1')]),
        ],
        lista: const ListaDeReservas(vista: VistaReservas.proximas, vacio: EstadoVacio(icono: Icons.event, titulo: 'x', texto: 'x')),
      );

      expect(find.text('Cancelación pendiente'), findsOneWidget);
      expect(find.byKey(const Key('cancelar-r1')), findsNothing);
    });

    testWidgets('«Mis clases»: sin operaciones pendientes se ofrece cancelar como siempre', (tester) async {
      await montarLista(
        tester,
        pendientes: const [],
        overrides: [
          reservasListProvider.overrideWith((ref, vista) async => [reservaDePrueba(codigo: 'r1')]),
        ],
        lista: const ListaDeReservas(vista: VistaReservas.proximas, vacio: EstadoVacio(icono: Icons.event, titulo: 'x', texto: 'x')),
      );

      expect(find.byKey(const Key('cancelar-r1')), findsOneWidget);
    });

    testWidgets('«Mis clases»: una reserva hecha sin conexión aparece como reserva pendiente, aunque no haya otras', (tester) async {
      final franja = franjaDisponibleDePrueba(codigo: 'f1');
      await montarLista(
        tester,
        pendientes: [_accion('reservar', ['f1'], datos: {'franjas': [datosDeFranja(franja)]})],
        overrides: [reservasListProvider.overrideWith((ref, vista) async => <Reserva>[])],
        lista: const ListaDeReservas(vista: VistaReservas.proximas, vacio: EstadoVacio(icono: Icons.event, titulo: 'vacío', texto: 'x')),
      );

      expect(find.text('vacío'), findsNothing);
      expect(find.text('Reserva pendiente'), findsOneWidget);
      expect(find.byKey(const Key('cancelar-f1')), findsNothing);
    });

    testWidgets('agenda del instructor: una cancelación pendiente marca la clase', (tester) async {
      await montarLista(
        tester,
        pendientes: [_accion('cancelar', ['r1'])],
        lista: ListaDeAgenda(clases: [claseDeAgendaDePrueba(cancelable: true)]),
      );

      expect(find.text('Cancelación pendiente'), findsOneWidget);
      expect(find.byKey(const Key('cancelar-r1')), findsNothing);
    });

    testWidgets('agenda del instructor: una evaluación pendiente se reconoce por el código de la clase cerrada', (tester) async {
      await montarLista(
        tester,
        pendientes: [_accion('evaluar', ['c1'])],
        lista: ListaDeAgenda(clases: [claseDeAgendaDePrueba(claseCodigo: 'c1', evaluable: true)]),
      );

      expect(find.text('Evaluación pendiente'), findsOneWidget);
    });
  });
}
