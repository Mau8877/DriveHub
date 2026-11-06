import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/models/accion_asistente.dart';
import 'package:mobile/features/asistente/models/nombre_accion.dart';
import 'package:mobile/features/asistente/models/preparacion_de_cambio.dart';
import 'package:mobile/features/asistente/services/preparador_de_cambios.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/reserva.dart';

import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  // Martes 4 de noviembre de 2025, 15:30
  final hoy = DateTime(2025, 11, 4, 15, 30);

  late ClasesServiceFalso servicio;

  setUp(() => servicio = ClasesServiceFalso());

  PreparadorDeCambios crear() {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio), pagosServiceFalso(PagosServiceFalso())]);
    addTearDown(container.dispose);
    final probe = Provider<PreparadorDeCambios>((ref) => PreparadorDeCambios(ref, ahora: () => hoy));
    return container.read(probe);
  }

  AccionAsistente cancelar({String? dia, String? hora, String? instructor}) =>
      AccionAsistente(accion: NombreAccion.cancelarClase, dia: dia, hora: hora, instructor: instructor);

  AccionAsistente reprogramar({String? dia, String? hora, String? instructor}) =>
      AccionAsistente(accion: NombreAccion.reprogramarClase, dia: dia, hora: hora, instructor: instructor);

  Reserva clase(
    String codigo,
    DateTime fecha,
    String hora, {
    String instructorCodigo = 'i1',
    String instructorNombre = 'Lucía Rojas',
    bool aTiempo = true,
    EstadoReserva estado = EstadoReserva.confirmada,
    bool terminada = false,
  }) => Reserva(
    codigo: codigo,
    fecha: fecha,
    horaInicio: hora,
    horaFin: '${(int.parse(hora.substring(0, 2)) + 1).toString().padLeft(2, '0')}:00',
    instructorCodigo: instructorCodigo,
    instructorNombre: instructorNombre,
    estado: estado,
    terminada: terminada,
    cancelacionATiempo: aTiempo,
  );

  final viernes = DateTime(2025, 11, 7);
  final sabado = DateTime(2025, 11, 8);

  PropuestaDeCambio propuesta(PreparacionDeCambio r) => r as PropuestaDeCambio;

  group('qué clase se identifica', () {
    test('sin datos apunta a la próxima clase, aunque la lista venga desordenada', () async {
      servicio.proximas = [clase('lejana', sabado, '09:00'), clase('proxima', viernes, '10:00')];

      final r = await crear().preparar(cancelar());

      expect(propuesta(r).reserva.codigo, 'proxima');
      expect(propuesta(r).tipo, TipoCambio.cancelar);
    });

    test('con un día apunta a la clase de ese día', () async {
      servicio.proximas = [clase('a', viernes, '10:00'), clase('b', sabado, '09:00')];

      final r = await crear().preparar(cancelar(dia: 'sabado'));

      expect(propuesta(r).reserva.codigo, 'b');
    });

    test('con día y hora', () async {
      servicio.proximas = [clase('a', viernes, '10:00'), clase('b', viernes, '15:00')];

      final r = await crear().preparar(cancelar(dia: 'viernes', hora: '15:00'));

      expect(propuesta(r).reserva.codigo, 'b');
    });

    test('con un instructor, por nombre parcial', () async {
      servicio.proximas = [
        clase('a', viernes, '10:00'),
        clase('b', sabado, '09:00', instructorCodigo: 'i2', instructorNombre: 'Carlos Quispe'),
      ];

      final r = await crear().preparar(cancelar(instructor: 'el profe Carlos'));

      expect(propuesta(r).reserva.codigo, 'b');
    });

    test('si cuadran varias, la persona elige entre ellas', () async {
      servicio.proximas = [clase('a', viernes, '10:00'), clase('b', viernes, '15:00'), clase('c', sabado, '09:00')];

      final r = await crear().preparar(cancelar(dia: 'viernes'));

      expect(r, isA<VariasPosibles>());
      expect((r as VariasPosibles).candidatas.map((c) => c.codigo), ['a', 'b']);
    });

    test('las clases que ya empezaron, canceladas o terminadas no se ofrecen', () async {
      servicio.proximas = [
        clase('empezo', DateTime(2025, 11, 4), '15:00'),
        clase('cancelada', viernes, '08:00', estado: EstadoReserva.cancelada),
        clase('terminada', viernes, '09:00', terminada: true),
        clase('vale', viernes, '10:00'),
      ];

      final r = await crear().preparar(cancelar());

      expect(propuesta(r).reserva.codigo, 'vale');
    });
  });

  group('cuando no se puede', () {
    test('sin clases lo dice', () async {
      final r = await crear().preparar(cancelar());
      expect((r as NadaQueCambiar).mensaje, 'No tienes clases por venir.');
    });

    test('ninguna cuadra con lo dicho', () async {
      servicio.proximas = [clase('a', viernes, '10:00')];
      final r = await crear().preparar(cancelar(dia: 'sabado'));
      expect(r, isA<NadaQueCambiar>());
    });

    test('un instructor sin clases tuyas no se inventa', () async {
      servicio.proximas = [clase('a', viernes, '10:00')];
      final r = await crear().preparar(cancelar(instructor: 'Roberto'));
      expect((r as NadaQueCambiar).mensaje, contains('Roberto'));
    });

    test('sin conexión da un aviso y no un error', () async {
      servicio.errorReservas = Exception('sin red');
      final r = await crear().preparar(cancelar());
      expect((r as NadaQueCambiar).mensaje, contains('conexión'));
    });

    test('lo que no es un cambio no se prepara', () {
      expect(
        () => crear().preparar(const AccionAsistente(accion: NombreAccion.verMisClases)),
        throwsArgumentError,
      );
    });
  });

  group('reglas de CU15', () {
    test('cancelar a tiempo avisa que se devuelve la hora', () async {
      servicio.proximas = [clase('a', viernes, '10:00')];
      final r = await crear().preparar(cancelar());
      expect(
        propuesta(r).resumen,
        'Cancelar tu clase del viernes 7/11 a las 10:00 con Lucía Rojas. Se te devolverá 1 hora.',
      );
    });

    test('cancelar fuera de plazo avisa que se pierde la hora, pero se puede', () async {
      servicio.proximas = [clase('a', viernes, '10:00', aTiempo: false)];
      final r = await crear().preparar(cancelar());
      expect(propuesta(r).resumen, contains('perderás la hora'));
    });

    test('reprogramar a tiempo propone mover la clase', () async {
      servicio.proximas = [clase('a', viernes, '10:00')];
      final r = await crear().preparar(reprogramar());
      expect(propuesta(r).tipo, TipoCambio.reprogramar);
      expect(propuesta(r).resumen, startsWith('Mover tu clase'));
    });

    test('reprogramar fuera de plazo no se ofrece y sugiere cancelar', () async {
      servicio.proximas = [clase('a', viernes, '10:00', aTiempo: false)];
      final r = await crear().preparar(reprogramar());
      expect((r as NadaQueCambiar).mensaje, allOf(contains('No se puede reprogramar'), contains('cancelarla')));
    });

    test('mañana y hoy se dicen así', () async {
      servicio.proximas = [clase('a', DateTime(2025, 11, 5), '10:00')];
      final r = await crear().preparar(cancelar());
      expect(propuesta(r).resumen, contains('tu clase de mañana a las'));
    });
  });

  test('preparar NO cambia nada: no cancela ni reprograma en el servidor', () async {
    servicio.proximas = [clase('a', viernes, '10:00')];
    final preparador = crear();

    await preparador.preparar(cancelar());
    await preparador.preparar(reprogramar());

    expect(servicio.cancelacionesPedidas, isEmpty);
    expect(servicio.reprogramacionesPedidas, isEmpty);
    expect(servicio.reservasPedidas, isEmpty);
  });

  test('elegir entre varias da la propuesta de esa clase', () async {
    final elegida = clase('b', viernes, '15:00');
    final r = crear().proponer(TipoCambio.cancelar, elegida);
    expect(propuesta(r).reserva, same(elegida));
  });
}
