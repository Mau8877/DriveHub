import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Las tres pestañas de la sección «Clases» del estudiante (CU14).
enum PestanaClases {
  misClases('Mis clases'),
  reservar('Reservar'),
  historial('Historial');

  final String etiqueta;

  const PestanaClases(this.etiqueta);
}

/// La pestaña que el estudiante eligió (estado puro de UI). Mientras no elige ninguna es `null`: la pantalla abre
/// «Mis clases» si ya tiene clases por venir y «Reservar» si no.
class ClasesPestanaNotifier extends Notifier<PestanaClases?> {
  @override
  PestanaClases? build() => null;

  void elegir(PestanaClases pestana) => state = pestana;
}

final clasesPestanaUiProvider = NotifierProvider<ClasesPestanaNotifier, PestanaClases?>(ClasesPestanaNotifier.new);
