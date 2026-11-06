import 'nombre_accion.dart';

/// Lo que el estudiante pidió con la voz, ya validado contra el catálogo (CU50).
///
/// * Los datos son los que el modelo devolvió, todavía sin resolver: `dia` es `viernes` o `manana`, no una fecha;
///   `instructor` es el nombre tal como lo dijo la persona, no un código. Resolverlos es del `ResolvedorDeDatos`.
/// * `hora` ya viene en `HH:mm` (24 horas), como la pide el catálogo.
class AccionAsistente {
  final NombreAccion accion;
  final String? dia;
  final String? momento;
  final String? instructor;
  final String? hora;

  const AccionAsistente({required this.accion, this.dia, this.momento, this.instructor, this.hora});

  @override
  bool operator ==(Object other) =>
      other is AccionAsistente &&
      other.accion == accion &&
      other.dia == dia &&
      other.momento == momento &&
      other.instructor == instructor &&
      other.hora == hora;

  @override
  int get hashCode => Object.hash(accion, dia, momento, instructor, hora);

  @override
  String toString() =>
      'AccionAsistente(${accion.nombre}, dia: $dia, momento: $momento, instructor: $instructor, hora: $hora)';
}
