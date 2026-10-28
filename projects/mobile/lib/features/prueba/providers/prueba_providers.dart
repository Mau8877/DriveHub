import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/prueba.dart';
import '../services/detector_rostro.dart';
import '../services/maquina_prueba.dart';
import '../services/sesiones_service.dart';

/// El detector de rostros del teléfono; es un provider para sustituirlo en los tests (ninguno abre la cámara).
final detectorRostroProvider = Provider.autoDispose<DetectorRostro>((ref) {
  final detector = DetectorRostroMlKit();
  ref.onDispose(detector.detener);
  return detector;
});

final sesionesServiceProvider = Provider<SesionesService>(
  (ref) => SesionesService(),
);

/// Los milisegundos transcurridos desde que se pidió el reloj. Es un provider para controlar el tiempo en los tests.
final relojPruebaProvider = Provider<int Function()>((ref) {
  final reloj = Stopwatch()..start();
  return () => reloj.elapsedMilliseconds;
});

/// Cada cuánto se evalúa la postura de la persona.
const Duration intervaloEvaluacion = Duration(milliseconds: 150);

/// Una medición más vieja que esto se considera «ya no se ve el rostro».
const int vigenciaMedicionMs = 600;

/// En qué momento de la prueba está la persona.
enum FasePrueba { inicio, preparando, enCurso, terminada }

/// Qué pasó al guardar el resultado en el historial.
enum GuardadoPrueba { guardando, guardado, noGuardado }

class EstadoPrueba {
  final FasePrueba fase;

  /// El paso actual (0 es el primero).
  final int indice;

  /// Cuánto de la postura necesaria lleva sostenida la persona en el paso actual (de 0 a 1).
  final double progreso;

  /// El tiempo que le queda en el paso actual.
  final int restanteMs;

  /// Hace un momento que no se ve ningún rostro.
  final bool sinRostro;
  final ResultadoPrueba? resultado;
  final GuardadoPrueba? guardado;

  /// Por qué no se pudo empezar: [PermisoCamaraDenegado] o [CamaraNoDisponible] (u otro error).
  final Object? error;

  const EstadoPrueba({
    this.fase = FasePrueba.inicio,
    this.indice = 0,
    this.progreso = 0,
    this.restanteMs = 0,
    this.sinRostro = false,
    this.resultado,
    this.guardado,
    this.error,
  });

  EstadoPrueba copiar({
    FasePrueba? fase,
    int? indice,
    double? progreso,
    int? restanteMs,
    bool? sinRostro,
    GuardadoPrueba? guardado,
  }) => EstadoPrueba(
    fase: fase ?? this.fase,
    indice: indice ?? this.indice,
    progreso: progreso ?? this.progreso,
    restanteMs: restanteMs ?? this.restanteMs,
    sinRostro: sinRostro ?? this.sinRostro,
    resultado: resultado,
    guardado: guardado ?? this.guardado,
    error: error,
  );
}

/// Dirige la prueba de conducción asistida (CU39).
///
/// * `iniciar` abre la cámara y empieza a medir; cada [intervaloEvaluacion] se le da a la [MaquinaPrueba] la última
///   medición vigente. Al terminar todos los pasos se cierra la cámara y se guarda el resultado en el historial.
/// * Si no se puede abrir la cámara, el error queda en el estado y la prueba sigue en `inicio`.
/// * Si el resultado no se pudo guardar (sin conexión), se muestra igual y se puede reintentar con `guardar`.
class PruebaNotifier extends AutoDisposeNotifier<EstadoPrueba> {
  MaquinaPrueba? _maquina;
  StreamSubscription<Medicion?>? _suscripcion;
  Timer? _temporizador;
  Medicion? _ultima;
  int _ultimaMs = -vigenciaMedicionMs;

  @override
  EstadoPrueba build() {
    ref.onDispose(_parar);
    return const EstadoPrueba();
  }

  Future<void> iniciar() async {
    if (state.fase == FasePrueba.preparando ||
        state.fase == FasePrueba.enCurso) {
      return;
    }
    state = const EstadoPrueba(fase: FasePrueba.preparando);
    final detector = ref.read(detectorRostroProvider);
    try {
      await detector.iniciar();
    } catch (error) {
      state = EstadoPrueba(error: error);
      return;
    }

    final reloj = ref.read(relojPruebaProvider);
    final maquina = MaquinaPrueba()..iniciar(reloj());
    _maquina = maquina;
    _ultima = null;
    _suscripcion = detector.mediciones.listen((medicion) {
      _ultima = medicion;
      _ultimaMs = reloj();
    });
    _temporizador = Timer.periodic(
      intervaloEvaluacion,
      (_) => _evaluar(detector, reloj),
    );
    state = EstadoPrueba(
      fase: FasePrueba.enCurso,
      restanteMs: maquina.configuracion.tiempoPasoMs,
    );
  }

  void _evaluar(DetectorRostro detector, int Function() reloj) {
    final maquina = _maquina;
    if (maquina == null) return;
    final ahora = reloj();
    final vigente = ahora - _ultimaMs <= vigenciaMedicionMs ? _ultima : null;
    maquina.alimentar(vigente, ahora);
    if (maquina.terminada) {
      unawaited(_terminar(detector, maquina, ahora));
      return;
    }
    state = state.copiar(
      indice: maquina.indice,
      progreso: maquina.progresoPaso(ahora),
      restanteMs: maquina.restanteMs(ahora),
      sinRostro: vigente == null,
    );
  }

  Future<void> _terminar(
    DetectorRostro detector,
    MaquinaPrueba maquina,
    int ahora,
  ) async {
    _parar();
    await detector.detener();
    state = EstadoPrueba(
      fase: FasePrueba.terminada,
      indice: maquina.pasos.length,
      resultado: maquina.resultado(ahora, modelo: detector.modelo),
    );
    await guardar();
  }

  /// Guarda el resultado en el historial; si falla, queda `noGuardado` y se puede reintentar.
  Future<void> guardar() async {
    final resultado = state.resultado;
    if (resultado == null || state.guardado == GuardadoPrueba.guardando) return;
    state = state.copiar(guardado: GuardadoPrueba.guardando);
    try {
      await ref.read(sesionesServiceProvider).guardarPrueba(resultado);
      state = state.copiar(guardado: GuardadoPrueba.guardado);
    } catch (_) {
      state = state.copiar(guardado: GuardadoPrueba.noGuardado);
    }
  }

  /// Cancela la prueba en curso y vuelve al inicio.
  Future<void> cancelar() async {
    _parar();
    await ref.read(detectorRostroProvider).detener();
    state = const EstadoPrueba();
  }

  /// Vuelve al inicio para hacer la prueba otra vez.
  void reiniciar() => state = const EstadoPrueba();

  void _parar() {
    _temporizador?.cancel();
    _temporizador = null;
    // * Es un stream de difusión: no hace falta esperar a que la cancelación termine
    unawaited(_suscripcion?.cancel());
    _suscripcion = null;
    _maquina = null;
  }
}

final pruebaProvider =
    AutoDisposeNotifierProvider<PruebaNotifier, EstadoPrueba>(
      PruebaNotifier.new,
    );
