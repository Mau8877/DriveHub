import 'estado_pago.dart';

/// El estado de un pago (CU26): `GET /pagos/{codigo}/estado`. La app lo consulta mientras espera que la
/// pasarela confirme.
class EstadoDePago {
  final String codigo;
  final EstadoPago estado;

  const EstadoDePago({required this.codigo, required this.estado});

  factory EstadoDePago.fromJson(Map<String, dynamic> json) => EstadoDePago(
    codigo: json['codigo'] as String,
    estado: EstadoPago.fromJson(json['estado'] as String),
  );
}
