import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/pagina.dart';
import '../models/tutorial.dart';
import '../models/tutorial_request.dart';

/// Llamadas a la API de los tutoriales: los del instructor (CU33) y los publicados (CU35).
///
/// * Un error de negocio del backend llega como [ApiException] (con su código `ERR_...`), no como un
///   [DioException] crudo. Cualquier otro error (sin red, por ejemplo) se relanza tal cual.
class TutorialesService {
  final Dio _dio;

  TutorialesService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Los tutoriales del instructor, del más reciente al más antiguo; [estado] filtra por estado.
  Future<Pagina<Tutorial>> mios({
    EstadoTutorial? estado,
    int page = 1,
    int pageSize = 50,
  }) => _lista('/tutoriales', {
    'page': page,
    'pageSize': pageSize,
    'filter[estado]': ?estado?.valorApi,
  });

  /// Los tutoriales publicados de la escuela dirigidos a quien pide (CU35), con búsqueda y filtro por tipo.
  Future<Pagina<Tutorial>> publicados({
    String? busqueda,
    TipoTutorial? tipo,
    int page = 1,
    int pageSize = 50,
  }) => _lista('/tutoriales/publicados', {
    'page': page,
    'pageSize': pageSize,
    if (busqueda != null && busqueda.trim().isNotEmpty)
      'search': busqueda.trim(),
    'filter[tipo]': ?tipo?.valorApi,
  });

  /// Sube el PDF de un tutorial como `multipart/form-data` con la parte `archivo`.
  Future<ArchivoSubido> subirPdf(
    Uint8List bytes, {
    required String nombre,
  }) async {
    final cuerpo = FormData.fromMap({
      'archivo': MultipartFile.fromBytes(
        bytes,
        filename: nombre,
        contentType: DioMediaType.parse('application/pdf'),
      ),
    });
    final datos = await _llamar(
      () =>
          _dio.post<Map<String, dynamic>>('/tutoriales/archivos', data: cuerpo),
    );
    return ArchivoSubido.fromJson(datos!);
  }

  Future<Tutorial> crear(TutorialRequest request) async {
    final datos = await _llamar(
      () => _dio.post<Map<String, dynamic>>(
        '/tutoriales',
        data: request.toJson(),
      ),
    );
    return Tutorial.fromJson(datos!);
  }

  Future<Tutorial> editar(String codigo, TutorialRequest request) async {
    final datos = await _llamar(
      () => _dio.patch<Map<String, dynamic>>(
        '/tutoriales/$codigo',
        data: request.toJson(),
      ),
    );
    return Tutorial.fromJson(datos!);
  }

  Future<Tutorial> publicar(String codigo) async {
    final datos = await _llamar(
      () => _dio.post<Map<String, dynamic>>('/tutoriales/$codigo/publicar'),
    );
    return Tutorial.fromJson(datos!);
  }

  Future<Tutorial> archivar(String codigo) async {
    final datos = await _llamar(
      () => _dio.post<Map<String, dynamic>>('/tutoriales/$codigo/archivar'),
    );
    return Tutorial.fromJson(datos!);
  }

  Future<Pagina<Tutorial>> _lista(
    String ruta,
    Map<String, dynamic> query,
  ) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        ruta,
        queryParameters: query,
      );
      return Pagina.fromJson(response.data!, Tutorial.fromJson);
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }

  Future<Map<String, dynamic>?> _llamar(
    Future<Response<Map<String, dynamic>>> Function() peticion,
  ) async {
    try {
      return (await peticion()).data;
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
