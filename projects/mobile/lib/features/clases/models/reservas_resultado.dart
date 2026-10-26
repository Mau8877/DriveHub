import 'franja_rechazada.dart';
import 'reserva.dart';

/// Lo que dejó confirmar varias franjas a la vez (CU14): las que quedaron reservadas, las que no con su motivo, y las
/// horas que le quedan al estudiante.
class ReservasResultado {
  final List<Reserva> reservadas;
  final List<FranjaRechazada> rechazadas;

  /// El saldo real que informa Pagos; nulo si Pagos no respondió al pedirlo (la app lo vuelve a pedir).
  final int? horasDisponibles;

  const ReservasResultado({required this.reservadas, required this.rechazadas, required this.horasDisponibles});

  factory ReservasResultado.fromJson(Map<String, dynamic> json) {
    return ReservasResultado(
      reservadas: (json['reservadas'] as List<dynamic>)
          .map((e) => Reserva.fromJson(e as Map<String, dynamic>))
          .toList(),
      rechazadas: (json['rechazadas'] as List<dynamic>)
          .map((e) => FranjaRechazada.fromJson(e as Map<String, dynamic>))
          .toList(),
      horasDisponibles: (json['horasDisponibles'] as num?)?.toInt(),
    );
  }

  /// Ninguna franja quedó reservada: no se gastó ninguna hora.
  bool get ningunaReservada => reservadas.isEmpty;
}
