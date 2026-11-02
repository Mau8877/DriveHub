import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/models/asistencia_clase.dart';
import 'package:mobile/features/clases/models/clase_agenda.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/providers/agenda_provider.dart';
import 'package:mobile/features/clases/widgets/formulario_clase_sheet.dart';
import 'package:mobile/features/clases/widgets/mensajes_de_cierre.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  final porCerrar = claseDeAgendaDePrueba(codigo: 'r1', cerrable: true, estudianteNombre: 'Eva Estudiante');
  final yaCerrada = claseDeAgendaDePrueba(
    codigo: 'r1',
    estado: EstadoReserva.completada,
    claseCodigo: 'c1',
    estudianteNombre: 'Eva Estudiante',
  );

  setUp(() => servicio = ClasesServiceFalso());

  /// Una pantalla mínima con un botón que abre la hoja de cerrar y otro la de editar.
  Future<ProviderContainer> montar(WidgetTester tester, {ClaseAgenda? aEditar}) async {
    tester.view.physicalSize = const Size(800, 1800);
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
              builder: (context) => Column(
                children: [
                  TextButton(
                    key: const Key('abrirCerrar'),
                    onPressed: () => mostrarCerrarClase(context, porCerrar),
                    child: const Text('abrir cerrar'),
                  ),
                  TextButton(
                    key: const Key('abrirEditar'),
                    onPressed: () => mostrarEditarClase(context, aEditar ?? yaCerrada),
                    child: const Text('abrir editar'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
    return container;
  }

  Future<void> abrirCerrar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('abrirCerrar')));
    await tester.pumpAndSettle();
  }

  Future<void> abrirEditar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('abrirEditar')));
    await tester.pumpAndSettle();
  }

  Future<void> elegir(WidgetTester tester, AsistenciaClase asistencia) async {
    await tester.tap(
      find.descendant(of: find.byKey(const Key('asistencia')), matching: find.text(asistencia.etiqueta)),
    );
    await tester.pumpAndSettle();
  }

  Future<void> escribir(WidgetTester tester, String clave, String texto) async {
    await tester.enterText(find.byKey(Key(clave)), texto);
    await tester.pump();
  }

  Future<void> confirmar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('confirmarCierre')));
    await tester.pumpAndSettle();
  }

  bool confirmarHabilitado(WidgetTester tester) =>
      tester.widget<FilledButton>(find.byKey(const Key('confirmarCierre'))).onPressed != null;

  bool hojaAbierta() => find.byKey(const Key('confirmarCierre')).evaluate().isNotEmpty;

  // ================================================================ cerrar

  group('cerrar una clase', () {
    testWidgets('muestra la clase y los campos, con «Cerrar clase» apagado hasta elegir la asistencia', (tester) async {
      await montar(tester);
      await abrirCerrar(tester);

      expect(find.text('Cerrar clase'), findsNWidgets(2));
      expect(find.byKey(const Key('claseACerrar')), findsOneWidget);
      expect(find.text('Eva Estudiante'), findsOneWidget);
      expect(find.byKey(const Key('asistencia')), findsOneWidget);
      expect(find.byKey(const Key('campoTema')), findsOneWidget);
      expect(find.byKey(const Key('campoObservaciones')), findsOneWidget);
      expect(find.byKey(const Key('campoKmInicio')), findsOneWidget);
      expect(find.byKey(const Key('campoKmFin')), findsOneWidget);
      expect(confirmarHabilitado(tester), isFalse);
      expect(servicio.cierresPedidos, isEmpty);

      await elegir(tester, AsistenciaClase.tarde);
      expect(confirmarHabilitado(tester), isTrue);
    });

    testWidgets('con «Presente» manda el cuerpo, cierra la hoja y avisa que quedó completada', (tester) async {
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);
      await escribir(tester, 'campoTema', 'Giros en U');
      await escribir(tester, 'campoObservaciones', 'Muy bien');
      await escribir(tester, 'campoKmInicio', '1000');
      await escribir(tester, 'campoKmFin', '1042');

      await confirmar(tester);

      expect(servicio.cierresPedidos.single.toJson(), {
        'reservaCodigo': 'r1',
        'asistencia': 'PRESENTE',
        'tema': 'Giros en U',
        'observaciones': 'Muy bien',
        'kmInicio': 1000,
        'kmFin': 1042,
      });
      expect(hojaAbierta(), isFalse);
      expect(find.text('Clase cerrada. Quedó completada.'), findsOneWidget);
    });

    testWidgets('solo con la asistencia no manda los campos vacíos', (tester) async {
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);

      await confirmar(tester);

      expect(servicio.cierresPedidos.single.toJson(), {'reservaCodigo': 'r1', 'asistencia': 'PRESENTE'});
    });

    testWidgets('con «Ausente» oculta tema y kilómetros, avisa que pierde la hora y manda solo observaciones', (
      tester,
    ) async {
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.ausente);

      expect(find.byKey(const Key('campoTema')), findsNothing);
      expect(find.byKey(const Key('campoKmInicio')), findsNothing);
      expect(find.byKey(const Key('campoKmFin')), findsNothing);
      expect(find.text('El estudiante pierde la hora de esta clase.'), findsOneWidget);

      await escribir(tester, 'campoObservaciones', 'No avisó');
      await confirmar(tester);

      expect(servicio.cierresPedidos.single.toJson(), {
        'reservaCodigo': 'r1',
        'asistencia': 'AUSENTE',
        'observaciones': 'No avisó',
      });
      expect(find.text('Clase cerrada. Quedó registrada la inasistencia.'), findsOneWidget);
    });

    testWidgets('elegir «Ausente» vacía lo escrito en tema y kilómetros: no se puede enviar a escondidas', (
      tester,
    ) async {
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);
      await escribir(tester, 'campoTema', 'Giros');
      await escribir(tester, 'campoKmInicio', '100');

      await elegir(tester, AsistenciaClase.ausente);
      await confirmar(tester);

      expect(servicio.cierresPedidos.single.toJson(), {'reservaCodigo': 'r1', 'asistencia': 'AUSENTE'});
    });

    testWidgets('volver a «Presente» muestra los campos vacíos y sin el aviso', (tester) async {
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);
      await escribir(tester, 'campoTema', 'Giros');
      await elegir(tester, AsistenciaClase.ausente);

      await elegir(tester, AsistenciaClase.tarde);

      expect(find.text('El estudiante pierde la hora de esta clase.'), findsNothing);
      expect(tester.widget<TextFormField>(find.byKey(const Key('campoTema'))).controller?.text, isEmpty);
    });

    testWidgets('«Volver» cierra la hoja sin cerrar la clase', (tester) async {
      await montar(tester);
      await abrirCerrar(tester);

      await tester.tap(find.byKey(const Key('volverDeCierre')));
      await tester.pumpAndSettle();

      expect(hojaAbierta(), isFalse);
      expect(servicio.cierresPedidos, isEmpty);
    });
  });

  // ================================================================ validaciones

  group('validaciones', () {
    Future<void> intentarConDato(WidgetTester tester, String campo, String valor, String mensaje) async {
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);
      await escribir(tester, campo, valor);

      await confirmar(tester);

      expect(find.text(mensaje), findsOneWidget);
      expect(servicio.cierresPedidos, isEmpty);
      expect(hojaAbierta(), isTrue);
    }

    testWidgets('un tema de 121 caracteres',
        (tester) => intentarConDato(tester, 'campoTema', 'a' * 121, 'El tema admite hasta 120 caracteres'));

    testWidgets(
      'unas observaciones de 501 caracteres',
      (tester) => intentarConDato(
        tester,
        'campoObservaciones',
        'b' * 501,
        'Las observaciones admiten hasta 500 caracteres',
      ),
    );

    testWidgets('un kilometraje de 10 000 000',
        (tester) => intentarConDato(tester, 'campoKmInicio', '10000000', 'El kilometraje es demasiado grande'));

    testWidgets('un kilometraje final menor que el inicial', (tester) async {
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);
      await escribir(tester, 'campoKmInicio', '200');
      await escribir(tester, 'campoKmFin', '150');

      await confirmar(tester);

      expect(find.text('El kilometraje final no puede ser menor que el inicial'), findsOneWidget);
      expect(servicio.cierresPedidos, isEmpty);
    });

    testWidgets('los límites exactos (120, 500 y 9 999 999) sí se envían', (tester) async {
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);
      await escribir(tester, 'campoTema', 'a' * 120);
      await escribir(tester, 'campoObservaciones', 'b' * 500);
      await escribir(tester, 'campoKmFin', '9999999');

      await confirmar(tester);

      expect(servicio.cierresPedidos, hasLength(1));
    });
  });

  // ================================================================ envío y errores

  group('envío y errores al cerrar', () {
    testWidgets('un doble toque manda una sola petición y deshabilita los botones', (tester) async {
      final espera = Completer<void>();
      servicio.antesDeCerrarClase = () => espera.future;
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);

      await tester.tap(find.byKey(const Key('confirmarCierre')));
      await tester.pump();
      await tester.tap(find.byKey(const Key('confirmarCierre')), warnIfMissed: false);
      await tester.pump();

      expect(servicio.cierresPedidos, hasLength(1));
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      expect(tester.widget<OutlinedButton>(find.byKey(const Key('volverDeCierre'))).onPressed, isNull);

      espera.complete();
      await tester.pumpAndSettle();
      expect(servicio.cierresPedidos, hasLength(1));
    });

    testWidgets('sin red la hoja queda abierta con el aviso y «Reintentar» vuelve a enviar', (tester) async {
      servicio.errorAlCerrarClase = DioException(requestOptions: RequestOptions(path: '/clases'));
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);
      await escribir(tester, 'campoTema', 'Giros');

      await confirmar(tester);

      expect(hojaAbierta(), isTrue);
      expect(find.byKey(const Key('errorCierre')), findsOneWidget);
      expect(find.text('Reintentar'), findsOneWidget);
      expect(tester.widget<TextFormField>(find.byKey(const Key('campoTema'))).controller?.text, 'Giros');

      servicio.errorAlCerrarClase = null;
      await confirmar(tester);

      expect(servicio.cierresPedidos, hasLength(2));
      expect(hojaAbierta(), isFalse);
      expect(find.text('Clase cerrada. Quedó completada.'), findsOneWidget);
    });

    for (final codigo in ['ERR_ACA_37', 'ERR_ACA_35']) {
      testWidgets('$codigo cierra la hoja con el mensaje del servidor y refresca la agenda', (tester) async {
        servicio.errorAlCerrarClase = ApiException(code: codigo, message: 'Esa clase no se puede cerrar');
        final container = await montar(tester);
        container.listen(agendaListProvider, (_, _) {});
        await container.read(agendaListProvider.future);
        await abrirCerrar(tester);
        await elegir(tester, AsistenciaClase.presente);
        servicio.pedidosDeAgenda.clear();

        await confirmar(tester);

        expect(hojaAbierta(), isFalse);
        expect(find.text('Esa clase no se puede cerrar'), findsOneWidget);
        await container.read(agendaListProvider.future);
        expect(servicio.pedidosDeAgenda, hasLength(1));
      });
    }

    testWidgets('ERR_ACA_38 se muestra dentro de la hoja, que sigue abierta con lo escrito y «Cerrar clase»', (
      tester,
    ) async {
      servicio.errorAlCerrarClase = ApiException(code: 'ERR_ACA_38', message: 'Los datos de la clase no son válidos');
      await montar(tester);
      await abrirCerrar(tester);
      await elegir(tester, AsistenciaClase.presente);
      await escribir(tester, 'campoTema', 'Giros');

      await confirmar(tester);

      expect(hojaAbierta(), isTrue);
      expect(find.text('Los datos de la clase no son válidos'), findsOneWidget);
      expect(tester.widget<TextFormField>(find.byKey(const Key('campoTema'))).controller?.text, 'Giros');
      expect(find.text('Reintentar'), findsNothing);
      expect(find.descendant(of: find.byKey(const Key('confirmarCierre')), matching: find.text('Cerrar clase')),
          findsOneWidget);
    });
  });

  // ================================================================ editar

  group('editar una clase cerrada', () {
    setUp(() {
      servicio.claseGuardada = claseDictadaDePrueba(
        asistencia: AsistenciaClase.presente,
        tema: 'Giros',
        observaciones: 'Obs',
        kmInicio: 100,
        kmFin: 150,
      );
    });

    String textoDe(WidgetTester tester, String clave) =>
        tester.widget<TextFormField>(find.byKey(Key(clave))).controller!.text;

    testWidgets('precarga lo guardado, muestra la asistencia sin poder cambiarla y pide la clase por su código', (
      tester,
    ) async {
      await montar(tester);
      await abrirEditar(tester);

      expect(find.text('Editar clase'), findsOneWidget);
      expect(servicio.clasesDictadasPedidas, ['c1']);
      expect(find.byKey(const Key('asistenciaFija')), findsOneWidget);
      expect(find.text('Asistencia: Presente'), findsOneWidget);
      expect(find.byKey(const Key('asistencia')), findsNothing);
      expect(textoDe(tester, 'campoTema'), 'Giros');
      expect(textoDe(tester, 'campoObservaciones'), 'Obs');
      expect(textoDe(tester, 'campoKmInicio'), '100');
      expect(textoDe(tester, 'campoKmFin'), '150');
      expect(find.descendant(of: find.byKey(const Key('confirmarCierre')), matching: find.text('Guardar')),
          findsOneWidget);
    });

    testWidgets('manda solo lo que cambió y avisa que se guardaron los cambios', (tester) async {
      await montar(tester);
      await abrirEditar(tester);
      await escribir(tester, 'campoTema', 'Estacionamiento');

      await confirmar(tester);

      expect(servicio.correccionesPedidas.single.codigo, 'c1');
      expect(servicio.correccionesPedidas.single.request.toJson(), {'tema': 'Estacionamiento'});
      expect(hojaAbierta(), isFalse);
      expect(find.text('Cambios guardados.'), findsOneWidget);
    });

    testWidgets('un texto borrado se manda vacío: así se borra', (tester) async {
      await montar(tester);
      await abrirEditar(tester);
      await escribir(tester, 'campoTema', '');

      await confirmar(tester);

      expect(servicio.correccionesPedidas.single.request.toJson(), {'tema': ''});
    });

    testWidgets('un kilometraje cambiado se manda como número', (tester) async {
      await montar(tester);
      await abrirEditar(tester);
      await escribir(tester, 'campoKmFin', '175');

      await confirmar(tester);

      expect(servicio.correccionesPedidas.single.request.toJson(), {'kmFin': 175});
    });

    testWidgets('sin cambios cierra la hoja sin llamar al servidor', (tester) async {
      await montar(tester);
      await abrirEditar(tester);

      await confirmar(tester);

      expect(servicio.correccionesPedidas, isEmpty);
      expect(hojaAbierta(), isFalse);
    });

    testWidgets('un kilometraje vaciado avisa que no se puede borrar y no se envía', (tester) async {
      await montar(tester);
      await abrirEditar(tester);
      await escribir(tester, 'campoKmInicio', '');
      await escribir(tester, 'campoTema', 'Otro tema');

      await confirmar(tester);

      expect(servicio.correccionesPedidas, isEmpty);
      expect(hojaAbierta(), isTrue);
      expect(find.text(avisoKilometrajeNoSeBorra), findsOneWidget);
    });

    testWidgets('el final menor que el inicial se avisa y no se envía', (tester) async {
      await montar(tester);
      await abrirEditar(tester);
      await escribir(tester, 'campoKmFin', '90');

      await confirmar(tester);

      expect(find.text('El kilometraje final no puede ser menor que el inicial'), findsOneWidget);
      expect(servicio.correccionesPedidas, isEmpty);
    });

    testWidgets('una clase a la que no asistió solo permite editar las observaciones', (tester) async {
      servicio.claseGuardada = claseDictadaDePrueba(asistencia: AsistenciaClase.ausente, observaciones: 'No avisó');
      await montar(tester);
      await abrirEditar(tester);

      expect(find.text('Asistencia: Ausente'), findsOneWidget);
      expect(find.byKey(const Key('campoTema')), findsNothing);
      expect(find.byKey(const Key('campoKmInicio')), findsNothing);
      expect(find.byKey(const Key('campoKmFin')), findsNothing);
      expect(find.byKey(const Key('avisoInasistencia')), findsNothing);

      await escribir(tester, 'campoObservaciones', 'Avisó tarde');
      await confirmar(tester);

      expect(servicio.correccionesPedidas.single.request.toJson(), {'observaciones': 'Avisó tarde'});
    });

    testWidgets('si no se pueden traer los datos muestra el error y «Reintentar» los vuelve a pedir', (tester) async {
      servicio.errorClaseDictada = DioException(requestOptions: RequestOptions(path: '/clases/c1'));
      await montar(tester);
      await abrirEditar(tester);

      expect(find.byKey(const Key('errorCargarClase')), findsOneWidget);
      expect(find.byKey(const Key('campoTema')), findsNothing);

      servicio.errorClaseDictada = null;
      await tester.tap(find.text('Reintentar'));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('errorCargarClase')), findsNothing);
      expect(textoDe(tester, 'campoTema'), 'Giros');
    });

    testWidgets('ERR_ACA_35 al guardar cierra la hoja con el mensaje del servidor', (tester) async {
      servicio.errorAlActualizarClase = ApiException(code: 'ERR_ACA_35', message: 'No encontramos esa clase');
      await montar(tester);
      await abrirEditar(tester);
      await escribir(tester, 'campoTema', 'Otro');

      await confirmar(tester);

      expect(hojaAbierta(), isFalse);
      expect(find.text('No encontramos esa clase'), findsOneWidget);
    });

    testWidgets('ERR_ACA_38 al guardar se muestra dentro y sin red pasa a «Reintentar»', (tester) async {
      servicio.errorAlActualizarClase = ApiException(code: 'ERR_ACA_38', message: 'Datos inválidos');
      await montar(tester);
      await abrirEditar(tester);
      await escribir(tester, 'campoTema', 'Otro');

      await confirmar(tester);
      expect(find.text('Datos inválidos'), findsOneWidget);
      expect(hojaAbierta(), isTrue);

      servicio.errorAlActualizarClase = DioException(requestOptions: RequestOptions(path: '/clases/c1'));
      await confirmar(tester);
      expect(find.text('Reintentar'), findsOneWidget);
      expect(hojaAbierta(), isTrue);
    });
  });
}
