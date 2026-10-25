const List<String> _meses = [
  'ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic', //
];

/// La fecha como se muestra en el historial de pagos (CU28): `04 may 2026`.
String formatearFechaCorta(DateTime fecha) =>
    '${fecha.day.toString().padLeft(2, '0')} ${_meses[fecha.month - 1]} ${fecha.year}';
