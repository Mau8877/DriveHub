import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/pagos/providers/estado_de_pago_provider.dart';
import 'package:mobile/features/pagos/providers/saldo_horas_provider.dart';
import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/layouts/shared/shell_con_barra.dart';

import '../../helpers/pagos_falsos.dart';
import '../../helpers/sesion_falsa.dart';

UsuarioSesion _con(List<String> roles) =>
    UsuarioSesion(codigo: 'u1', nombres: 'Ana', apellidos: 'Pérez', roles: roles, correoVerificado: true);

const _secciones = [
  SeccionShell('Inicio', Icons.home_outlined, 'resumen'),
  SeccionShell('Perfil', Icons.person_outline, 'CU06'),
];

Future<ProviderContainer> _montar(WidgetTester tester, UsuarioSesion usuario, PagosServiceFalso servicio) async {
  final container = ProviderContainer(
    overrides: [sesionFalsa(SesionFalsa(EstadoSesion.conSesion(usuario))), pagosServiceFalso(servicio)],
  );
  await tester.pumpWidget(
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(theme: AppTheme.light, home: const ShellConBarra(secciones: _secciones)),
    ),
  );
  await tester.pump();
  return container;
}

void main() {
  testWidgets('el estudiante ve sus horas junto a la campanita', (tester) async {
    final servicio = PagosServiceFalso()..saldo = 12;
    await _montar(tester, _con(['ESTUDIANTE']), servicio);

    expect(find.byKey(const Key('saldoHoras')), findsOneWidget);
    expect(find.text('12 h'), findsOneWidget);
    expect(find.byTooltip('Avisos'), findsOneWidget);
  });

  testWidgets('un estudiante sin horas ve 0 h', (tester) async {
    await _montar(tester, _con(['ESTUDIANTE']), PagosServiceFalso());

    expect(find.text('0 h'), findsOneWidget);
  });

  testWidgets('el instructor no ve el saldo ni lo pide', (tester) async {
    final servicio = PagosServiceFalso()..saldo = 12;
    await _montar(tester, _con(['INSTRUCTOR']), servicio);

    expect(find.byKey(const Key('saldoHoras')), findsNothing);
    expect(servicio.pedidosDeSaldo, 0);
  });

  testWidgets('una cuenta con los dos roles entra como instructor y no ve el saldo', (tester) async {
    final servicio = PagosServiceFalso();
    await _montar(tester, _con(['ESTUDIANTE', 'INSTRUCTOR']), servicio);

    expect(find.byKey(const Key('saldoHoras')), findsNothing);
    expect(servicio.pedidosDeSaldo, 0);
  });

  testWidgets('si falla la consulta muestra – h y el encabezado sigue completo', (tester) async {
    final servicio = PagosServiceFalso()..errorSaldo = Exception('sin red');
    await _montar(tester, _con(['ESTUDIANTE']), servicio);

    expect(find.text('– h'), findsOneWidget);
    expect(find.byTooltip('Avisos'), findsOneWidget);
    expect(find.byTooltip('Mi perfil'), findsOneWidget);
  });

  testWidgets('tocar el saldo lo vuelve a pedir', (tester) async {
    final servicio = PagosServiceFalso()..saldo = 3;
    await _montar(tester, _con(['ESTUDIANTE']), servicio);
    expect(find.text('3 h'), findsOneWidget);

    servicio.saldo = 8;
    await tester.tap(find.byKey(const Key('saldoHoras')));
    await tester.pump();
    await tester.pump();

    expect(find.text('8 h'), findsOneWidget);
    expect(servicio.pedidosDeSaldo, 2);
  });

  testWidgets('al confirmarse un pago el saldo se vuelve a pedir', (tester) async {
    final servicio = PagosServiceFalso()
      ..saldo = 0
      ..estados = [EstadoPago.confirmado];
    final container = await _montar(tester, _con(['ESTUDIANTE']), servicio);
    expect(find.text('0 h'), findsOneWidget);

    servicio.saldo = 10;
    final consulta = ConsultaDeEstado('g1', DateTime.now().add(const Duration(minutes: 30)));
    container.listen(estadoDePagoProvider(consulta), (_, _) {});
    await tester.pump();
    await tester.pump();
    await tester.pump();

    expect(find.text('10 h'), findsOneWidget);
    expect(container.read(saldoHorasProvider).value, 10);
  });
}
