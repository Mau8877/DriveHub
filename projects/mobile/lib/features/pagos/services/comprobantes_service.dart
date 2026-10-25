import 'dart:io';

import 'package:dio/dio.dart';
import 'package:path_provider/path_provider.dart';

import '../../../core/network/api_client.dart';

/// Baja el PDF de un comprobante (CU30).
///
/// * El PDF viaja con el token del Bearer y se guarda en el directorio temporal: no hay una dirección pública que
///   compartir ni se conserva para verlo sin conexión.
/// * Un error de negocio llega como [ApiException] (`ERR_PAG_13` no existe o no es tuyo, `ERR_PAG_14` todavía se
///   prepara, `ERR_PAG_15` el almacén no responde). Cualquier otro error (sin red) se relanza tal cual.
class ComprobantesService {
  final Dio _dio;
  final Future<Directory> Function() _directorioTemporal;

  ComprobantesService({
    Dio? dio,
    Future<Directory> Function()? directorioTemporal,
  }) : _dio = dio ?? ApiClient.instance,
       _directorioTemporal = directorioTemporal ?? getTemporaryDirectory;

  /// Descarga el PDF y lo guarda como `comprobante-{codigo}.pdf` (se sobrescribe), y devuelve el archivo.
  Future<File> descargarPdf(String codigoComprobante) async {
    final List<int> bytes;
    try {
      final response = await _dio.get<List<int>>(
        '/comprobantes/$codigoComprobante/pdf',
        options: Options(responseType: ResponseType.bytes),
      );
      bytes = response.data!;
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
    final directorio = await _directorioTemporal();
    final archivo = File(
      '${directorio.path}${Platform.pathSeparator}comprobante-$codigoComprobante.pdf',
    );
    return archivo.writeAsBytes(bytes, flush: true);
  }
}
