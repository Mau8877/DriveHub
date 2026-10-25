import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../providers/comprobante_provider.dart';

/// El texto de aviso de un error al bajar o abrir un comprobante (CU30), en español y sin términos técnicos.
String mensajeDeErrorDeComprobante(Object error) {
  if (error is SinVisorDePdf) {
    return 'No hay ninguna app para abrir PDF en tu teléfono.';
  }
  if (error is ApiException) {
    return switch (error.code) {
      'ERR_PAG_14' =>
        'Tu comprobante todavía se está preparando, inténtalo en unos minutos.',
      'ERR_PAG_15' =>
        'No pudimos leer el comprobante. Inténtalo de nuevo más tarde.',
      _ => error.message,
    };
  }
  if (error is DioException &&
      (error.type == DioExceptionType.connectionError ||
          error.type == DioExceptionType.connectionTimeout ||
          error.type == DioExceptionType.sendTimeout ||
          error.type == DioExceptionType.receiveTimeout)) {
    return 'No hay conexión. Revisa tu internet e inténtalo de nuevo.';
  }
  return 'No pudimos abrir el comprobante. Inténtalo de nuevo más tarde.';
}

/// El botón «Comprobante» (CU30): baja el PDF de un comprobante y lo abre en el visor del teléfono.
///
/// * Mientras baja muestra el spinner y se deshabilita.
/// * Si falla, avisa con el motivo (en preparación, sin conexión, sin visor de PDF...).
class BotonComprobante extends ConsumerWidget {
  final String codigo;

  const BotonComprobante({super.key, required this.codigo});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final descargando = ref.watch(comprobanteDescargaProvider(codigo)).isLoading;

    return OutlinedButton.icon(
      key: Key('botonComprobante-$codigo'),
      // * El tema estira los botones a todo el ancho; este va en una fila junto a otros elementos
      style: OutlinedButton.styleFrom(minimumSize: const Size(0, 40)),
      onPressed: descargando
          ? null
          : () async {
              final abierto = await ref
                  .read(comprobanteDescargaProvider(codigo).notifier)
                  .descargarYAbrir();
              if (abierto || !context.mounted) return;
              final error = ref.read(comprobanteDescargaProvider(codigo)).error;
              if (error != null) avisar(context, mensajeDeErrorDeComprobante(error));
            },
      icon: descargando
          ? const IndicadorBoton()
          : const Icon(Icons.picture_as_pdf_outlined),
      label: const Text('Comprobante'),
    );
  }
}
