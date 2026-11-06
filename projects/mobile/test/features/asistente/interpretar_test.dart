import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/models/accion_asistente.dart';
import 'package:mobile/features/asistente/models/catalogo_acciones.dart';
import 'package:mobile/features/asistente/models/nombre_accion.dart';
import 'package:mobile/features/asistente/models/resultado_interpretacion.dart';
import 'package:mobile/features/asistente/services/interpretar_accion.dart';

void main() {
  late CatalogoAcciones catalogo;

  setUpAll(() {
    catalogo = CatalogoAcciones.desdeJson(File(CatalogoAcciones.rutaAsset).readAsStringSync());
  });

  AccionAsistente entendida(ResultadoInterpretacion r) => (r as AccionEntendida).accion;
  MotivoRechazo rechazo(ResultadoInterpretacion r) => (r as AccionRechazada).motivo;
  ResultadoInterpretacion interpretar(String nombre, Map<String, dynamic> args) =>
      interpretarAccion(nombre, args, catalogo);

  group('catálogo', () {
    test('tiene exactamente las acciones del enum', () {
      expect(catalogo.acciones.keys.toSet(), NombreAccion.values.map((a) => a.nombre).toSet());
    });

    test('cada acción se convierte en una herramienta con su esquema', () {
      final herramientas = catalogo.herramientas();
      expect(herramientas, hasLength(NombreAccion.values.length));
      final buscar = herramientas.firstWhere((h) => h['function']['name'] == 'buscar_horarios');
      expect(buscar['function']['parameters']['properties']['momento']['enum'], ['manana', 'tarde', 'noche']);
    });
  });

  group('acciones válidas', () {
    test('ver_mis_clases sin datos', () {
      expect(entendida(interpretar('ver_mis_clases', {})), const AccionAsistente(accion: NombreAccion.verMisClases));
    });

    test('ver_mis_clases con un día', () {
      expect(entendida(interpretar('ver_mis_clases', {'dia': 'viernes'})).dia, 'viernes');
    });

    test('buscar_horarios con día, momento e instructor', () {
      final a = entendida(interpretar('buscar_horarios', {'dia': 'manana', 'momento': 'tarde', 'instructor': 'Carlos'}));
      expect((a.dia, a.momento, a.instructor), ('manana', 'tarde', 'Carlos'));
    });

    test('seleccionar_horario con su hora', () {
      expect(entendida(interpretar('seleccionar_horario', {'hora': '15:00'})).hora, '15:00');
    });

    test('cancelar_clase y reprogramar_clase se entienden y cambian datos', () {
      expect(entendida(interpretar('cancelar_clase', {})).accion.cambiaDatos, isTrue);
      expect(entendida(interpretar('reprogramar_clase', {'hora': '09:30'})).accion.cambiaDatos, isTrue);
    });

    test('las consultas no cambian datos', () {
      const cambian = {NombreAccion.cancelarClase, NombreAccion.reprogramarClase};
      for (final a in NombreAccion.values.where((a) => !cambian.contains(a))) {
        expect(a.cambiaDatos, isFalse, reason: a.nombre);
      }
    });

    test('un argumento vacío se ignora', () {
      final a = entendida(interpretar('ver_mis_clases', {'dia': '  ', 'instructor': ''}));
      expect((a.dia, a.instructor), (null, null));
    });
  });

  group('rechazos', () {
    test('una función inventada es desconocida', () {
      expect(rechazo(interpretar('reservar_todo', {})), MotivoRechazo.desconocida);
    });

    test('fuera_de_alcance no es una acción ejecutable', () {
      expect(rechazo(interpretar('fuera_de_alcance', {})), MotivoRechazo.fueraDeAlcance);
    });

    test('seleccionar_horario sin hora falta un dato', () {
      expect(rechazo(interpretar('seleccionar_horario', {'dia': 'lunes'})), MotivoRechazo.faltaDato);
    });

    test('un día fuera del catálogo es un argumento inválido', () {
      expect(rechazo(interpretar('ver_mis_clases', {'dia': 'jueveees'})), MotivoRechazo.argumentoInvalido);
    });

    test('un argumento que esa acción no tiene es inválido', () {
      expect(rechazo(interpretar('ver_historial', {'dia': 'lunes'})), MotivoRechazo.argumentoInvalido);
    });

    test('un argumento que no es texto es inválido', () {
      expect(rechazo(interpretar('buscar_horarios', {'instructor': 5})), MotivoRechazo.argumentoInvalido);
    });

    test('una hora que no es HH:mm es inválida', () {
      expect(rechazo(interpretar('seleccionar_horario', {'hora': '3 pm'})), MotivoRechazo.argumentoInvalido);
      expect(rechazo(interpretar('cancelar_clase', {'hora': '25:00'})), MotivoRechazo.argumentoInvalido);
    });
  });
}
