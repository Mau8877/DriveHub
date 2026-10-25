import 'dart:io';

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
  Future<File> descargarPdf(String codigoComprobante) async {
    descargas.add(codigoComprobante);
    await antes?.call();
    final fallo = error;
    if (fallo != null) throw fallo;
    return File('comprobante-$codigoComprobante.pdf');
  }
}

/// Un abridor de archivos que no abre nada: anota las rutas y responde si "había visor".
class AbridorFalso {
  bool hayVisor = true;
  final List<String> abiertos = [];

  Future<bool> call(String ruta) async {
    abiertos.add(ruta);
    return hayVisor;
  }
}

List<Override> comprobantesFalsos(ComprobantesServiceFalso servicio, AbridorFalso abridor) => [
  comprobantesServiceProvider.overrideWithValue(servicio),
  abridorDeArchivosProvider.overrideWithValue(abridor.call),
];
