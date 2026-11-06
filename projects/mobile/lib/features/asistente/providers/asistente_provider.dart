import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../clases/models/reserva.dart';
import '../models/estado_asistente.dart';
import '../models/preparacion_de_cambio.dart';
import '../models/resultado_interpretacion.dart';
import '../services/ejecutor_de_consultas.dart';
import '../services/modelo_de_acciones.dart';
import '../services/preparador_de_cambios.dart';
import '../services/reconocedor_de_voz.dart';
import 'modelo_de_acciones_provider.dart';
import 'reconocedor_de_voz_provider.dart';

/// El asistente de voz de clases (CU50): encadena la voz, el modelo y los ejecutores, y expone el [EstadoAsistente]
/// que la pantalla dibuja.
///
/// * Mantener pulsado llama a [empezarAEscuchar] y soltar a [terminarDeEscuchar]. Si se suelta antes de que
///   termine de arrancar el micrófono, se detiene igual: nunca queda una grabación abierta.
/// * Las consultas se ejecutan al momento sobre la pantalla (`EjecutorDeConsultas`).
/// * Cancelar y reprogramar NUNCA los hace este controlador: solo identifica la clase (`PreparadorDeCambios`) y
///   deja el estado en [PideCambio] para que la pantalla abra el diálogo o la hoja de `clases`, donde se confirma.
/// * Nada de lo que falla llega como excepción a la pantalla: todo es un estado con un mensaje en español.
class AsistenteNotifier extends Notifier<EstadoAsistente> {
  bool _iniciando = false;
  bool _soltado = false;

  @override
  EstadoAsistente build() => const Inactivo();

  Future<void> empezarAEscuchar() async {
    if (state is Escuchando || state is Entendiendo) return;
    state = const Escuchando();
    _iniciando = true;
    _soltado = false;
    try {
      final voz = await ref.read(reconocedorDeVozProvider.future);
      await voz.empezar();
    } on PermisoDeMicrofonoDenegado {
      _iniciando = false;
      state = const SinPermiso();
      return;
    } catch (e) {
      _iniciando = false;
      state = Fallo(_mensajeDeFallo(e));
      return;
    }
    _iniciando = false;
    // * La persona soltó el botón mientras arrancaba el micrófono
    if (_soltado) await terminarDeEscuchar();
  }

  Future<void> terminarDeEscuchar() async {
    if (state is! Escuchando) return;
    if (_iniciando) {
      _soltado = true;
      return;
    }

    state = const Entendiendo();
    final String texto;
    try {
      final voz = await ref.read(reconocedorDeVozProvider.future);
      texto = await voz.terminar();
    } catch (e) {
      state = Fallo(_mensajeDeFallo(e));
      return;
    }
    if (texto.isEmpty) {
      state = const NoEntendi(mensaje: 'No te escuché. Mantén pulsado el micrófono y habla.');
      return;
    }

    state = Entendiendo(texto: texto);
    try {
      final modelo = await ref.read(modeloDeAccionesProvider.future);
      final interpretacion = await modelo.entender(texto);
      state = await _actuar(texto, interpretacion);
    } catch (e) {
      state = Fallo(_mensajeDeFallo(e));
    }
  }

  /// La persona eligió una de las clases que cuadraban ([Elegir]).
  void elegir(Reserva reserva) {
    final actual = state;
    if (actual is! Elegir) return;
    final preparacion = ref.read(preparadorDeCambiosProvider).proponer(actual.tipo, reserva);
    state = switch (preparacion) {
      PropuestaDeCambio() => PideCambio(texto: actual.texto, propuesta: preparacion),
      NadaQueCambiar(:final mensaje) => Resultado(texto: actual.texto, mensaje: mensaje, exito: false),
      VariasPosibles() => actual,
    };
  }

  /// Vuelve a [Inactivo]; si estaba grabando, descarta el audio.
  Future<void> cerrar() async {
    final estabaGrabando = state is Escuchando;
    state = const Inactivo();
    if (estabaGrabando) {
      _soltado = false;
      try {
        final voz = await ref.read(reconocedorDeVozProvider.future);
        await voz.cancelar();
      } catch (_) {
        // * Ya no hay nada que mostrar: lo importante es que no quede una grabación abierta
      }
    }
  }

  Future<EstadoAsistente> _actuar(String texto, ResultadoInterpretacion interpretacion) async {
    switch (interpretacion) {
      case AccionRechazada(:final motivo):
        return NoEntendi(texto: texto, mensaje: _mensajeDeRechazo(motivo));
      case AccionEntendida(:final accion):
        if (!accion.accion.cambiaDatos) {
          final r = await ref.read(ejecutorDeConsultasProvider).ejecutar(accion);
          return Resultado(texto: texto, mensaje: r.mensaje, exito: r.exito);
        }
        final preparacion = await ref.read(preparadorDeCambiosProvider).preparar(accion);
        return switch (preparacion) {
          PropuestaDeCambio() => PideCambio(texto: texto, propuesta: preparacion),
          VariasPosibles(:final tipo, :final candidatas) => Elegir(texto: texto, tipo: tipo, candidatas: candidatas),
          NadaQueCambiar(:final mensaje) => Resultado(texto: texto, mensaje: mensaje, exito: false),
        };
    }
  }

  String _mensajeDeRechazo(MotivoRechazo motivo) => switch (motivo) {
    MotivoRechazo.fueraDeAlcance => 'Eso no se puede hacer desde Clases. Puedo ver tus clases, buscar horarios, '
        'cancelar o reprogramar una clase y decirte cuántas horas te quedan.',
    MotivoRechazo.faltaDato => 'Me falta un dato para hacerlo. Repítelo con más detalle, por ejemplo la hora.',
    MotivoRechazo.desconocida || MotivoRechazo.argumentoInvalido => 'No entendí lo que pides. Intenta decirlo de otra forma.',
  };

  String _mensajeDeFallo(Object e) => switch (e) {
    ModeloDeVozNoDisponible() => 'El asistente de voz no está disponible en este teléfono.',
    ModeloDeAccionesNoDisponible() => 'El asistente no está disponible en este teléfono.',
    _ => 'Algo salió mal con el asistente. Intenta de nuevo.',
  };
}

final asistenteProvider = NotifierProvider<AsistenteNotifier, EstadoAsistente>(AsistenteNotifier.new);
