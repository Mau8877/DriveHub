import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/models/catalogo_acciones.dart';
import 'package:mobile/features/asistente/models/nombre_accion.dart';
import 'package:mobile/features/asistente/models/resultado_interpretacion.dart';
import 'package:mobile/features/asistente/services/modelo_de_acciones.dart';
import 'package:mobile/features/asistente/services/motor_litert_lm.dart';

/// Un motor que responde lo configurado y anota cada texto: nunca carga un `.litertlm`.
class MotorFalso implements MotorDeLlamadas {
  List<LlamadaAFuncion> respuesta = [];
  Object? error;
  final List<String> textos = [];
  bool liberado = false;

  @override
  Future<List<LlamadaAFuncion>> llamar(String texto) async {
    textos.add(texto);
    final e = error;
    if (e != null) throw e;
    return respuesta;
  }

  @override
  Future<void> liberar() async => liberado = true;
}

void main() {
  late CatalogoAcciones catalogo;
  late MotorFalso motor;
  late ModeloDeAccionesLitert modelo;

  setUpAll(() {
    catalogo = CatalogoAcciones.desdeJson(File(CatalogoAcciones.rutaAsset).readAsStringSync());
  });

  setUp(() {
    motor = MotorFalso();
    modelo = ModeloDeAccionesLitert(motor: motor, catalogo: catalogo);
  });

  group('ModeloDeAccionesLitert', () {
    test('le pasa el texto al motor y valida la acción que elige', () async {
      motor.respuesta = [const LlamadaAFuncion('ver_mis_clases', {'dia': 'viernes'})];

      final r = await modelo.entender('tengo práctica el viernes?');

      expect(motor.textos, ['tengo práctica el viernes?']);
      final accion = (r as AccionEntendida).accion;
      expect(accion.accion, NombreAccion.verMisClases);
      expect(accion.dia, 'viernes');
    });

    test('solo toma la primera llamada', () async {
      motor.respuesta = [
        const LlamadaAFuncion('ver_historial', {}),
        const LlamadaAFuncion('cancelar_clase', {}),
      ];

      final r = await modelo.entender('mi historial');

      expect((r as AccionEntendida).accion.accion, NombreAccion.verHistorial);
    });

    test('si el modelo contesta texto y no llama a ninguna función, no inventa una acción', () async {
      motor.respuesta = [];

      final r = await modelo.entender('hola');

      expect((r as AccionRechazada).motivo, MotivoRechazo.desconocida);
    });

    test('una función inventada o un argumento inválido se rechaza', () async {
      motor.respuesta = [const LlamadaAFuncion('borrar_todo', {})];
      expect(((await modelo.entender('x')) as AccionRechazada).motivo, MotivoRechazo.desconocida);

      motor.respuesta = [const LlamadaAFuncion('ver_mis_clases', {'dia': 'jueveees'})];
      expect(((await modelo.entender('x')) as AccionRechazada).motivo, MotivoRechazo.argumentoInvalido);
    });

    test('fuera_de_alcance llega como rechazo, no como acción', () async {
      motor.respuesta = [const LlamadaAFuncion('fuera_de_alcance', {})];

      final r = await modelo.entender('cuéntame un chiste');

      expect((r as AccionRechazada).motivo, MotivoRechazo.fueraDeAlcance);
    });

    test('un fallo del motor se propaga (la pantalla lo muestra)', () async {
      motor.error = const ModeloDeAccionesNoDisponible('sin modelo');

      expect(() => modelo.entender('x'), throwsA(isA<ModeloDeAccionesNoDisponible>()));
    });

    test('liberar libera el motor', () async {
      await modelo.liberar();
      expect(motor.liberado, isTrue);
    });
  });

  group('MotorLitertLm', () {
    test('sin el archivo del modelo avisa que no está disponible, sin tocar el plugin', () async {
      final ruta = '${Directory.systemTemp.path}${Platform.pathSeparator}no_existe_${DateTime.now().microsecondsSinceEpoch}.litertlm';
      final real = MotorLitertLm(rutaModelo: ruta, catalogo: catalogo);

      await expectLater(
        real.llamar('hola'),
        throwsA(isA<ModeloDeAccionesNoDisponible>().having((e) => e.motivo, 'motivo', contains(ruta))),
      );
    });

    test('liberar sin haber cargado nada no falla', () async {
      final real = MotorLitertLm(rutaModelo: 'x', catalogo: catalogo);
      await real.liberar();
    });
  });

  group('esquema de las herramientas', () {
    test('cada acción expone sus parámetros como JSON Schema', () {
      final buscar = catalogo.acciones['buscar_horarios']!.esquemaDeParametros();
      expect(buscar['type'], 'object');
      expect((buscar['properties'] as Map).keys, containsAll(['dia', 'momento', 'instructor']));
      expect(buscar['required'], isEmpty);
      expect(catalogo.acciones['seleccionar_horario']!.esquemaDeParametros()['required'], ['hora']);
    });
  });
}
