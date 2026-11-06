import '../models/catalogo_acciones.dart';
import '../models/resultado_interpretacion.dart';
import 'interpretar_accion.dart';

/// El modelo de acciones no está en el teléfono (todavía no se copió) o no se pudo cargar (CU50).
class ModeloDeAccionesNoDisponible implements Exception {
  final String motivo;

  const ModeloDeAccionesNoDisponible(this.motivo);

  @override
  String toString() => 'ModeloDeAccionesNoDisponible: $motivo';
}

/// Entiende lo que dijo la persona y elige una acción de la pantalla de Clases (CU50). Corre en el teléfono, sin
/// internet.
abstract class ModeloDeAcciones {
  /// La acción que corresponde a [texto], o el rechazo si el modelo no eligió una acción válida del catálogo.
  /// Lanza [ModeloDeAccionesNoDisponible] si el modelo no está.
  Future<ResultadoInterpretacion> entender(String texto);

  /// Libera la memoria del modelo.
  Future<void> liberar();
}

/// Una llamada a función tal como la devuelve el modelo, todavía sin validar.
class LlamadaAFuncion {
  final String nombre;
  final Map<String, dynamic> argumentos;

  const LlamadaAFuncion(this.nombre, this.argumentos);
}

/// Lo que de verdad ejecuta el modelo en el teléfono: recibe el texto y devuelve las llamadas a función que eligió.
///
/// * Es la frontera con el plugin nativo: la implementación real es `MotorLitertLm` y los tests usan uno falso,
///   así `flutter test` nunca carga un `.litertlm`.
abstract class MotorDeLlamadas {
  Future<List<LlamadaAFuncion>> llamar(String texto);

  Future<void> liberar();
}

/// El [ModeloDeAcciones] real: pasa el texto por el [MotorDeLlamadas] y valida lo que devuelve con
/// [interpretarAccion].
///
/// * El modelo se equivoca a veces: solo se toma la primera llamada a función y solo si el catálogo la acepta.
/// * Si el modelo contesta texto en vez de llamar a una función, no se inventa una acción: es un rechazo.
class ModeloDeAccionesLitert implements ModeloDeAcciones {
  final MotorDeLlamadas _motor;
  final CatalogoAcciones _catalogo;

  ModeloDeAccionesLitert({required this._motor, required this._catalogo});

  @override
  Future<ResultadoInterpretacion> entender(String texto) async {
    final llamadas = await _motor.llamar(texto);
    if (llamadas.isEmpty) return const AccionRechazada(MotivoRechazo.desconocida);
    final primera = llamadas.first;
    return interpretarAccion(primera.nombre, primera.argumentos, _catalogo);
  }

  @override
  Future<void> liberar() => _motor.liberar();
}
