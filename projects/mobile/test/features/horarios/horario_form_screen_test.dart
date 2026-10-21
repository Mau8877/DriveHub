import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/horarios/screens/horario_form_screen.dart';

import '../../helpers/horarios_falsos.dart';

/// El formulario de un horario nuevo: lo que tiene lógica (validación, vista previa y errores del backend).
void main() {
  late HorariosServiceFalso servicio;

  setUp(() => servicio = HorariosServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => Scaffold(
            body: Builder(
              builder: (contexto) =>
                  TextButton(onPressed: () => contexto.push('/schedules/new'), child: const Text('abrir')),
            ),
          ),
        ),
        GoRoute(path: '/schedules/new', builder: (_, _) => const HorarioFormScreen()),
      ],
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [horariosServiceFalso(servicio)],
        child: MaterialApp.router(routerConfig: router),
      ),
    );
    await tester.tap(find.text('abrir'));
    await tester.pumpAndSettle();
  }

  /// Elige una hora con el selector: toca el campo y acepta la hora que propone.
  Future<void> aceptarHora(WidgetTester tester, Key campo) async {
    await tester.ensureVisible(find.byKey(campo));
    await tester.tap(find.byKey(campo));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
  }

  Future<void> elegirDias(WidgetTester tester, List<int> dias) async {
    for (final dia in dias) {
      await tester.ensureVisible(find.byKey(Key('dia$dia')));
      await tester.tap(find.byKey(Key('dia$dia')));
      await tester.pump();
    }
  }

  Future<void> escribirNombre(WidgetTester tester, String nombre) async {
    await tester.enterText(find.byKey(const Key('campoNombre')), nombre);
    await tester.pump();
  }

  Future<void> guardar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('botonGuardarHorario')));
    await tester.pumpAndSettle();
  }

  /// Lo mínimo para que el formulario sea válido: nombre, un día y las dos horas (08:00 a 09:00).
  Future<void> llenarLoNecesario(WidgetTester tester) async {
    await escribirNombre(tester, 'Mañanas');
    await elegirDias(tester, [1]);
    await aceptarHora(tester, const Key('campoHoraInicio'));
    await aceptarHora(tester, const Key('campoHoraFin'));
  }

  group('validación', () {
    testWidgets('con los campos vacíos, Guardar muestra los mensajes y no llama al servicio', (tester) async {
      await montar(tester);

      await guardar(tester);

      expect(find.text('El nombre es obligatorio'), findsOneWidget);
      expect(find.text('Elige al menos un día de la semana'), findsOneWidget);
      expect(find.text('La hora de inicio es obligatoria'), findsOneWidget);
      expect(find.text('La hora de fin es obligatoria'), findsOneWidget);
      expect(servicio.llamadas, isEmpty);
    });

    testWidgets('sin ningún día elegido no guarda aunque lo demás esté bien', (tester) async {
      await montar(tester);
      await escribirNombre(tester, 'Mañanas');
      await aceptarHora(tester, const Key('campoHoraInicio'));
      await aceptarHora(tester, const Key('campoHoraFin'));

      await guardar(tester);

      expect(find.byKey(const Key('errorDias')), findsOneWidget);
      expect(servicio.llamadas, isEmpty);
    });

    testWidgets('el aviso de los días no aparece antes de intentar guardar', (tester) async {
      await montar(tester);

      expect(find.byKey(const Key('errorDias')), findsNothing);
    });

    testWidgets('al elegir un día desaparece el aviso de los días', (tester) async {
      await montar(tester);
      await guardar(tester);
      expect(find.byKey(const Key('errorDias')), findsOneWidget);

      await elegirDias(tester, [2]);

      expect(find.byKey(const Key('errorDias')), findsNothing);
    });

    testWidgets('un tramo que no es múltiplo de la duración muestra su mensaje', (tester) async {
      await montar(tester);
      await escribirNombre(tester, 'Mañanas');
      await elegirDias(tester, [1]);
      await aceptarHora(tester, const Key('campoHoraInicio'));
      await aceptarHora(tester, const Key('campoHoraFin'));
      // * 08:00 a 09:00 (la hora de fin propuesta es inicio + 60) no es múltiplo de 45
      await tester.tap(find.text('45 min'));
      await tester.pumpAndSettle();

      await guardar(tester);

      expect(find.text('El tramo entre inicio y fin debe ser un múltiplo de la duración de la clase'), findsWidgets);
      expect(servicio.llamadas, isEmpty);
    });

    testWidgets('el nombre no admite más de 60 caracteres', (tester) async {
      await montar(tester);

      await tester.enterText(find.byKey(const Key('campoNombre')), 'a' * 80);
      await tester.pump();

      final campo = tester.widget<TextField>(
        find.descendant(of: find.byKey(const Key('campoNombre')), matching: find.byType(TextField)),
      );
      expect(campo.controller!.text.length, 60);
    });
  });

  group('vista previa', () {
    testWidgets('al principio pide elegir los días y las horas', (tester) async {
      await montar(tester);

      expect(find.text('Elige los días y las horas para ver cuántas franjas se crearán.'), findsOneWidget);
    });

    testWidgets('con días y horas válidos muestra las franjas por día y por semana, y se recalcula en vivo', (
      tester,
    ) async {
      await montar(tester);
      await aceptarHora(tester, const Key('campoHoraInicio'));
      await aceptarHora(tester, const Key('campoHoraFin'));
      await elegirDias(tester, [1, 2, 3]);

      // * 08:00 a 09:00 con clases de 60 min = 1 por día, en 3 días
      expect(find.text('Se crearán 1 franja por día, 3 por semana.'), findsOneWidget);

      await elegirDias(tester, [4]);
      expect(find.text('Se crearán 1 franja por día, 4 por semana.'), findsOneWidget);

      await elegirDias(tester, [4]);
      expect(find.text('Se crearán 1 franja por día, 3 por semana.'), findsOneWidget);
    });

    testWidgets('si la duración no divide la ventana, vuelve al mensaje inicial', (tester) async {
      await montar(tester);
      await aceptarHora(tester, const Key('campoHoraInicio'));
      await aceptarHora(tester, const Key('campoHoraFin'));
      await elegirDias(tester, [1]);

      await tester.tap(find.text('45 min'));
      await tester.pumpAndSettle();

      expect(find.text('Elige los días y las horas para ver cuántas franjas se crearán.'), findsOneWidget);
    });
  });

  group('guardar', () {
    testWidgets('con datos válidos llama al servicio con el pedido esperado y vuelve con el aviso', (tester) async {
      await montar(tester);
      await escribirNombre(tester, '  Mañanas  ');
      await elegirDias(tester, [5, 1, 3]);
      await aceptarHora(tester, const Key('campoHoraInicio'));
      await aceptarHora(tester, const Key('campoHoraFin'));

      await guardar(tester);

      expect(servicio.llamadas, contains('crearPlantilla'));
      final pedido = servicio.ultimoRequest!;
      expect(pedido.nombre, 'Mañanas');
      expect(pedido.diasSemana, [1, 3, 5]);
      expect(pedido.horaInicio, '08:00');
      expect(pedido.horaFin, '09:00');
      expect(pedido.duracionClaseMin, 60);
      expect(pedido.vigenteDesde, isNotNull);
      expect(pedido.vigenteHasta, isNull);
      // * Volvió a la pantalla anterior y avisó cuántas franjas se generaron
      expect(find.text('abrir'), findsOneWidget);
      expect(find.text('Horario creado: 14 franjas generadas'), findsOneWidget);
    });

    testWidgets('la duración elegida viaja en el pedido y la hora de fin sugerida la respeta', (tester) async {
      await montar(tester);
      await escribirNombre(tester, 'Mañanas');
      await elegirDias(tester, [1]);
      await aceptarHora(tester, const Key('campoHoraInicio'));
      await tester.ensureVisible(find.text('90 min'));
      await tester.tap(find.text('90 min'));
      await tester.pumpAndSettle();
      // * La hora de fin que propone el selector es el inicio más una clase: 08:00 + 90 min
      await aceptarHora(tester, const Key('campoHoraFin'));

      await guardar(tester);

      expect(servicio.ultimoRequest!.duracionClaseMin, 90);
      expect(servicio.ultimoRequest!.horaFin, '09:30');
    });

    testWidgets('mientras guarda, el botón se deshabilita y dice Guardando', (tester) async {
      final respuesta = Completer<void>();
      servicio.antesDeResponder = () => respuesta.future;
      await montar(tester);
      await llenarLoNecesario(tester);

      await tester.tap(find.byKey(const Key('botonGuardarHorario')));
      await tester.pump();

      expect(find.text('Guardando…'), findsOneWidget);
      final boton = tester.widget<FilledButton>(find.byKey(const Key('botonGuardarHorario')));
      expect(boton.onPressed, isNull);

      respuesta.complete();
      await tester.pumpAndSettle();
      expect(find.text('Guardando…'), findsNothing);
    });

    testWidgets('un error de negocio del backend se muestra en el formulario y no pierde lo escrito', (tester) async {
      servicio.error = ApiException(code: 'ERR_ACA_04', message: 'El horario se cruza con otro horario activo tuyo');
      await montar(tester);
      await llenarLoNecesario(tester);

      await guardar(tester);

      expect(find.text('El horario se cruza con otro horario activo tuyo'), findsOneWidget);
      expect(find.text('Nuevo horario'), findsWidgets);
      expect(find.text('08:00'), findsOneWidget);
      expect(find.text('Mañanas'), findsOneWidget);
    });

    testWidgets('un error de red muestra el aviso genérico', (tester) async {
      servicio.error = StateError('sin red');
      await montar(tester);
      await llenarLoNecesario(tester);

      await guardar(tester);

      expect(find.text('No pudimos conectar con el servidor. Intenta de nuevo.'), findsOneWidget);
    });
  });
}
