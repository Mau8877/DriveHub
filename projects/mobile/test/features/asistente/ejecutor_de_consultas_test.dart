import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/models/accion_asistente.dart';
import 'package:mobile/features/asistente/models/nombre_accion.dart';
import 'package:mobile/features/asistente/services/ejecutor_de_consultas.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/reserva.dart';
import 'package:mobile/features/clases/providers/clases_pestana_provider.dart';
import 'package:mobile/features/clases/providers/franjas_disponibles_provider.dart';
import 'package:mobile/features/clases/providers/franjas_seleccionadas_provider.dart';

import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  // Martes 4 de noviembre de 2025, 15:30
  final hoy = DateTime(2025, 11, 4, 15, 30);

  late ClasesServiceFalso servicio;
  late PagosServiceFalso pagos;

  setUp(() {
    servicio = ClasesServiceFalso();
    pagos = PagosServiceFalso()..saldo = 5;
  });

  /// El ejecutor sobre un contenedor con los servicios falsos y el reloj fijo.
  (ProviderContainer, EjecutorDeConsultas) crear() {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio), pagosServiceFalso(pagos)]);
    addTearDown(container.dispose);
    final probe = Provider<EjecutorDeConsultas>((ref) => EjecutorDeConsultas(ref, ahora: () => hoy));
    return (container, container.read(probe));
  }

  AccionAsistente accion(NombreAccion a, {String? dia, String? momento, String? instructor, String? hora}) =>
      AccionAsistente(accion: a, dia: dia, momento: momento, instructor: instructor, hora: hora);

  group('ver_mis_clases y ver_historial', () {
    test('abren la pestaña Mis clases y avisan si no hay clases', () async {
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verMisClases));

      expect(c.read(clasesPestanaUiProvider), PestanaClases.misClases);
      expect(r.exito, isTrue);
      expect(r.mensaje, contains('Todavía no tienes clases'));
    });

    test('sin filtros dice que son sus próximas clases', () async {
      servicio.proximas = [reservaDePrueba()];
      final (_, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verMisClases));

      expect(r.mensaje, 'Estas son tus próximas clases.');
    });

    test('con un día responde con las clases de ese día', () async {
      servicio.proximas = [
        reservaDePrueba(codigo: 'r1', fecha: DateTime(2025, 11, 7), horaInicio: '10:00', instructorNombre: 'Lucía Rojas'),
        reservaDePrueba(codigo: 'r2', fecha: DateTime(2025, 11, 8), horaInicio: '09:00'),
      ];
      final (_, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verMisClases, dia: 'viernes'));

      expect(r.exito, isTrue);
      expect(r.mensaje, 'Tienes 1 clase el viernes 7/11: el viernes 7/11 10:00 con Lucía Rojas.');
    });

    test('con un día sin clases lo dice', () async {
      servicio.proximas = [reservaDePrueba(fecha: DateTime(2025, 11, 8))];
      final (_, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verMisClases, dia: 'manana'));

      expect(r.mensaje, 'No tienes clases mañana.');
    });

    test('con un instructor filtra por él, por nombre parcial', () async {
      servicio.proximas = [
        reservaDePrueba(codigo: 'r1', instructorNombre: 'Lucía Rojas'),
        Reserva(
          codigo: 'r2',
          fecha: DateTime(2099, 11, 2),
          horaInicio: '11:00',
          horaFin: '12:00',
          instructorCodigo: 'i2',
          instructorNombre: 'Carlos Quispe',
          estado: EstadoReserva.confirmada,
          terminada: false,
        ),
      ];
      final (_, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verMisClases, instructor: 'la profe Lucia'));

      expect(r.mensaje, startsWith('Tienes 1 clase con Lucía Rojas'));
    });

    test('un instructor con el que no tiene clases se avisa', () async {
      servicio.proximas = [reservaDePrueba()];
      final (_, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verMisClases, instructor: 'Roberto'));

      expect(r.exito, isFalse);
      expect(r.mensaje, contains('Roberto'));
    });

    test('ver_historial abre la pestaña Historial', () async {
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verHistorial));

      expect(c.read(clasesPestanaUiProvider), PestanaClases.historial);
      expect(r.exito, isTrue);
    });

    test('sin conexión da un aviso y no un error', () async {
      servicio.errorReservas = Exception('sin red');
      final (_, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verMisClases));

      expect(r.exito, isFalse);
      expect(r.mensaje, contains('conexión'));
    });
  });

  group('buscar_horarios', () {
    test('aplica día, momento e instructor y abre Reservar', () async {
      servicio.instructores = [instructorDePrueba(codigo: 'i9', nombre: 'Lucía Rojas')];
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(
        accion(NombreAccion.buscarHorarios, dia: 'viernes', momento: 'tarde', instructor: 'Lucía'),
      );

      expect(r.exito, isTrue);
      expect(c.read(clasesPestanaUiProvider), PestanaClases.reservar);
      expect(c.read(reservarFechaUiProvider), DateTime(2025, 11, 7));
      expect(c.read(reservarMomentoUiProvider), MomentoDia.tarde);
      expect(c.read(reservarInstructorUiProvider), 'i9');
      expect(r.mensaje, 'Mostrando horarios libres el viernes 7/11, por la tarde, con Lucía Rojas.');
      expect(servicio.pedidosDeInstructores.single, DateTime(2025, 11, 7));
    });

    test('solo con el momento deja lo demás como estaba', () async {
      final (c, ejecutor) = crear();
      c.read(reservarInstructorUiProvider.notifier).elegir('i1');

      await ejecutor.ejecutar(accion(NombreAccion.buscarHorarios, momento: 'noche'));

      expect(c.read(reservarMomentoUiProvider), MomentoDia.noche);
      expect(c.read(reservarInstructorUiProvider), 'i1');
    });

    test('un instructor que no existe no cambia nada y da las opciones', () async {
      servicio.instructores = [instructorDePrueba(nombre: 'Lucía Rojas')];
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(
        accion(NombreAccion.buscarHorarios, dia: 'viernes', momento: 'tarde', instructor: 'Roberto'),
      );

      expect(r.exito, isFalse);
      expect(r.mensaje, 'No encontré a "Roberto". Hay horarios con Lucía Rojas.');
      expect(c.read(clasesPestanaUiProvider), isNull);
      expect(c.read(reservarFechaUiProvider), isNot(DateTime(2025, 11, 7)));
      expect(c.read(reservarMomentoUiProvider), MomentoDia.todos);
    });

    test('un instructor ambiguo pide elegir', () async {
      servicio.instructores = [
        instructorDePrueba(codigo: 'i1', nombre: 'Carlos Quispe'),
        instructorDePrueba(codigo: 'i2', nombre: 'Carlos Mamani'),
      ];
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.buscarHorarios, instructor: 'Carlos'));

      expect(r.exito, isFalse);
      expect(r.mensaje, contains('Carlos Quispe, Carlos Mamani'));
      expect(c.read(reservarInstructorUiProvider), isNull);
    });

    test('sin conexión al buscar al instructor da un aviso y no cambia nada', () async {
      servicio.errorInstructores = Exception('sin red');
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.buscarHorarios, momento: 'tarde', instructor: 'Lucía'));

      expect(r.exito, isFalse);
      expect(c.read(reservarMomentoUiProvider), MomentoDia.todos);
    });
  });

  group('seleccionar_horario', () {
    test('marca la franja de esa hora sin reservar', () async {
      servicio.franjas = [
        franjaDisponibleDePrueba(codigo: 'f10', horaInicio: '10:00'),
        franjaDisponibleDePrueba(codigo: 'f11', horaInicio: '11:00'),
      ];
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.seleccionarHorario, hora: '11:00'));

      expect(r.exito, isTrue);
      expect(c.read(franjasSeleccionadasUiProvider).keys, ['f11']);
      expect(c.read(clasesPestanaUiProvider), PestanaClases.reservar);
      expect(servicio.reservasPedidas, isEmpty);
      expect(r.mensaje, contains('Confirmar'));
    });

    test('sin horario a esa hora lo dice y no marca nada', () async {
      servicio.franjas = [franjaDisponibleDePrueba(horaInicio: '10:00')];
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.seleccionarHorario, hora: '16:00'));

      expect(r.exito, isFalse);
      expect(r.mensaje, contains('16:00'));
      expect(c.read(franjasSeleccionadasUiProvider), isEmpty);
    });

    test('con varios instructores a esa hora pide cuál', () async {
      servicio.franjas = [
        franjaDisponibleDePrueba(codigo: 'a', horaInicio: '10:00', instructorNombre: 'Lucía Rojas'),
        franjaDisponibleDePrueba(codigo: 'b', horaInicio: '10:00', instructorCodigo: 'i2', instructorNombre: 'Carlos Quispe'),
      ];
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.seleccionarHorario, hora: '10:00'));

      expect(r.exito, isFalse);
      expect(r.mensaje, contains('Lucía Rojas, Carlos Quispe'));
      expect(c.read(franjasSeleccionadasUiProvider), isEmpty);
    });

    test('sin horas disponibles no marca', () async {
      pagos.saldo = 0;
      servicio.franjas = [franjaDisponibleDePrueba(horaInicio: '10:00')];
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.seleccionarHorario, hora: '10:00'));

      expect(r.exito, isFalse);
      expect(r.mensaje, 'No tienes horas disponibles para reservar.');
      expect(c.read(franjasSeleccionadasUiProvider), isEmpty);
    });

    test('pedirlo dos veces no lo desmarca', () async {
      servicio.franjas = [franjaDisponibleDePrueba(codigo: 'f10', horaInicio: '10:00')];
      final (c, ejecutor) = crear();

      await ejecutor.ejecutar(accion(NombreAccion.seleccionarHorario, hora: '10:00'));
      final r = await ejecutor.ejecutar(accion(NombreAccion.seleccionarHorario, hora: '10:00'));

      expect(c.read(franjasSeleccionadasUiProvider).keys, ['f10']);
      expect(r.mensaje, contains('ya está marcado'));
    });

    test('con un instructor inexistente no marca nada', () async {
      servicio.franjas = [franjaDisponibleDePrueba(horaInicio: '10:00')];
      servicio.instructores = [instructorDePrueba(nombre: 'Ivan Alfa')];
      final (c, ejecutor) = crear();

      final r = await ejecutor.ejecutar(
        accion(NombreAccion.seleccionarHorario, hora: '10:00', instructor: 'Roberto'),
      );

      expect(r.exito, isFalse);
      expect(c.read(franjasSeleccionadasUiProvider), isEmpty);
    });
  });

  group('limpiar_filtros y ver_horas_disponibles', () {
    test('limpiar_filtros vuelve momento e instructor a todos', () async {
      final (c, ejecutor) = crear();
      c.read(reservarMomentoUiProvider.notifier).elegir(MomentoDia.tarde);
      c.read(reservarInstructorUiProvider.notifier).elegir('i1');

      final r = await ejecutor.ejecutar(accion(NombreAccion.limpiarFiltros));

      expect(r.exito, isTrue);
      expect(c.read(reservarMomentoUiProvider), MomentoDia.todos);
      expect(c.read(reservarInstructorUiProvider), isNull);
      expect(c.read(clasesPestanaUiProvider), PestanaClases.reservar);
    });

    test('ver_horas_disponibles dice el saldo', () async {
      pagos.saldo = 5;
      final (_, ejecutor) = crear();
      expect((await ejecutor.ejecutar(accion(NombreAccion.verHorasDisponibles))).mensaje, 'Te quedan 5 horas.');
    });

    test('con una hora usa el singular y sin horas lo dice', () async {
      pagos.saldo = 1;
      final (_, uno) = crear();
      expect((await uno.ejecutar(accion(NombreAccion.verHorasDisponibles))).mensaje, 'Te quedan 1 hora.');

      pagos.saldo = 0;
      final (_, cero) = crear();
      expect((await cero.ejecutar(accion(NombreAccion.verHorasDisponibles))).mensaje, 'No tienes horas disponibles.');
    });

    test('sin conexión al ver las horas da un aviso', () async {
      pagos.errorSaldo = Exception('sin red');
      final (_, ejecutor) = crear();

      final r = await ejecutor.ejecutar(accion(NombreAccion.verHorasDisponibles));

      expect(r.exito, isFalse);
    });
  });

  group('lo que no es una consulta', () {
    test('cancelar, reprogramar y fuera_de_alcance no se ejecutan acá', () async {
      final (_, ejecutor) = crear();
      for (final a in [NombreAccion.cancelarClase, NombreAccion.reprogramarClase, NombreAccion.fueraDeAlcance]) {
        expect(() => ejecutor.ejecutar(accion(a)), throwsArgumentError, reason: a.nombre);
      }
    });
  });
}
