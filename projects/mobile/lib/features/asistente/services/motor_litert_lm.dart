import 'dart:io';

import 'package:flutter_litert_lm/flutter_litert_lm.dart';

import '../models/catalogo_acciones.dart';
import 'modelo_de_acciones.dart';

/// El motor real del asistente (CU50): el modelo FunctionGemma afinado (`.litertlm`) corriendo en el teléfono con
/// LiteRT-LM (CPU por defecto; GPU si se pide).
///
/// * El motor se crea la primera vez que se usa y se reutiliza: cargar el modelo tarda y ocupa memoria.
/// * Cada petición usa una conversación nueva (sin historial) y la cierra al terminar: una frase no puede
///   influir en la siguiente.
/// * Las herramientas son las del catálogo y NO se ejecutan (`automaticToolCalling: false`): el modelo solo
///   elige; ejecutar es de la app, después de validar.
/// * Temperatura baja (0,1): se busca la acción más probable, no variedad.
/// * Sin `systemInstruction`: el `.litertlm` trae el formato de FunctionGemma en sus metadatos, y poner otro
///   cambia el prompt con el que se entrenó el modelo.
class MotorLitertLm implements MotorDeLlamadas {
  final String _rutaModelo;
  final CatalogoAcciones _catalogo;

  LiteLmEngine? _motor;
  Future<LiteLmEngine>? _creando;

  final LiteLmBackend _backend;

  MotorLitertLm({required this._rutaModelo, required this._catalogo, this._backend = LiteLmBackend.cpu});

  @override
  Future<List<LlamadaAFuncion>> llamar(String texto) async {
    final motor = await _obtenerMotor();
    final conversacion = await motor.createConversation(
      LiteLmConversationConfig(
        tools: [
          for (final accion in _catalogo.acciones.values)
            LiteLmTool(
              name: accion.nombre,
              description: accion.descripcion,
              parameters: accion.esquemaDeParametros(),
            ),
        ],
        automaticToolCalling: false,
        samplerConfig: const LiteLmSamplerConfig(temperature: 0.1, topK: 64, topP: 0.95),
      ),
    );
    try {
      final respuesta = await conversacion.sendMessage(texto);
      return [for (final c in respuesta.toolCalls) LlamadaAFuncion(c.name, c.arguments)];
    } finally {
      await conversacion.dispose();
    }
  }

  Future<LiteLmEngine> _obtenerMotor() {
    final existente = _motor;
    if (existente != null) return Future.value(existente);
    return _creando ??= _crear();
  }

  Future<LiteLmEngine> _crear() async {
    if (!File(_rutaModelo).existsSync()) {
      _creando = null;
      throw ModeloDeAccionesNoDisponible('No está el modelo en $_rutaModelo');
    }
    try {
      final motor = await LiteLmEngine.create(LiteLmEngineConfig(modelPath: _rutaModelo, backend: _backend));
      _motor = motor;
      return motor;
    } catch (e) {
      throw ModeloDeAccionesNoDisponible('No se pudo cargar el modelo: $e');
    } finally {
      _creando = null;
    }
  }

  @override
  Future<void> liberar() async {
    final motor = _motor;
    _motor = null;
    await motor?.dispose();
  }
}
