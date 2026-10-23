import 'fechas.dart';

/// Resultado de bloquear o desbloquear un día completo (CU12): cuántas franjas cambiaron de estado.
/// Cero no es un error: el día no tenía franjas del estado pedido.
class DiaBloqueado {
  final DateTime fecha;
  final int franjasAfectadas;

  const DiaBloqueado({required this.fecha, required this.franjasAfectadas});

  factory DiaBloqueado.fromJson(Map<String, dynamic> json) {
    return DiaBloqueado(
      fecha: fechaDesdeApi(json['fecha'] as String),
      franjasAfectadas: json['franjasAfectadas'] as int,
    );
  }
}
