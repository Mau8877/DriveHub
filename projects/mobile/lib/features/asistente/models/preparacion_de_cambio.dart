import '../../clases/models/reserva.dart';

/// Qué cambio de una clase se pidió por voz.
enum TipoCambio { cancelar, reprogramar }

/// El resultado de identificar qué clase quiso cambiar la persona (CU50). Preparar NO cambia nada: solo apunta.
sealed class PreparacionDeCambio {
  const PreparacionDeCambio();
}

/// Se identificó UNA clase. La pantalla abre la confirmación que ya existe en `clases` (el diálogo de cancelar o la
/// hoja de reprogramar): ahí la persona confirma.
class PropuestaDeCambio extends PreparacionDeCambio {
  final TipoCambio tipo;
  final Reserva reserva;

  /// Lo que se le dice a la persona al abrir la confirmación («Cancelar tu clase del viernes 7/11 a las 10:00…»).
  final String resumen;

  const PropuestaDeCambio({required this.tipo, required this.reserva, required this.resumen});
}

/// Varias clases cuadran con lo dicho: la persona elige una (de [candidatas], de la más próxima a la más lejana).
class VariasPosibles extends PreparacionDeCambio {
  final TipoCambio tipo;
  final List<Reserva> candidatas;

  const VariasPosibles({required this.tipo, required this.candidatas});
}

/// No hay nada que cambiar (sin clases, ninguna cuadra, fuera de plazo, sin red): [mensaje] se lo explica.
class NadaQueCambiar extends PreparacionDeCambio {
  final String mensaje;

  const NadaQueCambiar(this.mensaje);
}
