import 'dart:math' as math;

import '../models/prueba.dart';

/// Los umbrales de la prueba (CU39). Los ángulos son en grados.
class ConfiguracionPrueba {
  /// Cuánto hay que girar la cabeza hacia un espejo lateral.
  final double anguloLado;

  /// Cuánto hay que levantar la cabeza para el retrovisor.
  final double anguloArriba;

  /// Cuánto se puede desviar la cabeza de «al frente» (a los lados y arriba o abajo).
  final double toleranciaFrente;

  /// La mínima probabilidad de ojos abiertos para dar por atenta a la persona.
  final double ojosMinimos;

  /// Cuánto tiempo hay que sostener la postura para que el paso cuente.
  final int sostenerMs;

  /// Cuánto tiempo se le da a la persona en cada paso.
  final int tiempoPasoMs;

  /// Si el teléfono entrega el giro con el signo contrario al esperado (positivo = izquierda del conductor),
  /// se invierte acá en lugar de tocar la lógica.
  final bool invertirLados;

  const ConfiguracionPrueba({
    this.anguloLado = 18,
    this.anguloArriba = 10,
    this.toleranciaFrente = 10,
    this.ojosMinimos = 0.4,
    this.sostenerMs = 600,
    this.tiempoPasoMs = 8000,
    this.invertirLados = false,
  });
}

/// La máquina de estados de la prueba de conducción (CU39): recorre los pasos uno por uno y decide si la persona
/// logró cada uno, según la orientación de su cabeza.
///
/// * Es puro: no usa la cámara ni el reloj. Recibe las mediciones y la hora (en milisegundos) desde afuera, así que
///   se prueba sin teléfono.
/// * Un paso se logra al sostener la postura [ConfiguracionPrueba.sostenerMs]; si pasa [ConfiguracionPrueba.tiempoPasoMs]
///   sin lograrlo, queda como no logrado y se sigue con el siguiente.
/// * «Al frente» pide además los ojos abiertos, como señal de atención.
class MaquinaPrueba {
  final List<PasoPrueba> pasos;
  final ConfiguracionPrueba configuracion;

  final List<ResultadoPaso> _resultados = [];
  int _indice = 0;
  int _inicioPasoMs = 0;
  int _inicioTotalMs = 0;
  int? _sostenidoDesdeMs;
  bool _iniciada = false;

  MaquinaPrueba({
    this.pasos = pasosDePrueba,
    this.configuracion = const ConfiguracionPrueba(),
  });

  bool get iniciada => _iniciada;
  bool get terminada => _iniciada && _indice >= pasos.length;
  int get indice => _indice;
  PasoPrueba? get pasoActual => terminada || !_iniciada ? null : pasos[_indice];
  List<ResultadoPaso> get resultados => List.unmodifiable(_resultados);

  /// Cuánto de la postura necesaria lleva sostenida la persona en el paso actual (de 0 a 1).
  double progresoPaso(int ahoraMs) {
    final desde = _sostenidoDesdeMs;
    if (desde == null || terminada) return 0;
    return math.min(1, (ahoraMs - desde) / configuracion.sostenerMs);
  }

  /// Cuánto tiempo le queda a la persona en el paso actual.
  int restanteMs(int ahoraMs) => terminada
      ? 0
      : math.max(0, configuracion.tiempoPasoMs - (ahoraMs - _inicioPasoMs));

  void iniciar(int ahoraMs) {
    _iniciada = true;
    _indice = 0;
    _resultados.clear();
    _inicioPasoMs = ahoraMs;
    _inicioTotalMs = ahoraMs;
    _sostenidoDesdeMs = null;
  }

  /// Avanza la prueba con la última medición ([medicion] es `null` si no se ve ningún rostro) a las [ahoraMs].
  void alimentar(Medicion? medicion, int ahoraMs) {
    if (!_iniciada || terminada) return;

    if (medicion != null && _cumple(pasos[_indice].tipo, medicion)) {
      _sostenidoDesdeMs ??= ahoraMs;
      if (ahoraMs - _sostenidoDesdeMs! >= configuracion.sostenerMs) {
        _cerrarPaso(
          aprobado: true,
          ahoraMs: ahoraMs,
          tiempoMs: ahoraMs - _inicioPasoMs,
        );
        return;
      }
    } else {
      _sostenidoDesdeMs = null;
    }

    if (ahoraMs - _inicioPasoMs >= configuracion.tiempoPasoMs) {
      _cerrarPaso(
        aprobado: false,
        ahoraMs: ahoraMs,
        tiempoMs: configuracion.tiempoPasoMs,
      );
    }
  }

  /// El resultado final; solo cuando la prueba terminó.
  ResultadoPrueba resultado(int ahoraMs, {required String modelo}) {
    assert(terminada, 'La prueba todavía no terminó');
    final aprobados = _resultados.where((r) => r.aprobado).length;
    return ResultadoPrueba(
      pasos: List.unmodifiable(_resultados),
      duracionSeg: math.max(1, ((ahoraMs - _inicioTotalMs) / 1000).round()),
      puntaje: (aprobados / pasos.length * 1000).round() / 10,
      retroalimentacion: _retroalimentacion(),
      modelo: modelo,
    );
  }

  void _cerrarPaso({
    required bool aprobado,
    required int ahoraMs,
    required int tiempoMs,
  }) {
    _resultados.add(
      ResultadoPaso(
        orden: _indice + 1,
        paso: pasos[_indice],
        aprobado: aprobado,
        tiempoMs: tiempoMs,
      ),
    );
    _indice++;
    _inicioPasoMs = ahoraMs;
    _sostenidoDesdeMs = null;
  }

  bool _cumple(TipoPaso tipo, Medicion m) {
    final t = configuracion;
    final lado = t.invertirLados ? -m.giro : m.giro;
    return switch (tipo) {
      TipoPaso.frente =>
        m.giro.abs() <= t.toleranciaFrente &&
            m.cabeceo.abs() <= t.toleranciaFrente &&
            (m.ojosAbiertos ?? 1) >= t.ojosMinimos,
      TipoPaso.espejoIzquierdo => lado >= t.anguloLado,
      TipoPaso.espejoDerecho => lado <= -t.anguloLado,
      TipoPaso.retrovisor => m.cabeceo >= t.anguloArriba,
    };
  }

  String _retroalimentacion() {
    final fallidos = _resultados
        .where((r) => !r.aprobado)
        .map((r) => r.paso.instruccion.toLowerCase());
    if (fallidos.isEmpty) {
      return '¡Muy bien! Revisaste los espejos y el frente en el orden correcto. Mantén ese hábito antes de cada maniobra.';
    }
    final aprobados = _resultados.length - fallidos.length;
    final lista = fallidos.join(', ');
    return aprobados == 0
        ? 'No logramos validar ningún paso. Coloca el teléfono de frente, con buena luz, y gira la cabeza con calma. Pasos pendientes: $lista.'
        : 'Lograste $aprobados de ${_resultados.length} pasos. Te faltó: $lista. Antes de maniobrar revisa siempre los espejos con un giro claro de la cabeza.';
  }
}
