/// Un alumno del instructor con sus horas (CU29): un elemento de `GET /horas-alumnos`.
///
/// * [horasCompradas] y [horasRestantes] son del alumno en total; [horasGastadasConmigo] cuenta solo lo gastado
///   con el instructor que consulta.
/// * Las horas son solo para mostrar: la app no suma ni compara, el backend manda.
class HoraAlumno {
  final String estudianteCodigo;
  final String nombres;
  final String apellidos;
  final int horasCompradas;
  final int horasGastadasConmigo;
  final int horasRestantes;

  const HoraAlumno({
    required this.estudianteCodigo,
    required this.nombres,
    required this.apellidos,
    required this.horasCompradas,
    required this.horasGastadasConmigo,
    required this.horasRestantes,
  });

  factory HoraAlumno.fromJson(Map<String, dynamic> json) => HoraAlumno(
    estudianteCodigo: json['estudianteCodigo'] as String,
    nombres: json['nombres'] as String,
    apellidos: json['apellidos'] as String,
    horasCompradas: (json['horasCompradas'] as num).toInt(),
    horasGastadasConmigo: (json['horasGastadasConmigo'] as num).toInt(),
    horasRestantes: (json['horasRestantes'] as num).toInt(),
  );

  String get nombreCompleto => '$nombres $apellidos';
}
