import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/horarios/models/crear_plantilla_horario_request.dart';
import 'package:mobile/features/horarios/models/dia_bloqueado.dart';
import 'package:mobile/features/horarios/models/estado_plantilla.dart';
import 'package:mobile/features/horarios/models/fechas.dart';
import 'package:mobile/features/horarios/models/franja.dart';
import 'package:mobile/features/horarios/models/plantilla_horario.dart';
import 'package:mobile/features/horarios/models/plantilla_horario_creada.dart';

Map<String, dynamic> _horarioJson({
  String estado = 'ACTIVA',
  List<int> dias = const [1, 3, 5],
  String? vigenteHasta = '2026-12-01',
}) => {
  'codigo': 'h1',
  'nombre': 'Mañanas',
  'diasSemana': dias,
  'horaInicio': '08:00',
  'horaFin': '12:00',
  'duracionClaseMin': 60,
  'vigenteDesde': '2026-10-26',
  'vigenteHasta': vigenteHasta,
  'estado': estado,
};

void main() {
  group('EstadoPlantilla', () {
    test('lee los tres estados de la API', () {
      expect(EstadoPlantilla.fromJson('ACTIVA'), EstadoPlantilla.activa);
      expect(EstadoPlantilla.fromJson('PAUSADA'), EstadoPlantilla.pausada);
      expect(EstadoPlantilla.fromJson('INACTIVA'), EstadoPlantilla.inactiva);
    });

    test('un valor desconocido lanza FormatException', () {
      expect(() => EstadoPlantilla.fromJson('ARCHIVADA'), throwsFormatException);
    });

    test('valorApi es el nombre que viaja en la API', () {
      expect(EstadoPlantilla.pausada.valorApi, 'PAUSADA');
    });
  });

  group('PlantillaHorario.fromJson', () {
    test('parsea todos los campos, con los días como lista y la vigencia como fecha', () {
      final horario = PlantillaHorario.fromJson(_horarioJson());

      expect(horario.codigo, 'h1');
      expect(horario.nombre, 'Mañanas');
      expect(horario.diasSemana, [1, 3, 5]);
      expect(horario.horaInicio, '08:00');
      expect(horario.horaFin, '12:00');
      expect(horario.duracionClaseMin, 60);
      expect(horario.vigenteDesde, DateTime(2026, 10, 26));
      expect(horario.vigenteHasta, DateTime(2026, 12, 1));
      expect(horario.estado, EstadoPlantilla.activa);
      expect(horario.activa, isTrue);
    });

    test('sin vigenteHasta queda nulo y un estado pausado no es activo', () {
      final horario = PlantillaHorario.fromJson(_horarioJson(estado: 'PAUSADA', vigenteHasta: null));

      expect(horario.vigenteHasta, isNull);
      expect(horario.estado, EstadoPlantilla.pausada);
      expect(horario.activa, isFalse);
    });

    test('un estado desconocido lanza FormatException', () {
      expect(() => PlantillaHorario.fromJson(_horarioJson(estado: 'X')), throwsFormatException);
    });

    test('calcula las franjas por día y por semana', () {
      final horario = PlantillaHorario.fromJson(_horarioJson());

      // * 08:00 a 12:00 con clases de 60 min = 4 por día, en 3 días
      expect(horario.franjasPorDia, 4);
      expect(horario.franjasPorSemana, 12);
    });
  });

  test('PlantillaHorarioCreada.fromJson toma el horario y franjasGeneradas del mismo nivel', () {
    final creada = PlantillaHorarioCreada.fromJson({..._horarioJson(), 'franjasGeneradas': 46});

    expect(creada.franjasGeneradas, 46);
    expect(creada.horario.codigo, 'h1');
    expect(creada.horario.diasSemana, [1, 3, 5]);
  });

  group('CrearPlantillaHorarioRequest.toJson', () {
    test('sin vigencias no envía los campos nulos y manda los días como lista', () {
      const request = CrearPlantillaHorarioRequest(
        nombre: 'Mañanas',
        diasSemana: [1, 2, 3],
        horaInicio: '08:00',
        horaFin: '12:00',
        duracionClaseMin: 60,
      );

      expect(request.toJson(), {
        'nombre': 'Mañanas',
        'diasSemana': [1, 2, 3],
        'horaInicio': '08:00',
        'horaFin': '12:00',
        'duracionClaseMin': 60,
      });
    });

    test('con vigencias las envía como yyyy-MM-dd', () {
      final request = CrearPlantillaHorarioRequest(
        nombre: 'Sábados',
        diasSemana: const [6],
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

  test('DiaBloqueado.fromJson parsea la fecha y la cantidad de franjas', () {
    final dia = DiaBloqueado.fromJson({'fecha': '2026-11-02', 'franjasAfectadas': 4});

    expect(dia.fecha, DateTime(2026, 11, 2));
    expect(dia.franjasAfectadas, 4);
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

    Franja franja(EstadoFranja estado, String hora, {DateTime? fecha}) =>
        Franja(codigo: 'f', fecha: fecha ?? DateTime(2026, 11, 2), horaInicio: hora, horaFin: '23:59', estado: estado);

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
      expect(franja(EstadoFranja.libre, '09:30').accionDisponible(ahora), isNull);
    });

    test('una de un día pasado no ofrece acción', () {
      expect(franja(EstadoFranja.libre, '23:00', fecha: DateTime(2026, 11, 1)).accionDisponible(ahora), isNull);
    });
  });

  test('ResumenFranjas cuenta cuántas hay de cada estado', () {
    Franja franja(EstadoFranja estado) =>
        Franja(codigo: 'f', fecha: DateTime(2026, 11, 2), horaInicio: '10:00', horaFin: '11:00', estado: estado);

    final resumen = ResumenFranjas.de([
      franja(EstadoFranja.libre),
      franja(EstadoFranja.libre),
      franja(EstadoFranja.reservada),
      franja(EstadoFranja.bloqueada),
      franja(EstadoFranja.bloqueada),
      franja(EstadoFranja.bloqueada),
    ]);

    expect(resumen.libres, 2);
    expect(resumen.reservadas, 1);
    expect(resumen.bloqueadas, 3);
  });

  group('fechas', () {
    test('lunesDeLaSemana devuelve el lunes de cualquier día de esa semana', () {
      expect(lunesDeLaSemana(DateTime(2026, 10, 26)), DateTime(2026, 10, 26));
      expect(lunesDeLaSemana(DateTime(2026, 10, 28, 15, 40)), DateTime(2026, 10, 26));
      expect(lunesDeLaSemana(DateTime(2026, 11, 1)), DateTime(2026, 10, 26));
      expect(lunesDeLaSemana(DateTime(2026, 11, 2)), DateTime(2026, 11, 2));
    });

    test('sumarDias cruza el mes y el año sin arrastrar la hora', () {
      expect(sumarDias(DateTime(2026, 10, 26), 7), DateTime(2026, 11, 2));
      expect(sumarDias(DateTime(2026, 12, 28), 7), DateTime(2027, 1, 4));
      expect(sumarDias(DateTime(2026, 11, 2), -7), DateTime(2026, 10, 26));
    });

    test('etiquetaDeSemana muestra el rango, con un solo mes o con dos', () {
      expect(etiquetaDeSemana(DateTime(2026, 11, 2)), '2 – 8 nov');
      expect(etiquetaDeSemana(DateTime(2026, 10, 26)), '26 oct – 1 nov');
    });

    test('letras y nombres de los días', () {
      expect([for (var d = 1; d <= 7; d++) letraDeDia(d)].join(), 'LMXJVSD');
      expect(nombreDeDiaCapitalizado(3), 'Miércoles');
      expect(diaConNumero(DateTime(2026, 10, 26)), 'Lunes 26');
    });

    test('franjasDeLaVentana cuenta las clases que caben y da cero con datos inválidos', () {
      expect(franjasDeLaVentana('08:00', '12:00', 60), 4);
      expect(franjasDeLaVentana('08:00', '12:00', 90), 2);
      expect(franjasDeLaVentana('12:00', '08:00', 60), 0);
      expect(franjasDeLaVentana('', '12:00', 60), 0);
      expect(franjasDeLaVentana('08:00', '12:00', 0), 0);
      expect(franjasDeLaVentana('08:00', '12:00', null), 0);
    });
  });
}
