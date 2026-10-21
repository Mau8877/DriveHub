import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/horarios/models/crear_plantilla_horario_request.dart';
import 'package:mobile/features/horarios/models/fechas.dart';
import 'package:mobile/features/horarios/models/franja.dart';
import 'package:mobile/features/horarios/models/pagina.dart';
import 'package:mobile/features/horarios/models/plantilla_horario.dart';
import 'package:mobile/features/horarios/models/plantilla_horario_creada.dart';

void main() {
  group('PlantillaHorario.fromJson', () {
    test('parsea todos los campos, con la vigencia como fecha', () {
      final horario = PlantillaHorario.fromJson({
        'codigo': 'h1',
        'diaSemana': 3,
        'horaInicio': '08:00',
        'horaFin': '12:00',
        'duracionClaseMin': 60,
        'vigenteDesde': '2026-10-26',
        'vigenteHasta': '2026-12-01',
        'activa': true,
      });

      expect(horario.codigo, 'h1');
      expect(horario.diaSemana, 3);
      expect(horario.horaInicio, '08:00');
      expect(horario.horaFin, '12:00');
      expect(horario.duracionClaseMin, 60);
      expect(horario.vigenteDesde, DateTime(2026, 10, 26));
      expect(horario.vigenteHasta, DateTime(2026, 12, 1));
      expect(horario.activa, isTrue);
    });

    test('sin vigenteHasta queda nulo', () {
      final horario = PlantillaHorario.fromJson({
        'codigo': 'h1',
        'diaSemana': 1,
        'horaInicio': '08:00',
        'horaFin': '12:00',
        'duracionClaseMin': 60,
        'vigenteDesde': '2026-10-26',
        'vigenteHasta': null,
        'activa': false,
      });

      expect(horario.vigenteHasta, isNull);
      expect(horario.activa, isFalse);
    });
  });

  test('PlantillaHorarioCreada.fromJson toma el horario y franjasGeneradas del mismo nivel', () {
    final creada = PlantillaHorarioCreada.fromJson({
      'codigo': 'h1',
      'diaSemana': 1,
      'horaInicio': '08:00',
      'horaFin': '12:00',
      'duracionClaseMin': 60,
      'vigenteDesde': '2026-10-26',
      'activa': true,
      'franjasGeneradas': 14,
    });

    expect(creada.franjasGeneradas, 14);
    expect(creada.horario.codigo, 'h1');
    expect(creada.horario.horaFin, '12:00');
  });

  group('CrearPlantillaHorarioRequest.toJson', () {
    test('sin vigencias no envía los campos nulos', () {
      const request = CrearPlantillaHorarioRequest(
        diaSemana: 1,
        horaInicio: '08:00',
        horaFin: '12:00',
        duracionClaseMin: 60,
      );

      expect(request.toJson(), {
        'diaSemana': 1,
        'horaInicio': '08:00',
        'horaFin': '12:00',
        'duracionClaseMin': 60,
      });
    });

    test('con vigencias las envía como yyyy-MM-dd', () {
      final request = CrearPlantillaHorarioRequest(
        diaSemana: 2,
        horaInicio: '14:00',
        horaFin: '18:00',
        duracionClaseMin: 90,
        vigenteDesde: DateTime(2026, 11, 2),
        vigenteHasta: DateTime(2027, 1, 5),
      );

      final json = request.toJson();

      expect(json['vigenteDesde'], '2026-11-02');
      expect(json['vigenteHasta'], '2027-01-05');
    });
  });

  group('EstadoFranja', () {
    test('lee los tres estados de la API', () {
      expect(EstadoFranja.fromJson('LIBRE'), EstadoFranja.libre);
      expect(EstadoFranja.fromJson('RESERVADA'), EstadoFranja.reservada);
      expect(EstadoFranja.fromJson('BLOQUEADA'), EstadoFranja.bloqueada);
    });

    test('un valor desconocido lanza FormatException', () {
      expect(() => EstadoFranja.fromJson('EXPIRADA'), throwsFormatException);
    });

    test('valorApi es el nombre que viaja en la API', () {
      expect(EstadoFranja.bloqueada.valorApi, 'BLOQUEADA');
    });
  });

  test('Franja.fromJson parsea la fecha, las horas y el estado', () {
    final franja = Franja.fromJson({
      'codigo': 'f1',
      'fecha': '2026-11-02',
      'horaInicio': '10:00',
      'horaFin': '11:00',
      'estado': 'BLOQUEADA',
    });

    expect(franja.codigo, 'f1');
    expect(franja.fecha, DateTime(2026, 11, 2));
    expect(franja.horaInicio, '10:00');
    expect(franja.estado, EstadoFranja.bloqueada);
  });

  group('Pagina.fromJson', () {
    test('arma los items con la función dada y copia el meta', () {
      final pagina = Pagina<Franja>.fromJson({
        'data': [
          {'codigo': 'f1', 'fecha': '2026-11-02', 'horaInicio': '08:00', 'horaFin': '09:00', 'estado': 'LIBRE'},
          {'codigo': 'f2', 'fecha': '2026-11-02', 'horaInicio': '09:00', 'horaFin': '10:00', 'estado': 'LIBRE'},
        ],
        'meta': {'page': 2, 'pageSize': 2, 'totalItems': 5, 'totalPages': 3},
      }, Franja.fromJson);

      expect(pagina.items.map((f) => f.codigo), ['f1', 'f2']);
      expect(pagina.page, 2);
      expect(pagina.pageSize, 2);
      expect(pagina.totalItems, 5);
      expect(pagina.totalPages, 3);
    });

    test('una lista vacía da una página sin items', () {
      final pagina = Pagina<Franja>.fromJson({
        'data': <dynamic>[],
        'meta': {'page': 1, 'pageSize': 20, 'totalItems': 0, 'totalPages': 0},
      }, Franja.fromJson);

      expect(pagina.items, isEmpty);
      expect(pagina.totalItems, 0);
    });
  });

  group('Franja.accionDisponible', () {
    final ahora = DateTime(2026, 11, 2, 9, 30);

    Franja franja(EstadoFranja estado, String hora, {DateTime? fecha}) => Franja(
      codigo: 'f',
      fecha: fecha ?? DateTime(2026, 11, 2),
      horaInicio: hora,
      horaFin: '23:59',
      estado: estado,
    );

    test('una libre futura se puede bloquear', () {
      expect(franja(EstadoFranja.libre, '10:00').accionDisponible(ahora), AccionFranja.bloquear);
    });

    test('una bloqueada futura se puede desbloquear', () {
      expect(franja(EstadoFranja.bloqueada, '10:00').accionDisponible(ahora), AccionFranja.desbloquear);
    });

    test('una reservada no ofrece acción', () {
      expect(franja(EstadoFranja.reservada, '10:00').accionDisponible(ahora), isNull);
    });

    test('una que ya empezó no ofrece acción, sea cual sea su estado', () {
      expect(franja(EstadoFranja.libre, '09:00').accionDisponible(ahora), isNull);
      expect(franja(EstadoFranja.bloqueada, '09:00').accionDisponible(ahora), isNull);
    });

    test('la que empieza exactamente ahora no ofrece acción', () {
      expect(franja(EstadoFranja.libre, '09:30').accionDisponible(ahora), isNull);
    });

    test('una franja de un día pasado no ofrece acción', () {
      expect(franja(EstadoFranja.libre, '23:00', fecha: DateTime(2026, 11, 1)).accionDisponible(ahora), isNull);
    });

    test('una de un día futuro la ofrece aunque su hora sea temprano', () {
      expect(
        franja(EstadoFranja.libre, '06:00', fecha: DateTime(2026, 11, 3)).accionDisponible(ahora),
        AccionFranja.bloquear,
      );
    });
  });

  group('fechas', () {
    test('nombreDeDia va de lunes (1) a domingo (7)', () {
      expect(nombreDeDia(1), 'lunes');
      expect(nombreDeDia(3), 'miércoles');
      expect(nombreDeDia(7), 'domingo');
    });

    test('fechaParaApi y fechaDesdeApi son inversas', () {
      expect(fechaParaApi(DateTime(2026, 1, 5)), '2026-01-05');
      expect(fechaDesdeApi('2026-01-05'), DateTime(2026, 1, 5));
    });

    test('fechaParaMostrar usa dd/MM/yyyy', () {
      expect(fechaParaMostrar(DateTime(2026, 1, 5)), '05/01/2026');
    });

    test('horaParaApi rellena con ceros', () {
      expect(horaParaApi(8, 5), '08:05');
      expect(horaParaApi(23, 59), '23:59');
    });

    test('soloFecha descarta la hora', () {
      expect(soloFecha(DateTime(2026, 11, 2, 15, 45)), DateTime(2026, 11, 2));
    });
  });
}
