import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/features/simulacros/models/intento_iniciado.dart';
import 'package:mobile/features/simulacros/models/intento_resultado.dart';
import 'package:mobile/features/simulacros/models/intento_resumen.dart';
import 'package:mobile/features/simulacros/models/simulacro_disponible.dart';
import 'package:mobile/features/simulacros/models/tipo_pregunta.dart';
import 'package:mobile/features/simulacros/providers/simulacros_service_provider.dart';
import 'package:mobile/features/simulacros/services/simulacros_service.dart';

SimulacroDisponible simulacroDePrueba({
  String codigo = 's1',
  String nombre = 'Simulacro básico',
}) => SimulacroDisponible(
  codigo: codigo,
  nombre: nombre,
  categoria: 'B',
  duracionMin: 20,
  nroPreguntas: 2,
  puntajeAprobacion: 70,
);

/// Un intento de dos preguntas: una de opción única (A es la correcta) y una de varias respuestas.
IntentoIniciado intentoDePrueba({String codigo = 'i1', int duracionMin = 20}) =>
    IntentoIniciado(
      codigo: codigo,
      simulacroNombre: 'Simulacro básico',
      duracionMin: duracionMin,
      preguntas: const [
        PreguntaParaRendir(
          codigo: 'p1',
          enunciado: '¿Qué indica el PARE?',
          tipo: TipoPregunta.opcionUnica,
          opciones: [
            OpcionParaRendir(id: 'A', texto: 'Detenerse'),
            OpcionParaRendir(id: 'B', texto: 'Seguir'),
          ],
        ),
        PreguntaParaRendir(
          codigo: 'p2',
          enunciado: '¿Cuáles son reglamentarias?',
          tipo: TipoPregunta.opcionMultiple,
          opciones: [
            OpcionParaRendir(id: 'A', texto: 'Prohibido estacionar'),
            OpcionParaRendir(id: 'B', texto: 'Velocidad máxima'),
            OpcionParaRendir(id: 'C', texto: 'Hospital cercano'),
          ],
        ),
      ],
    );

IntentoResultado resultadoDePrueba({
  bool aprobado = false,
  String estado = 'FINALIZADO',
}) => IntentoResultado(
  codigo: 'i1',
  simulacroNombre: 'Simulacro básico',
  estado: estado,
  aciertos: aprobado ? 2 : 1,
  total: 2,
  puntaje: aprobado ? 100 : 50,
  aprobado: aprobado,
  puntajeAprobacion: 70,
  preguntas: const [
    Correccion(
      codigo: 'p1',
      enunciado: '¿Qué indica el PARE?',
      tipo: TipoPregunta.opcionUnica,
      opciones: [
        OpcionCorregida(id: 'A', texto: 'Detenerse', correcta: true),
        OpcionCorregida(id: 'B', texto: 'Seguir', correcta: false),
      ],
      elegidas: ['A'],
      correcta: true,
      explicacion: 'Obliga a detenerse.',
    ),
    Correccion(
      codigo: 'p2',
      enunciado: '¿Cuáles son reglamentarias?',
      tipo: TipoPregunta.opcionMultiple,
      opciones: [
        OpcionCorregida(id: 'A', texto: 'Prohibido estacionar', correcta: true),
        OpcionCorregida(id: 'B', texto: 'Velocidad máxima', correcta: true),
        OpcionCorregida(id: 'C', texto: 'Hospital cercano', correcta: false),
      ],
      elegidas: ['A', 'C'],
      correcta: false,
      explicacion: null,
    ),
  ],
);

IntentoResumen resumenDePrueba({String codigo = 'i1', bool aprobado = true}) =>
    IntentoResumen(
      codigo: codigo,
      simulacroNombre: 'Simulacro básico',
      estado: 'FINALIZADO',
      iniciadoEn: DateTime(2026, 11, 2, 10),
      aciertos: 2,
      total: 2,
      puntaje: 100,
      aprobado: aprobado,
    );

/// Servicio de simulacros controlado desde el test: responde lo configurado y anota cada llamada.
///
/// * Un `Dio` vacío en el constructor: el real (ApiClient) necesita dotenv cargado.
class SimulacrosServiceFalso extends SimulacrosService {
  SimulacrosServiceFalso() : super(dio: Dio());

  List<SimulacroDisponible> simulacros = [];
  List<IntentoResumen> historialDePrueba = [];
  IntentoIniciado intento = intentoDePrueba();
  IntentoResultado resultado = resultadoDePrueba();
  Object? errorAlIniciar;
  Object? errorAlFinalizar;

  final List<String> iniciados = [];
  final List<Map<String, Set<String>>> enviados = [];

  @override
  Future<List<SimulacroDisponible>> disponibles() async => simulacros;

  @override
  Future<IntentoIniciado> iniciar(String simulacroCodigo) async {
    iniciados.add(simulacroCodigo);
    final error = errorAlIniciar;
    if (error != null) throw error;
    return intento;
  }

  @override
  Future<IntentoResultado> finalizar(
    String intentoCodigo,
    Map<String, Set<String>> elegidas,
  ) async {
    enviados.add({
      for (final e in elegidas.entries) e.key: {...e.value},
    });
    final error = errorAlFinalizar;
    if (error != null) throw error;
    return resultado;
  }

  @override
  Future<IntentoResultado> corregido(String intentoCodigo) async => resultado;

  @override
  Future<Pagina<IntentoResumen>> historial({
    int page = 1,
    int pageSize = 50,
  }) async => Pagina(
    items: historialDePrueba,
    page: 1,
    pageSize: pageSize,
    totalItems: historialDePrueba.length,
    totalPages: 1,
  );
}

Override simulacrosServiceFalso(SimulacrosServiceFalso servicio) =>
    simulacrosServiceProvider.overrideWithValue(servicio);
