import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';

/// Baja el PDF de un comprobante (CU30).
///
/// * El PDF viaja con el token del Bearer y llega como bytes: guardarlo y abrirlo depende de la plataforma (el
///   teléfono o el navegador), y de eso se ocupa `abrirPdf`, no el servicio.
/// * Un error de negocio llega como [ApiException] (`ERR_PAG_13` no existe o no es tuyo, `ERR_PAG_14` todavía se
///   prepara, `ERR_PAG_15` el almacén no responde). Cualquier otro error (sin red) se relanza tal cual.
class ComprobantesService {
  final Dio _dio;

  ComprobantesService({Dio? dio}) : _dio = dio ?? ApiClient.instance;

  /// Los bytes del PDF del comprobante.
  Future<List<int>> descargarPdf(String codigoComprobante) async {
    try {
      final response = await _dio.get<List<int>>(
        '/comprobantes/$codigoComprobante/pdf',
        options: Options(responseType: ResponseType.bytes),
      );
      return response.data!;
    } on DioException catch (e) {
      final error = e.error;
      if (error is ApiException) throw error;
      rethrow;
    }
  }
}
