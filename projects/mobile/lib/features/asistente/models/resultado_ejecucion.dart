/// Lo que pasó al ejecutar una acción del asistente (CU50), con el mensaje que se le muestra a la persona.
///
/// * [exito] es falso cuando no se pudo hacer lo pedido y el mensaje explica por qué (un día sin clases, un
///   instructor que no existe, sin horas, sin conexión). No es una excepción: es una respuesta esperada.
class ResultadoEjecucion {
  final bool exito;
  final String mensaje;

  const ResultadoEjecucion.ok(this.mensaje) : exito = true;

  const ResultadoEjecucion.fallo(this.mensaje) : exito = false;

  @override
  String toString() => 'ResultadoEjecucion(${exito ? 'ok' : 'fallo'}: $mensaje)';
}
