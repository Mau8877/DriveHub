/// Un plan de horas que el estudiante puede comprar (CU26): `GET /planes-pago/disponibles`.
///
/// * Los montos son solo para mostrar (la app no suma ni compara dinero): el backend manda.
class PlanDisponible {
  final String codigo;
  final String nombre;
  final int horas;
  final double costoBs;
  final double costoUsd;

  const PlanDisponible({
    required this.codigo,
    required this.nombre,
    required this.horas,
    required this.costoBs,
    required this.costoUsd,
  });

  factory PlanDisponible.fromJson(Map<String, dynamic> json) => PlanDisponible(
    codigo: json['codigo'] as String,
    nombre: json['nombre'] as String,
    horas: (json['horas'] as num).toInt(),
    costoBs: (json['costoBs'] as num).toDouble(),
    costoUsd: (json['costoUsd'] as num).toDouble(),
  );
}
