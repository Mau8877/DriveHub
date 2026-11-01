import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../models/intento_iniciado.dart';
import '../models/intento_resultado.dart';
import '../models/intento_resumen.dart';
import '../models/simulacro_disponible.dart';

/// Llamadas a la API de los simulacros escritos del estudiante (CU21): ver los simulacros de su escuela, rendir
/// uno y ver su corrección.
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`), no como un
/// * [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class SimulacrosService {
  final Dio _dio;

  SimulacrosService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  Future<T> _llamar<T>(Future<T> Function() llamada) async {
    try {
      return await llamada();
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  /// Los simulacros activos de la escuela del estudiante.
  Future<List<SimulacroDisponible>> disponibles() {
    return _llamar(() async {
      final response = await _dio.get<List<dynamic>>('/simulacros/disponibles');
      return response.data!
          .map((e) => SimulacroDisponible.fromJson(e as Map<String, dynamic>))
          .toList();
    });
  }

  /// Inicia un intento: el backend arma las preguntas y empieza a contar el tiempo.
  Future<IntentoIniciado> iniciar(String simulacroCodigo) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/intentos-simulacro',
        data: {'simulacroCodigo': simulacroCodigo},
      );
      return IntentoIniciado.fromJson(response.data!);
    });
  }

  /// Envía las respuestas ([elegidas]: código de la pregunta -> ids de las opciones marcadas) y trae la
  /// corrección. Una pregunta sin entrada es una pregunta sin responder.
  Future<IntentoResultado> finalizar(
    String intentoCodigo,
    Map<String, Set<String>> elegidas,
  ) {
    return _llamar(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/intentos-simulacro/$intentoCodigo/finalizar',
        data: {
          'respuestas': [
            for (final entrada in elegidas.entries)
              {
                'preguntaCodigo': entrada.key,
                'elegidas': entrada.value.toList()..sort(),
              },
          ],
        },
      );
      return IntentoResultado.fromJson(response.data!);
    });
  }

  /// La corrección de un intento ya terminado.
  Future<IntentoResultado> corregido(String intentoCodigo) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/intentos-simulacro/$intentoCodigo',
      );
      return IntentoResultado.fromJson(response.data!);
    });
  }

  /// El historial del estudiante, del más reciente al más antiguo.
  Future<Pagina<IntentoResumen>> historial({int page = 1, int pageSize = 50}) {
    return _llamar(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/intentos-simulacro',
        queryParameters: {'page': page, 'pageSize': pageSize},
      );
      return Pagina.fromJson(response.data!, IntentoResumen.fromJson);
    });
  }
}
