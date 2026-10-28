import 'luz_tablero.dart';

/// Lo que el modelo del teléfono cree que es la luz, con su confianza (de 0 a 1).
class Deteccion {
  final String codigo;
  final double confianza;

  const Deteccion({required this.codigo, required this.confianza});
}

/// Una luz reconocida: la detección unida a su ficha del catálogo.
class LuzReconocida {
  final LuzTablero luz;
  final double confianza;

  const LuzReconocida({required this.luz, required this.confianza});
}

/// El resultado de analizar una foto del tablero en el teléfono (CU40).
class AnalisisTablero {
  /// Las luces más probables (hasta 3), de mayor a menor confianza, que existen en el catálogo.
  final List<LuzReconocida> luces;

  /// Cuánto tardó el modelo en el teléfono.
  final int latenciaMs;

  /// Nombre y versión del modelo, para el historial.
  final String modelo;

  /// La miniatura de la foto (JPEG de hasta 512 px), si se va a guardar en el historial.
  final List<int>? miniatura;

  const AnalisisTablero({
    required this.luces,
    required this.latenciaMs,
    required this.modelo,
    this.miniatura,
  });

  /// La luz más probable, o `null` si no hay ninguna.
  LuzReconocida? get principal => luces.isEmpty ? null : luces.first;

  /// ¿La luz principal supera el umbral de confianza? Si no, el modelo «no está seguro».
  bool seguro(double umbral) => (principal?.confianza ?? 0) >= umbral;
}
