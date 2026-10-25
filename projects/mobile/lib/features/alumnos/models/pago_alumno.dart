import '../../pagos/models/estado_pago_visible.dart';

/// Una compra del alumno vista por su instructor (CU29): plan, horas, fecha y estado. Solo lectura.
///
/// * El [estado] reutiliza el de las compras del estudiante (CU28): la relación de negocio lo justifica.
class PagoAlumno {
  final String codigo;
  final String planNombre;
  final int horas;
  final EstadoPagoVisible estado;
  final DateTime creadoEn;

  /// El comprobante de la compra, cuando su PDF ya está listo (CU30); si no, `null`.
  final String? comprobanteCodigo;

  const PagoAlumno({
    required this.codigo,
    required this.planNombre,
    required this.horas,
    required this.estado,
    required this.creadoEn,
    this.comprobanteCodigo,
  });

  factory PagoAlumno.fromJson(Map<String, dynamic> json) => PagoAlumno(
    codigo: json['codigo'] as String,
    planNombre: json['planNombre'] as String,
    horas: (json['horas'] as num).toInt(),
    estado: EstadoPagoVisible.fromJson(json['estado'] as String),
    creadoEn: DateTime.parse(json['creadoEn'] as String).toLocal(),
    comprobanteCodigo: json['comprobanteCodigo'] as String?,
  );
}
