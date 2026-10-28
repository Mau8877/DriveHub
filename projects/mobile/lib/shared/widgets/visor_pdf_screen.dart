import 'package:flutter/material.dart';
import 'package:pdfrx/pdfrx.dart';

import 'cockpit_scaffold.dart';

/// Lo que recibe el visor de PDF por la ruta `/pdf` (`extra`): la dirección del archivo y el título de la barra.
class VisorPdfArgs {
  final String url;
  final String titulo;

  const VisorPdfArgs({required this.url, required this.titulo});
}

/// Visor de PDF dentro de la app (manuales, CU34; tutoriales, CU33 y CU35): el archivo se lee de su dirección
/// pública y se desliza, se acerca y se aleja con los dedos.
///
/// * Vive en `shared/` porque lo usan varias features.
/// * Si el archivo no carga (sin conexión, dirección rota) muestra un aviso en lugar del documento.
class VisorPdfScreen extends StatelessWidget {
  final String url;
  final String titulo;

  const VisorPdfScreen({super.key, required this.url, required this.titulo});

  @override
  Widget build(BuildContext context) {
    final direccion = Uri.tryParse(url);

    return CockpitScaffold(
      appBar: AppBar(title: Text(titulo, overflow: TextOverflow.ellipsis)),
      body: direccion == null
          ? const _ErrorDelVisor()
          : PdfViewer.uri(
              direccion,
              params: PdfViewerParams(
                errorBannerBuilder: (context, error, stackTrace, documentRef) =>
                    const _ErrorDelVisor(),
              ),
            ),
    );
  }
}

class _ErrorDelVisor extends StatelessWidget {
  const _ErrorDelVisor();

  @override
  Widget build(BuildContext context) {
    return const Center(
      key: Key('errorVisorPdf'),
      child: Padding(
        padding: EdgeInsets.all(32),
        child: Text(
          'No pudimos abrir el PDF. Revisa tu conexión e inténtalo de nuevo.',
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}
