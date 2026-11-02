const _meses = [
  'ene',
  'feb',
  'mar',
  'abr',
  'may',
  'jun',
  'jul',
  'ago',
  'sep',
  'oct',
  'nov',
  'dic',
];

/// La fecha de un aviso en la lista: `ahora`, `hace 5 min`, `hace 3 h`, `ayer` o `04 may` (con el año si no es
/// el actual). [ahora] se recibe para poder probarlo.
String fechaDeAviso(DateTime fecha, {required DateTime ahora}) {
  final diferencia = ahora.difference(fecha);
  if (!diferencia.isNegative && diferencia.inMinutes < 1) return 'ahora';
  if (!diferencia.isNegative && diferencia.inMinutes < 60) {
    return 'hace ${diferencia.inMinutes} min';
  }

  final hoy = DateTime(ahora.year, ahora.month, ahora.day);
  final dia = DateTime(fecha.year, fecha.month, fecha.day);
  final dias = hoy.difference(dia).inDays;
  if (dias == 0) return 'hace ${diferencia.inHours} h';
  if (dias == 1) return 'ayer';

  final texto = '${fecha.day.toString().padLeft(2, '0')} ${_meses[fecha.month - 1]}';
  return fecha.year == ahora.year ? texto : '$texto ${fecha.year}';
}

/// La fecha y la hora completas de un aviso, para su detalle: `04 may 2026, 14:30`.
String fechaYHoraDeAviso(DateTime fecha) {
  final hora = '${fecha.hour.toString().padLeft(2, '0')}:${fecha.minute.toString().padLeft(2, '0')}';
  return '${fecha.day.toString().padLeft(2, '0')} ${_meses[fecha.month - 1]} ${fecha.year}, $hora';
}
