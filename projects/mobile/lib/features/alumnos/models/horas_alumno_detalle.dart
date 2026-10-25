import 'consumo_clase.dart';
import 'hora_alumno.dart';
import 'pago_alumno.dart';

/// El detalle de un alumno para su instructor (CU29): `GET /horas-alumnos/{estudianteCodigo}`.
///
/// * [consumos] son las clases con este instructor, la más reciente primero; vacía si todavía no hubo clases.
/// * [pagos] son las compras del alumno, las más recientes primero.
class HorasAlumnoDetalle {
  final HoraAlumno resumen;
  final List<ConsumoClase> consumos;
  final List<PagoAlumno> pagos;

  const HorasAlumnoDetalle({
    required this.resumen,
    required this.consumos,
    required this.pagos,
  });

  factory HorasAlumnoDetalle.fromJson(Map<String, dynamic> json) =>
      HorasAlumnoDetalle(
        resumen: HoraAlumno.fromJson(json['resumen'] as Map<String, dynamic>),
        consumos: (json['consumos'] as List<dynamic>)
            .map((e) => ConsumoClase.fromJson(e as Map<String, dynamic>))
            .toList(),
        pagos: (json['pagos'] as List<dynamic>)
            .map((e) => PagoAlumno.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}
