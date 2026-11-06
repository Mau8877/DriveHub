import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/asistente/models/accion_asistente.dart';
import 'package:mobile/features/asistente/models/nombre_accion.dart';
import 'package:mobile/features/asistente/models/resultado_interpretacion.dart';
import 'package:mobile/features/asistente/providers/modelo_de_acciones_provider.dart';
import 'package:mobile/features/asistente/providers/reconocedor_de_voz_provider.dart';
import 'package:mobile/features/asistente/services/modelo_de_acciones.dart';
import 'package:mobile/features/asistente/services/reconocedor_de_voz.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/clases/screens/clases_screen.dart';

import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';
import '../../helpers/sesion_falsa.dart';
import 'asistente_test.dart';

void main() {
  late VozFalsa voz;
  late ModeloFalso modelo;
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  setUp(() {
    voz = VozFalsa();
    modelo = ModeloFalso();
    servicio = ClasesServiceFalso();
    pagos = PagosServiceFalso()..saldo = 5;
  });

  Future<ProviderContainer> montar(WidgetTester tester) async {
    final container = ProviderContainer(
      overrides: [
        clasesServiceFalso(servicio),
        pagosServiceFalso(pagos),
        sesionFalsa(
          SesionFalsa(
            EstadoSesion.conSesion(
              const UsuarioSesion(codigo: 'u1', nombres: 'Ana', apellidos: 'Pérez', roles: ['ESTUDIANTE']),
            ),
          ),
        ),
        reconocedorDeVozProvider.overrideWith((ref) async => voz),
        modeloDeAccionesProvider.overrideWith((ref) async => modelo),
      ],
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(theme: AppTheme.light, home: const Scaffold(body: ClasesScreen())),
      ),
    );
    await tester.pumpAndSettle();
    return container;
  }

  /// Mantiene pulsado el micrófono y suelta, como lo haría la persona.
  Future<void> hablar(WidgetTester tester) async {
    final gesto = await tester.startGesture(tester.getCenter(find.byKey(const Key('botonMicrofono'))));
    await tester.pump();
    await gesto.up();
    await tester.pumpAndSettle();
  }

  void accion(NombreAccion a) => modelo.resultado = AccionEntendida(AccionAsistente(accion: a));

  testWidgets('el micrófono está al lado de las horas y el panel no se ve mientras no se usa', (tester) async {
    await montar(tester);

    expect(find.byKey(const Key('botonMicrofono')), findsOneWidget);
    expect(find.byKey(const Key('panelAsistente')), findsNothing);
  });

  testWidgets('mantener pulsado graba y soltar muestra el resultado con lo que se oyó', (tester) async {
    accion(NombreAccion.verHorasDisponibles);
    voz.texto = 'cuántas horas me quedan';
    await montar(tester);

    await hablar(tester);

    expect(voz.empezadas, 1);
    expect(voz.terminadas, 1);
    expect(find.text('«cuántas horas me quedan»'), findsOneWidget);
    expect(find.text('Te quedan 5 horas.'), findsOneWidget);
  });

  testWidgets('cerrar el resultado oculta el panel', (tester) async {
    accion(NombreAccion.verHorasDisponibles);
    await montar(tester);
    await hablar(tester);

    await tester.tap(find.byKey(const Key('cerrarAsistente')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('panelAsistente')), findsNothing);
  });

  testWidgets('cancelar una clase: el panel pide continuar y abre el diálogo de siempre, sin cancelar nada', (
    tester,
  ) async {
    servicio.proximas = [reservaDePrueba(codigo: 'r1', cancelacionATiempo: true)];
    accion(NombreAccion.cancelarClase);
    await montar(tester);

    await hablar(tester);

    expect(find.textContaining('Cancelar tu clase'), findsOneWidget);
    expect(servicio.cancelacionesPedidas, isEmpty);

    await tester.tap(find.byKey(const Key('continuarCambio')));
    await tester.pumpAndSettle();

    expect(find.text('¿Cancelar esta clase?'), findsOneWidget);
    expect(servicio.cancelacionesPedidas, isEmpty, reason: 'solo se cancela al confirmar en el diálogo');

    await tester.tap(find.byKey(const Key('volverDeCancelar')));
    await tester.pumpAndSettle();

    expect(servicio.cancelacionesPedidas, isEmpty);
    expect(find.byKey(const Key('panelAsistente')), findsNothing, reason: 'el asistente queda inactivo');
  });

  testWidgets('confirmar en el diálogo cancela la clase', (tester) async {
    servicio.proximas = [reservaDePrueba(codigo: 'r1', cancelacionATiempo: true)];
    accion(NombreAccion.cancelarClase);
    await montar(tester);
    await hablar(tester);

    await tester.tap(find.byKey(const Key('continuarCambio')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('confirmarCancelacion')));
    await tester.pumpAndSettle();

    expect(servicio.cancelacionesPedidas, ['r1']);
  });

  testWidgets('«No» descarta la propuesta sin tocar nada', (tester) async {
    servicio.proximas = [reservaDePrueba(codigo: 'r1', cancelacionATiempo: true)];
    accion(NombreAccion.cancelarClase);
    await montar(tester);
    await hablar(tester);

    await tester.tap(find.byKey(const Key('descartarCambio')));
    await tester.pumpAndSettle();

    expect(servicio.cancelacionesPedidas, isEmpty);
    expect(find.byKey(const Key('panelAsistente')), findsNothing);
  });

  testWidgets('con varias clases posibles se elige una y recién entonces se propone el cambio', (tester) async {
    servicio.proximas = [
      reservaDePrueba(codigo: 'r1', horaInicio: '10:00', horaFin: '11:00', cancelacionATiempo: true),
      reservaDePrueba(codigo: 'r2', horaInicio: '15:00', horaFin: '16:00', cancelacionATiempo: true),
    ];
    modelo.resultado = const AccionEntendida(AccionAsistente(accion: NombreAccion.cancelarClase, instructor: 'Ivan'));
    await montar(tester);
    await hablar(tester);

    expect(find.text('¿Cuál clase quieres cancelar?'), findsOneWidget);
    expect(find.byKey(const Key('candidata-r1')), findsOneWidget);
    expect(find.byKey(const Key('candidata-r2')), findsOneWidget);

    await tester.tap(find.byKey(const Key('candidata-r2')));
    await tester.pumpAndSettle();

    expect(find.textContaining('15:00'), findsWidgets);
    expect(find.byKey(const Key('continuarCambio')), findsOneWidget);
    expect(servicio.cancelacionesPedidas, isEmpty);
  });

  testWidgets('lo que no se entiende se explica y se puede cerrar', (tester) async {
    modelo.resultado = const AccionRechazada(MotivoRechazo.fueraDeAlcance);
    await montar(tester);

    await hablar(tester);

    expect(find.textContaining('Eso no se puede hacer desde Clases'), findsOneWidget);
  });

  testWidgets('sin permiso de micrófono lo avisa', (tester) async {
    voz.errorAlEmpezar = const PermisoDeMicrofonoDenegado();
    await montar(tester);

    await hablar(tester);

    expect(find.textContaining('permiso para usar el micrófono'), findsOneWidget);
  });

  testWidgets('sin los modelos avisa y la pantalla sigue usable', (tester) async {
    modelo.error = const ModeloDeAccionesNoDisponible('x');
    await montar(tester);

    await hablar(tester);

    expect(find.text('El asistente no está disponible en este teléfono.'), findsOneWidget);
    expect(find.text('Mis clases'), findsOneWidget);
    expect(find.text('Reservar'), findsOneWidget);
  });
}
