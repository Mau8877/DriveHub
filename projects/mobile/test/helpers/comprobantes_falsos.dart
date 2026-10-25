import 'package:dio/dio.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/features/pagos/providers/comprobante_provider.dart';
import 'package:mobile/features/pagos/services/comprobantes_service.dart';

/// Servicio de comprobantes controlado desde el test (CU30): anota cada descarga y responde lo configurado.
class ComprobantesServiceFalso extends ComprobantesService {
  // * Un Dio vacío: el real (ApiClient) necesita dotenv cargado
  ComprobantesServiceFalso() : super(dio: Dio());

  Object? error;
  Future<void> Function()? antes;
  final List<String> descargas = [];

  @override
  Future<List<int>> descargarPdf(String codigoComprobante) async {
    descargas.add(codigoComprobante);
    await antes?.call();
    final fallo = error;
    if (fallo != null) throw fallo;
    return [37, 80, 68, 70];
  }
}

/// Un abridor de PDF que no abre nada: anota los nombres y responde si "había visor".
class AbridorFalso {
  bool hayVisor = true;
  final List<String> abiertos = [];

  Future<bool> call(List<int> bytes, String nombre) async {
    abiertos.add(nombre);
    return hayVisor;
  }
}

List<Override> comprobantesFalsos(ComprobantesServiceFalso servicio, AbridorFalso abridor) => [
  comprobantesServiceProvider.overrideWithValue(servicio),
  abridorDePdfProvider.overrideWithValue(abridor.call),
];
