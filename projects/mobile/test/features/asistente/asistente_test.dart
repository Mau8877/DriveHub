import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/models/accion_asistente.dart';
import 'package:mobile/features/asistente/models/estado_asistente.dart';
import 'package:mobile/features/asistente/models/nombre_accion.dart';
import 'package:mobile/features/asistente/models/preparacion_de_cambio.dart';
import 'package:mobile/features/asistente/models/resultado_interpretacion.dart';
import 'package:mobile/features/asistente/providers/asistente_provider.dart';
import 'package:mobile/features/asistente/providers/modelo_de_acciones_provider.dart';
import 'package:mobile/features/asistente/providers/reconocedor_de_voz_provider.dart';
import 'package:mobile/features/asistente/services/modelo_de_acciones.dart';
import 'package:mobile/features/asistente/services/reconocedor_de_voz.dart';
import 'package:mobile/features/clases/models/reserva.dart';

import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

class VozFalsa implements ReconocedorDeVoz {
  String texto = 'cancela mi próxima clase';
  Object? errorAlEmpezar;
  Object? errorAlTerminar;

  /// Si no es null, `empezar` espera a que se complete: simula un micrófono que tarda en arrancar.
  Completer<void>? arranque;
  int empezadas = 0;
  int terminadas = 0;
  int canceladas = 0;

  @override
  Future<void> empezar() async {
    empezadas++;
    await arranque?.future;
    final e = errorAlEmpezar;
    if (e != null) throw e;
  }

  @override
  Future<String> terminar() async {
    terminadas++;
    final e = errorAlTerminar;
    if (e != null) throw e;
    return texto;
  }

  @override
  Future<void> cancelar() async => canceladas++;
}

class ModeloFalso implements ModeloDeAcciones {
  ResultadoInterpretacion resultado = const AccionEntendida(AccionAsistente(accion: NombreAccion.verMisClases));
  Object? error;
  final List<String> textos = [];

  @override
  Future<ResultadoInterpretacion> entender(String texto) async {
    textos.add(texto);
    final e = error;
    if (e != null) throw e;
    return resultado;
  }

  @override
  Future<void> liberar() async {}
}

