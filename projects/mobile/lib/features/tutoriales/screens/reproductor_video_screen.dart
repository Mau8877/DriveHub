import 'package:flutter/material.dart';
import 'package:youtube_player_iframe/youtube_player_iframe.dart';

import '../../../shared/widgets/cockpit_scaffold.dart';
import '../models/youtube.dart';

/// Lo que recibe el reproductor por la ruta `/video` (`extra`): el enlace de YouTube y el título de la barra.
class ReproductorVideoArgs {
  final String url;
  final String titulo;

  const ReproductorVideoArgs({required this.url, required this.titulo});
}

/// Reproductor de video dentro de la app (tutoriales, CU35): reproduce el video de YouTube del tutorial sin salir
/// de DriveHub.
///
/// * Un enlace que no es de YouTube no llega (el backend lo rechaza al crear el tutorial), pero si pasara se
///   muestra un aviso en lugar del reproductor.
class ReproductorVideoScreen extends StatefulWidget {
  final String url;
  final String titulo;

  const ReproductorVideoScreen({
    super.key,
    required this.url,
    required this.titulo,
  });

  @override
  State<ReproductorVideoScreen> createState() => _ReproductorVideoScreenState();
}

class _ReproductorVideoScreenState extends State<ReproductorVideoScreen> {
  YoutubePlayerController? _controlador;

  @override
  void initState() {
    super.initState();
    final id = idDeYoutube(widget.url);
    if (id != null) {
      _controlador = YoutubePlayerController.fromVideoId(
        videoId: id,
        autoPlay: true,
        params: const YoutubePlayerParams(showFullscreenButton: true),
      );
    }
  }

  @override
  void dispose() {
    _controlador?.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final controlador = _controlador;

    return CockpitScaffold(
      appBar: AppBar(
        title: Text(widget.titulo, overflow: TextOverflow.ellipsis),
      ),
      body: controlador == null
          ? const Center(
              key: Key('errorReproductor'),
              child: Padding(
                padding: EdgeInsets.all(32),
                child: Text(
                  'No pudimos abrir el video: el enlace no es válido.',
                  textAlign: TextAlign.center,
                ),
              ),
            )
          : Center(child: YoutubePlayer(controller: controlador)),
    );
  }
}
