/// Un instructor de la escuela con al menos una franja libre (CU14), para el filtro «Instructor».
class InstructorDisponible {
  final String codigo;
  final String nombre;

  const InstructorDisponible({required this.codigo, required this.nombre});

  factory InstructorDisponible.fromJson(Map<String, dynamic> json) {
    return InstructorDisponible(codigo: json['codigo'] as String, nombre: json['nombre'] as String);
  }
}
