import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/providers/notificaciones_providers.dart';
import 'package:mobile/features/notificaciones/screens/preferencias_notificacion_screen.dart';
import 'package:mobile/features/perfil/providers/perfil_provider.dart';

import '../../helpers/preferencias_falsas.dart';
import '../../helpers/sesion_falsa.dart';

Future<void> _montar(
  WidgetTester tester,
  ServicioPreferenciasFalso servicio,
) async {
  tester.view.physicalSize = const Size(900, 3200);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sesionFalsa(SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba))),
        perfilServiceProvider.overrideWithValue(PerfilFalso()),
        preferenciasServiceProvider.overrideWithValue(servicio),
      ],
      child: MaterialApp(
        theme: AppTheme.light,
        home: const PreferenciasNotificacionScreen(),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Finder _interruptor(String titulo) =>
    find.widgetWithText(SwitchListTile, titulo);

bool _activado(WidgetTester tester, String titulo) =>
    tester.widget<SwitchListTile>(_interruptor(titulo)).value;

Finder get _guardar => find.widgetWithText(FilledButton, 'Guardar');

bool _guardarHabilitado(WidgetTester tester) =>
    tester.widget<FilledButton>(_guardar).onPressed != null;

void main() {
  testWidgets(
    'con todo por defecto muestra los canales y los tipos activados y el silencio apagado',
    (tester) async {
      await _montar(tester, ServicioPreferenciasFalso());

      for (final titulo in [
        'Correo',
        'Notificaciones push',
        'Dentro de la app',
        'Clases y reservas',
        'Pagos',
        'Exámenes',
        'Recordatorios',
      ]) {
        expect(_activado(tester, titulo), isTrue, reason: titulo);
      }
      expect(_activado(tester, 'Horas de silencio'), isFalse);
      expect(find.text('Desde'), findsNothing);
      expect(
        find.textContaining('solo detiene las notificaciones push'),
        findsOneWidget,
      );
    },
  );

  testWidgets('Guardar está deshabilitado hasta que se cambia algo', (
    tester,
  ) async {
    await _montar(tester, ServicioPreferenciasFalso());
    expect(_guardarHabilitado(tester), isFalse);

    await tester.tap(_interruptor('Pagos'));
    await tester.pump();
    expect(_guardarHabilitado(tester), isTrue);

    // Volver a como estaba deja de ser un cambio
    await tester.tap(_interruptor('Pagos'));
    await tester.pump();
    expect(_guardarHabilitado(tester), isFalse);
  });

  testWidgets(
    'guarda la configuración completa con el correo del perfil y avisa',
    (tester) async {
      final servicio = ServicioPreferenciasFalso();
      await _montar(tester, servicio);

      await tester.tap(_interruptor('Pagos'));
      await tester.tap(_interruptor('Notificaciones push'));
      await tester.pump();
      await tester.tap(_guardar);
      await tester.pumpAndSettle();

      expect(servicio.enviadas!.tipos[TipoNotificacion.pago], isFalse);
      expect(servicio.enviadas!.push, isFalse);
      expect(servicio.enviadas!.correo, isTrue);
      expect(servicio.enviadas!.tieneSilencio, isFalse);
      expect(servicio.correoEnviado, 'ana@gmail.com');
      expect(find.text('Preferencias guardadas'), findsOneWidget);
      expect(_guardarHabilitado(tester), isFalse);
    },
  );

  testWidgets(
    'al encender el silencio propone 22:00 a 06:00 y lo envía al guardar',
    (tester) async {
      final servicio = ServicioPreferenciasFalso();
      await _montar(tester, servicio);

      await tester.tap(_interruptor('Horas de silencio'));
      await tester.pump();

      expect(find.text('Desde'), findsOneWidget);
      expect(find.text('Hasta'), findsOneWidget);
      expect(find.text('22:00'), findsOneWidget);
      expect(find.text('06:00'), findsOneWidget);

      await tester.tap(_guardar);
      await tester.pumpAndSettle();

      expect(servicio.enviadas!.silencioDesde, '22:00');
      expect(servicio.enviadas!.silencioHasta, '06:00');
    },
  );

  testWidgets('apagar el silencio guardado lo envía sin franja', (
    tester,
  ) async {
    final servicio = ServicioPreferenciasFalso()
      ..guardadas = PreferenciasNotificacion.porDefecto().conSilencio(
        desde: '23:00',
        hasta: '05:30',
      );
    await _montar(tester, servicio);
    expect(_activado(tester, 'Horas de silencio'), isTrue);
    expect(find.text('23:00'), findsOneWidget);

    await tester.tap(_interruptor('Horas de silencio'));
    await tester.pump();
    await tester.tap(_guardar);
    await tester.pumpAndSettle();

    expect(servicio.enviadas!.silencioDesde, isNull);
    expect(servicio.enviadas!.silencioHasta, isNull);
  });

  testWidgets('dos horas iguales muestran el motivo y no dejan guardar', (
    tester,
  ) async {
    await _montar(tester, ServicioPreferenciasFalso());
    await tester.tap(_interruptor('Horas de silencio'));
    await tester.pump();

    final contenedor = ProviderScope.containerOf(
      tester.element(find.byType(PreferenciasNotificacionScreen)),
    );
    contenedor
        .read(preferenciasFormUiProvider.notifier)
        .editar(
          PreferenciasNotificacion.porDefecto(),
          (p) => p.conSilencio(desde: '10:00', hasta: '10:00'),
        );
    await tester.pump();

    expect(
      find.text(
        'La hora de inicio y la de fin del silencio no pueden ser iguales',
      ),
      findsOneWidget,
    );
    expect(_guardarHabilitado(tester), isFalse);
  });

  testWidgets('tocar «Desde» abre el selector de hora', (tester) async {
    await _montar(tester, ServicioPreferenciasFalso());
    await tester.tap(_interruptor('Horas de silencio'));
    await tester.pump();

    await tester.tap(find.text('Desde'));
    await tester.pumpAndSettle();
    expect(find.byType(TimePickerDialog), findsOneWidget);

    await tester.tap(find.text('Cancel'));
    await tester.pumpAndSettle();
    expect(find.byType(TimePickerDialog), findsNothing);
  });

  testWidgets(
    'si la carga falla no hay interruptores y «Reintentar» los trae',
    (tester) async {
      final servicio = ServicioPreferenciasFalso()
        ..errorAlObtener = Exception('sin red');
      await _montar(tester, servicio);

      expect(find.text('No pudimos cargar tus preferencias'), findsOneWidget);
      expect(find.byType(SwitchListTile), findsNothing);

      servicio.errorAlObtener = null;
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(find.byType(SwitchListTile), findsWidgets);
      expect(find.text('No pudimos cargar tus preferencias'), findsNothing);
    },
  );

  testWidgets(
    'si el guardado falla conserva lo tocado, muestra el motivo y deja reintentar',
    (tester) async {
      final servicio = ServicioPreferenciasFalso()
        ..errorAlGuardar = ApiException(
          code: 'ERR_NOT_03',
          message: 'Elige las dos horas de silencio, o ninguna',
        );
      await _montar(tester, servicio);

      await tester.tap(_interruptor('Pagos'));
      await tester.pump();
      await tester.tap(_guardar);
      await tester.pumpAndSettle();

      expect(
        find.text('Elige las dos horas de silencio, o ninguna'),
        findsOneWidget,
      );
      expect(_activado(tester, 'Pagos'), isFalse);
      expect(_guardarHabilitado(tester), isTrue);
      expect(find.text('Preferencias guardadas'), findsNothing);

      // Al reintentar con el servicio ya sano, guarda
      servicio.errorAlGuardar = null;
      await tester.tap(_guardar);
      await tester.pumpAndSettle();
      expect(find.text('Preferencias guardadas'), findsOneWidget);
      expect(
        find.text('Elige las dos horas de silencio, o ninguna'),
        findsNothing,
      );
    },
  );

  testWidgets('un fallo de red al guardar muestra un mensaje genérico', (
    tester,
  ) async {
    final servicio = ServicioPreferenciasFalso()
      ..errorAlGuardar = Exception('sin red');
    await _montar(tester, servicio);

    await tester.tap(_interruptor('Pagos'));
    await tester.pump();
    await tester.tap(_guardar);
    await tester.pumpAndSettle();

    expect(
      find.textContaining('No pudimos guardar tus preferencias'),
      findsOneWidget,
    );
    expect(_activado(tester, 'Pagos'), isFalse);
  });
}