void main() {
  late VozFalsa voz;
  late ModeloFalso modelo;
  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  setUp(() {
    voz = VozFalsa();
    modelo = ModeloFalso();
    servicio = ClasesServiceFalso();
    pagos = PagosServiceFalso()..saldo = 3;
  });

  ProviderContainer crear() {
    final container = ProviderContainer(
      overrides: [
        clasesServiceFalso(servicio),
        pagosServiceFalso(pagos),
        reconocedorDeVozProvider.overrideWith((ref) async => voz),
        modeloDeAccionesProvider.overrideWith((ref) async => modelo),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  /// Mantiene pulsado y suelta: el recorrido completo de una petición.
  Future<EstadoAsistente> hablar(ProviderContainer c) async {
    final n = c.read(asistenteProvider.notifier);
    await n.empezarAEscuchar();
    await n.terminarDeEscuchar();
    return c.read(asistenteProvider);
  }

  Reserva clase(String codigo, DateTime fecha, String hora) => reservaDePrueba(codigo: codigo, fecha: fecha, horaInicio: hora, cancelacionATiempo: true);

  void accion(NombreAccion a, {String? dia, String? hora}) =>
      modelo.resultado = AccionEntendida(AccionAsistente(accion: a, dia: dia, hora: hora));

  test('arranca inactivo', () {
    expect(crear().read(asistenteProvider), isA<Inactivo>());
  });

  group('consultas', () {
    test('se ejecutan al momento y quedan como resultado', () async {
      accion(NombreAccion.verHorasDisponibles);
      voz.texto = 'cuántas horas me quedan';
      final c = crear();

      final estado = await hablar(c);

      expect(modelo.textos, ['cuántas horas me quedan']);
      final r = estado as Resultado;
      expect(r.exito, isTrue);
      expect(r.mensaje, 'Te quedan 3 horas.');
      expect(r.texto, 'cuántas horas me quedan');
    });

    test('sin conexión es un resultado de fallo, no una excepción', () async {
      accion(NombreAccion.verHorasDisponibles);
      pagos.errorSaldo = Exception('sin red');

      final estado = await hablar(crear());

      expect((estado as Resultado).exito, isFalse);
    });
  });

  group('cancelar y reprogramar', () {
    test('identifica la clase y pide el cambio, sin cancelar nada', () async {
      servicio.proximas = [clase('r1', DateTime(2099, 11, 2), '10:00')];
      accion(NombreAccion.cancelarClase);

      final estado = await hablar(crear());

      final p = (estado as PideCambio).propuesta;
      expect(p.tipo, TipoCambio.cancelar);
      expect(p.reserva.codigo, 'r1');
      expect(servicio.cancelacionesPedidas, isEmpty);
      expect(servicio.reprogramacionesPedidas, isEmpty);
    });

    test('reprogramar tampoco cambia nada: pide el cambio', () async {
      servicio.proximas = [clase('r1', DateTime(2099, 11, 2), '10:00')];
      accion(NombreAccion.reprogramarClase);

      final estado = await hablar(crear());

      expect((estado as PideCambio).propuesta.tipo, TipoCambio.reprogramar);
      expect(servicio.reprogramacionesPedidas, isEmpty);
    });

    test('con varias clases posibles, la persona elige y recién ahí se pide el cambio', () async {
      servicio.proximas = [clase('r1', DateTime(2099, 11, 2), '10:00'), clase('r2', DateTime(2099, 11, 2), '15:00')];
      final c = crear();
      // * Un día que cuadra con las dos: usamos la hora para que cuadren ambas no hace falta; sin filtros va a la próxima
      modelo.resultado = AccionEntendida(AccionAsistente(accion: NombreAccion.cancelarClase, instructor: 'Ivan'));

      final estado = await hablar(c);

      final e = estado as Elegir;
      expect(e.candidatas.map((r) => r.codigo), ['r1', 'r2']);

      c.read(asistenteProvider.notifier).elegir(e.candidatas.last);

      final p = (c.read(asistenteProvider) as PideCambio).propuesta;
      expect(p.reserva.codigo, 'r2');
      expect(servicio.cancelacionesPedidas, isEmpty);
    });

    test('sin clases por venir lo dice', () async {
      accion(NombreAccion.cancelarClase);

      final estado = await hablar(crear());

      final r = estado as Resultado;
      expect(r.exito, isFalse);
      expect(r.mensaje, 'No tienes clases por venir.');
    });

    test('reprogramar fuera de plazo da el motivo y no pide el cambio', () async {
      servicio.proximas = [reservaDePrueba(fecha: DateTime(2099, 11, 2), cancelacionATiempo: false)];
      accion(NombreAccion.reprogramarClase);

      final estado = await hablar(crear());

      expect((estado as Resultado).mensaje, contains('No se puede reprogramar'));
    });
  });

  group('lo que no se entiende', () {
    test('fuera de alcance explica qué sí puede hacer', () async {
      modelo.resultado = const AccionRechazada(MotivoRechazo.fueraDeAlcance);
      voz.texto = 'cuéntame un chiste';

      final estado = await hablar(crear());

      final n = estado as NoEntendi;
      expect(n.texto, 'cuéntame un chiste');
      expect(n.mensaje, contains('Eso no se puede hacer desde Clases'));
    });

    test('falta un dato pide más detalle', () async {
      modelo.resultado = const AccionRechazada(MotivoRechazo.faltaDato);
      expect(((await hablar(crear())) as NoEntendi).mensaje, contains('Me falta un dato'));
    });

    test('una acción inventada o inválida dice que no entendió', () async {
      modelo.resultado = const AccionRechazada(MotivoRechazo.desconocida);
      expect(((await hablar(crear())) as NoEntendi).mensaje, contains('No entendí'));
    });

    test('audio vacío: pide repetir y no consulta al modelo', () async {
      voz.texto = '';

      final estado = await hablar(crear());

      expect((estado as NoEntendi).mensaje, contains('No te escuché'));
      expect(modelo.textos, isEmpty);
    });
  });

  group('permisos y fallos', () {
    test('sin permiso de micrófono', () async {
      voz.errorAlEmpezar = const PermisoDeMicrofonoDenegado();
      final c = crear();

      await c.read(asistenteProvider.notifier).empezarAEscuchar();

      expect(c.read(asistenteProvider), isA<SinPermiso>());
    });

    test('sin el modelo de voz', () async {
      voz.errorAlTerminar = const ModeloDeVozNoDisponible('x');
      expect(((await hablar(crear())) as Fallo).mensaje, contains('voz no está disponible'));
    });

    test('sin el modelo de acciones', () async {
      modelo.error = const ModeloDeAccionesNoDisponible('x');
      expect(((await hablar(crear())) as Fallo).mensaje, 'El asistente no está disponible en este teléfono.');
    });

    test('un error inesperado nunca llega como excepción', () async {
      modelo.error = StateError('boom');
      final estado = await hablar(crear());
      expect((estado as Fallo).mensaje, isNot(contains('boom')));
    });
  });

  group('grabación', () {
    test('si se suelta antes de que arranque el micrófono, igual termina y procesa', () async {
      voz.arranque = Completer<void>();
      accion(NombreAccion.verHorasDisponibles);
      final c = crear();
      final n = c.read(asistenteProvider.notifier);

      final arrancando = n.empezarAEscuchar();
      await Future<void>.delayed(Duration.zero);
      await n.terminarDeEscuchar();
      expect(voz.terminadas, 0, reason: 'todavía está arrancando');

      voz.arranque!.complete();
      await arrancando;

      expect(voz.terminadas, 1);
      expect(c.read(asistenteProvider), isA<Resultado>());
    });

    test('pulsar dos veces seguidas no abre dos grabaciones', () async {
      final c = crear();
      final n = c.read(asistenteProvider.notifier);

      await n.empezarAEscuchar();
      await n.empezarAEscuchar();

      expect(voz.empezadas, 1);
    });

    test('cerrar mientras escucha descarta el audio', () async {
      final c = crear();
      final n = c.read(asistenteProvider.notifier);
      await n.empezarAEscuchar();

      await n.cerrar();

      expect(voz.canceladas, 1);
      expect(c.read(asistenteProvider), isA<Inactivo>());
    });

    test('soltar sin haber pulsado no hace nada', () async {
      final c = crear();

      await c.read(asistenteProvider.notifier).terminarDeEscuchar();

      expect(voz.terminadas, 0);
      expect(c.read(asistenteProvider), isA<Inactivo>());
    });

    test('después de un resultado se puede volver a hablar', () async {
      accion(NombreAccion.verHorasDisponibles);
      final c = crear();
      await hablar(c);

      final estado = await hablar(c);

      expect(estado, isA<Resultado>());
      expect(voz.empezadas, 2);
    });
  });
}
